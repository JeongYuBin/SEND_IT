import { useState } from 'react'
import { resolveImageUrl } from './imageUrl'

type PlaceImageProps = {
  src: string | null | undefined
  alt?: string
  className?: string
  label?: string
  category?: string | null
  fallbackSources?: (string | null | undefined)[]
  draggable?: boolean
}

export function PlaceImage({
  src,
  alt = '',
  className = '',
  label = '등록된 사진이 없습니다',
  category,
  fallbackSources = [],
  draggable,
}: PlaceImageProps) {
  const [failedUrls, setFailedUrls] = useState<string[]>([])
  const visibleSrc = [src, ...fallbackSources].map(resolveImageUrl).find((url) => url && !failedUrls.includes(url))
  const value = category ?? ''
  const kind = /카페|커피|디저트|베이커리/.test(value) ? 'cafe'
    : /음식|식당|구이|요리|한식|중식|일식|양식|갈비|치킨|고기/.test(value) ? 'food'
    : /숙박|숙소|호텔|펜션|리조트/.test(value) ? 'stay'
    : /행사|축제/.test(value) ? 'event' : 'place'
  const icon = {
    cafe: 'M5 8h12v7a5 5 0 0 1-5 5h-2a5 5 0 0 1-5-5V8Zm12 1h2a3 3 0 0 1 0 6h-2M8 3v2m5-2v2M3 22h18',
    food: 'M5 3v6m3-6v6M2 3v6a3 3 0 0 0 6 0M5 12v9M19 21V3c-4 2-5 7-5 10h5',
    stay: 'M3 20V5m18 15v-8H3m0 5h18M6 12V8h5v4m0-4h7a3 3 0 0 1 3 3v1',
    event: 'm12 3 2.7 5.5 6.1.9-4.4 4.3 1 6.1-5.4-2.9-5.4 2.9 1-6.1-4.4-4.3 6.1-.9L12 3Z',
    place: 'M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 1 1 16 0ZM15 10a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z',
  }[kind]

  return (
    <span
      className={`place-image-skeleton place-image-refined ${className}`.trim()}
      data-kind={kind}
      role="img"
      aria-label={visibleSrc && alt ? alt : label}
    >
      <span className="place-image-fallback" aria-hidden="true">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round"><path d={icon} /></svg>
        <span>사진 미등록</span>
      </span>
      {visibleSrc && (
        <img
          className="place-image-content"
          draggable={draggable}
          src={visibleSrc}
          loading="lazy"
          decoding="async"
          alt=""
          referrerPolicy="no-referrer"
          onError={() => setFailedUrls((previous) => [...previous, visibleSrc])}
        />
      )}
    </span>
  )
}
