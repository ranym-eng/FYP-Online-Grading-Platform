export const API_BASE = import.meta.env.VITE_API_BASE_URL || ''

export async function apiRequest(path, options = {}, token = '') {
  const headers = {
    Accept: 'application/json',
    ...(options.body && !(options.body instanceof FormData) ? { 'Content-Type': 'application/json' } : {}),
    ...(token ? { Authorization: 'Bearer ' + token } : {}),
    ...(options.headers || {}),
  }
  const response = await fetch(API_BASE + path, { ...options, headers })
  const text = await response.text()
  let payload
  try {
    payload = text ? JSON.parse(text) : null
  } catch {
    payload = text
  }
  if (!response.ok) {
    throw new Error(payload?.message || payload?.error || response.statusText)
  }
  return payload
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
