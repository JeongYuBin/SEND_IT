import { useEffect, useState } from 'react'
import { searchKakaoPlaces } from './savedApi'
import type { KakaoPlaceSearchResult } from './types'

const normalize = (value: string) => value.replace(/\s+/g, '').toLocaleLowerCase()

export function PlaceNameLookup({ name, address, onSelect }: {
  name: string
  address: string
  onSelect: (place: KakaoPlaceSearchResult) => void
}) {
  const [result, setResult] = useState<{ query: string; places: KakaoPlaceSearchResult[]; error?: boolean } | null>(null)
  const query = name.trim()
  useEffect(() => {
    if (query.length < 2) return
    let cancelled = false
    const timer = window.setTimeout(async () => {
      try {
        const response = await searchKakaoPlaces(query)
        if (cancelled) return
        const places = response.places.filter(place => place.roadAddress || place.address)
        setResult({ query, places })
        const exact = places.filter(place => normalize(place.name) === normalize(query))
        // Only a unique result can fill an empty address without a user choice.
        if (!address.trim() && exact.length === 1 && response.last) onSelect(exact[0])
      } catch {
        if (!cancelled) setResult({ query, places: [], error: true })
      }
    }, 450)
    return () => { cancelled = true; window.clearTimeout(timer) }
  }, [query, address, onSelect])

  if (query.length < 2) return null
  if (result?.query !== query) return <small className="place-name-lookup-status" role="status">일치하는 장소를 찾고 있어요…</small>
  if (result.error) return <small className="place-name-lookup-status" role="status">장소를 찾지 못했어요. 주소를 직접 입력할 수 있어요.</small>
  if (!result.places.length) return <small className="place-name-lookup-status" role="status">검색 결과가 없어요. 장소명에 지역을 함께 입력해 보세요.</small>
  if (result.places.some(place => normalize(place.name) === normalize(query)
    && (place.roadAddress ?? place.address) === address)) {
    return <small className="place-name-lookup-status" role="status">카카오맵 장소와 일치하는 주소예요.</small>
  }
  return <div className="place-name-lookup" aria-label="카카오맵 장소 검색 결과">
    <small>주소를 적용할 장소를 선택해 주세요.</small>
    {result.places.map(place => <button type="button" key={place.kakaoPlaceId} onClick={() => onSelect(place)}>
      <strong>{place.name}</strong><span>{place.roadAddress ?? place.address}</span>
    </button>)}
  </div>
}
