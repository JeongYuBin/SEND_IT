const urlPattern = /https?:\/\/\S+|www\.\S+/gi
const emailPattern = /\b[\w.+-]+@[\w.-]+\.[A-Za-z]{2,}\b/g
const separatorPattern = /^[-_=·•\s]{4,}$/

export function sourceExcerpt(value: string | null, maxLength = 240) {
  if (!value) return null

  const seen = new Set<string>()
  const lines = value
    .split(/\r?\n/)
    .map((line) => line.replace(urlPattern, '').replace(emailPattern, '').trim())
    .filter((line) => line.length > 1 && !separatorPattern.test(line))
    .filter((line) => {
      const key = line.replace(/\s+/g, ' ').toLocaleLowerCase('ko-KR')
      if (seen.has(key)) return false
      seen.add(key)
      return true
    })

  const excerpt = lines.join(' ').replace(/\s+/g, ' ').trim()
  if (!excerpt) return null
  if (excerpt.length <= maxLength) return excerpt
  return `${excerpt.slice(0, maxLength).trimEnd()}…`
}
