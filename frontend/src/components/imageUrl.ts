export function resolveImageUrl(src: string | null | undefined) {
  if (!src || !src.startsWith('/api/')) return src
  const apiBase = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'
  try {
    return new URL(src, new URL(apiBase).origin).toString()
  } catch {
    return src
  }
}
