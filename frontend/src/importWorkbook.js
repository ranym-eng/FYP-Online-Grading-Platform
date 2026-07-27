const textDecoder = new TextDecoder('utf-8')

function readUint16(view, offset) {
  return view.getUint16(offset, true)
}

function readUint32(view, offset) {
  return view.getUint32(offset, true)
}

async function unzipEntries(buffer) {
  const bytes = new Uint8Array(buffer)
  const view = new DataView(buffer)
  let endOffset = -1

  for (let offset = bytes.length - 22; offset >= Math.max(0, bytes.length - 65557); offset -= 1) {
    if (readUint32(view, offset) === 0x06054b50) {
      endOffset = offset
      break
    }
  }
  if (endOffset < 0) throw new Error('Archive Excel invalide.')

  const entryCount = readUint16(view, endOffset + 10)
  let offset = readUint32(view, endOffset + 16)
  const entries = new Map()

  for (let index = 0; index < entryCount; index += 1) {
    if (readUint32(view, offset) !== 0x02014b50) throw new Error('Structure ZIP Excel non reconnue.')
    const compression = readUint16(view, offset + 10)
    const compressedSize = readUint32(view, offset + 20)
    const fileNameLength = readUint16(view, offset + 28)
    const extraLength = readUint16(view, offset + 30)
    const commentLength = readUint16(view, offset + 32)
    const localOffset = readUint32(view, offset + 42)
    const name = textDecoder.decode(bytes.slice(offset + 46, offset + 46 + fileNameLength))
    const localNameLength = readUint16(view, localOffset + 26)
    const localExtraLength = readUint16(view, localOffset + 28)
    const dataOffset = localOffset + 30 + localNameLength + localExtraLength
    const compressed = bytes.slice(dataOffset, dataOffset + compressedSize)
    let content

    if (compression === 0) {
      content = compressed
    } else if (compression === 8) {
      const stream = new Blob([compressed]).stream().pipeThrough(new DecompressionStream('deflate-raw'))
      content = new Uint8Array(await new Response(stream).arrayBuffer())
    } else {
      throw new Error(`Compression Excel non prise en charge: ${compression}.`)
    }

    entries.set(name.replaceAll('\\', '/'), content)
    offset += 46 + fileNameLength + extraLength + commentLength
  }

  return entries
}

function xmlDocument(entries, path) {
  const content = entries.get(path)
  if (!content) throw new Error(`Élément Excel manquant: ${path}.`)
  return new DOMParser().parseFromString(textDecoder.decode(content), 'application/xml')
}

function parseSharedStrings(entries) {
  if (!entries.has('xl/sharedStrings.xml')) return []
  const document = xmlDocument(entries, 'xl/sharedStrings.xml')
  return [...document.getElementsByTagNameNS('*', 'si')].map((item) => {
    return [...item.getElementsByTagNameNS('*', 't')].map((node) => node.textContent || '').join('')
  })
}

function columnIndex(reference) {
  const letters = reference.match(/^[A-Z]+/i)?.[0]?.toUpperCase() || 'A'
  return [...letters].reduce((value, letter) => value * 26 + letter.charCodeAt(0) - 64, 0) - 1
}

function normalizeTarget(target) {
  if (target.startsWith('/')) return target.slice(1)
  const parts = ['xl', ...target.split('/')]
  const normalized = []
  for (const part of parts) {
    if (part === '..') normalized.pop()
    else if (part !== '.') normalized.push(part)
  }
  return normalized.join('/')
}

function parseSheet(entries, path, sharedStrings) {
  const document = xmlDocument(entries, path)
  return [...document.getElementsByTagNameNS('*', 'row')].map((row) => {
    const values = []
    for (const cell of row.getElementsByTagNameNS('*', 'c')) {
      const index = columnIndex(cell.getAttribute('r') || 'A1')
      const type = cell.getAttribute('t')
      const raw = cell.getElementsByTagNameNS('*', 'v')[0]?.textContent || ''
      let value = raw
      if (type === 's') value = sharedStrings[Number(raw)] || ''
      if (type === 'inlineStr') value = [...cell.getElementsByTagNameNS('*', 't')].map((node) => node.textContent || '').join('')
      values[index] = value
    }
    return values.map((value) => value ?? '')
  })
}

function findHeaderRow(rows, kind) {
  const expected = kind === 'students'
    ? ['student id', 'identifiant', 'nom', 'prénom', 'prenom', 'filière', 'filiere']
    : ['professeur id', 'professor id', 'nom complet', 'email', 'rôle', 'role']

  let bestIndex = 0
  let bestScore = -1
  rows.forEach((row, index) => {
    const normalized = row.map((value) => String(value).trim().toLowerCase())
    const score = expected.filter((term) => normalized.some((value) => value.includes(term))).length
    if (score > bestScore && normalized.filter(Boolean).length >= 3) {
      bestIndex = index
      bestScore = score
    }
  })
  return bestIndex
}

function tableFromRows(rows, kind) {
  const headerIndex = findHeaderRow(rows, kind)
  const headers = rows[headerIndex].map((value, index) => String(value || `Colonne ${index + 1}`).trim())
  const dataRows = rows.slice(headerIndex + 1)
    .filter((row) => row.some((value) => String(value).trim()))
    .map((row) => Object.fromEntries(headers.map((header, index) => [header, String(row[index] ?? '').trim()])))
  return { headers, rows: dataRows }
}

export async function readXlsxFile(file, kind) {
  const entries = await unzipEntries(await file.arrayBuffer())
  const workbook = xmlDocument(entries, 'xl/workbook.xml')
  const relationships = xmlDocument(entries, 'xl/_rels/workbook.xml.rels')
  const relationTargets = new Map([...relationships.getElementsByTagNameNS('*', 'Relationship')].map((relation) => [
    relation.getAttribute('Id'),
    relation.getAttribute('Target'),
  ]))
  const sheets = [...workbook.getElementsByTagNameNS('*', 'sheet')]
  const preferredName = kind === 'students' ? 'liste_etudiants' : 'liste_professeurs'
  const selected = sheets.find((sheet) => (sheet.getAttribute('name') || '').toLowerCase() === preferredName) || sheets[0]
  if (!selected) throw new Error('Aucune feuille trouvée dans le fichier Excel.')
  const relationId = selected.getAttribute('r:id') || selected.getAttributeNS('http://schemas.openxmlformats.org/officeDocument/2006/relationships', 'id')
  const target = relationTargets.get(relationId)
  if (!target) throw new Error('Feuille Excel introuvable.')
  const rows = parseSheet(entries, normalizeTarget(target), parseSharedStrings(entries))
  return { sheetName: selected.getAttribute('name') || '', ...tableFromRows(rows, kind) }
}

function parseCsv(text) {
  const rows = []
  let row = []
  let value = ''
  let quoted = false

  for (let index = 0; index < text.length; index += 1) {
    const char = text[index]
    if (char === '"' && quoted && text[index + 1] === '"') {
      value += '"'; index += 1
    } else if (char === '"') {
      quoted = !quoted
    } else if (char === ',' && !quoted) {
      row.push(value); value = ''
    } else if ((char === '\n' || char === '\r') && !quoted) {
      if (char === '\r' && text[index + 1] === '\n') index += 1
      row.push(value); value = ''
      if (row.some((cell) => cell.trim())) rows.push(row)
      row = []
    } else {
      value += char
    }
  }
  row.push(value)
  if (row.some((cell) => cell.trim())) rows.push(row)
  return rows
}

export async function readImportFile(file, kind) {
  if (!file) throw new Error('Sélectionnez un fichier.')
  const extension = file.name.split('.').pop()?.toLowerCase()
  if (extension === 'xlsx') return readXlsxFile(file, kind)
  if (extension === 'csv') return { sheetName: file.name, ...tableFromRows(parseCsv(await file.text()), kind) }
  throw new Error('Format accepté: .xlsx ou .csv.')
}
