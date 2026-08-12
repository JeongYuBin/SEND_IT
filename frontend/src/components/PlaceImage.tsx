import { useState } from 'react'

type PlaceImageProps = {
  src: string | null | undefined
  alt?: string
  className?: string
  label?: string
}

export function resolveImageUrl(src: string | null | undefined) {
  if (!src || !src.startsWith('/api/')) return src
  const apiBase = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'
  try {
    return new URL(src, new URL(apiBase).origin).toString()
  } catch {
    return src
  }
}

export function PlaceImage({
  src,
  alt = '',
  className = '',
  label = '등록된 장소 이미지 없음',
}: PlaceImageProps) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null)
  const resolvedSrc = resolveImageUrl(src)
  const visibleSrc = resolvedSrc && resolvedSrc !== failedUrl ? resolvedSrc : null

  return (
    <span
      className={`place-image-skeleton ${className}`.trim()}
      role="img"
      aria-label={visibleSrc && alt ? alt : label}
    >
      <i className="place-image-skeleton-sun" />
      <i className="place-image-skeleton-mountain" />
      <i className="place-image-skeleton-ground" />
      {visibleSrc && (
        <img
          className="place-image-content"
          src={visibleSrc}
          alt=""
          referrerPolicy="no-referrer"
          onError={() => setFailedUrl(visibleSrc)}
        />
      )}
    </span>
  )
}
