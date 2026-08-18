export const API_BASE = import.meta.env.VITE_API_BASE_URL || ''

function authHeaders(token, headers = {}) {
  return {
    Accept: 'application/json',
    ...(token ? { Authorization: 'Bearer ' + token } : {}),
    ...headers,
  }
}

export async function apiRequest(path, options = {}, token = '') {
  const headers = authHeaders(token, {
    ...(options.body && !(options.body instanceof FormData) ? { 'Content-Type': 'application/json' } : {}),
    ...(options.headers || {}),
  })
  const response = await fetch(API_BASE + path, { ...options, headers })
  const text = await response.text()
  let payload
  try {
    payload = text ? JSON.parse(text) : null
  } catch {
    payload = text
  }
  if (!response.ok) {
    const error = new Error(payload?.message || payload?.error || response.statusText)
    error.status = response.status
    error.errorCode = payload?.data?.errorCode
    throw error
  }
  return payload
}

export async function downloadFile(path, token = '', fallbackName = 'export.xlsx') {
  const response = await fetch(API_BASE + path, { headers: authHeaders(token) })
  if (!response.ok) {
    let message = response.statusText
    try {
      const payload = await response.json()
      message = payload?.message || message
    } catch {
      // Keep the HTTP status text when the server did not return JSON.
    }
    const error = new Error(message)
    error.status = response.status
    throw error
  }
  const disposition = response.headers.get('Content-Disposition') || ''
  const encoded = disposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1]
  const quoted = disposition.match(/filename="?([^";]+)"?/i)?.[1]
  const filename = encoded ? decodeURIComponent(encoded) : quoted || fallbackName
  const url = URL.createObjectURL(await response.blob())
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  URL.revokeObjectURL(url)
  return filename
}

export function unwrapList(payload) {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.data)) return payload.data
  if (Array.isArray(payload?.data?.content)) return payload.data.content
  if (payload?.data && typeof payload.data === 'object') return [payload.data]
  return []
}

export function pretty(value) {
  if (value === null || value === undefined || value === '') return '-'
  return String(value)
    .replaceAll('_', ' ')
    .toLowerCase()
    .replace(/\b\w/g, (char) => char.toUpperCase())
}

export function itemName(item, fallback = 'Untitled') {
  if (!item || typeof item !== 'object') return fallback
  const personName = [item.firstName, item.lastName].filter(Boolean).join(' ')
  return item.fullName || personName || item.title || item.name || item.code || item.email || item.studentNumber || item.id || fallback
}

export function itemId(item) {
  return typeof item === 'object' && item ? item.id : item
}