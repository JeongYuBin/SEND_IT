import { useState } from 'react'

type PlaceImageProps = {
  src: string | null | undefined
  alt?: string
  className?: string
  label?: string
}

export function PlaceImage({
  src,
  alt = '',
  className = '',
  label = '등록된 장소 이미지 없음',
}: PlaceImageProps) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null)
  const visibleSrc = src && src !== failedUrl ? src : null

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
