import { useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { KakaoMap, type KakaoMapPoint } from '../../components/KakaoMap'
import type { SavedPlace } from './types'

export function SavedPlacesMap({ places }: { places: SavedPlace[] }) {
  const navigate = useNavigate()
  const points = useMemo<KakaoMapPoint[]>(
    () => places
      .filter((place) => place.latitude !== null && place.longitude !== null)
      .map((place) => ({
        id: place.savedPlaceId,
        name: place.name,
        latitude: place.latitude!,
        longitude: place.longitude!,
      })),
    [places],
  )

  if (points.length === 0) {
    return (
      <div className="map-empty-state">
        <strong>지도에 표시할 수 있는 장소가 없어요.</strong>
        <span>좌표가 분석된 장소를 저장하면 카카오맵에서 확인할 수 있습니다.</span>
      </div>
    )
  }

  return (
    <section className="saved-map" aria-label="저장 장소 카카오맵">
      <KakaoMap
        ariaLabel="저장한 장소 지도"
        points={points}
        onSelect={(point) => navigate(`/saved/places/${point.id}`)}
      />
    </section>
  )
}
