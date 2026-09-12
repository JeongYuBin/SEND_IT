import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { KakaoMap, type KakaoMapPoint } from '../../components/KakaoMap'
import type { SavedPlace } from '../saved/types'

const filters = [
  { label: '전체', pattern: /.*/ },
  { label: '음식점', pattern: /음식|식당|맛집|한식|중식|일식|양식/ },
  { label: '카페', pattern: /카페|커피|디저트/ },
  { label: '숙소', pattern: /숙소|호텔|펜션|게스트|리조트/ },
  { label: '즐겨찾기', pattern: /.*/ },
]

export function HomePlacesMap({ places }: { places: SavedPlace[] }) {
  const navigate = useNavigate()
  const [filter, setFilter] = useState('전체')
  const [currentLocation, setCurrentLocation] = useState({ latitude: 37.5665, longitude: 126.978 })

  useEffect(() => {
    if (!navigator.geolocation) return
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => setCurrentLocation({ latitude: coords.latitude, longitude: coords.longitude }),
      () => undefined,
      { enableHighAccuracy: true, timeout: 8000, maximumAge: 60_000 },
    )
  }, [])
  const points = useMemo<KakaoMapPoint[]>(() => {
    const selected = filters.find((item) => item.label === filter) ?? filters[0]
    return places
      .filter((place) => place.latitude !== null && place.longitude !== null)
      .filter((place) => filter === '즐겨찾기' ? place.priority > 0 : selected.pattern.test(place.category ?? ''))
      .map((place) => ({
        id: place.savedPlaceId,
        name: place.name,
        label: place.name,
        category: place.category,
        latitude: place.latitude!,
        longitude: place.longitude!,
      }))
  }, [filter, places])

  return (
    <section className="home-map" aria-label="저장한 장소 지도">
      <div className="home-map-filters" role="group" aria-label="장소 카테고리 필터">
        {filters.map((item) => (
          <button
            key={item.label}
            type="button"
            className={filter === item.label ? 'active' : ''}
            onClick={() => setFilter(item.label)}
          >{item.label}</button>
        ))}
      </div>
      <KakaoMap
        ariaLabel={`${filter} 저장 장소 ${points.length}곳`}
        points={points}
        initialCenter={currentLocation}
        fitPoints={false}
        onSelect={(point) => navigate(`/saved/places/${point.id}`)}
      />
      <div className="home-map-summary">
        <span>MY PLACES</span>
        <strong>저장한 장소 {points.length}곳</strong>
        <button type="button" onClick={() => navigate('/saved')}>목록으로 보기</button>
      </div>
    </section>
  )
}
