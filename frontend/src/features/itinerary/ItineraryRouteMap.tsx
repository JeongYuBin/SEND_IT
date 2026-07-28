import { useCallback, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  KakaoMap,
  type KakaoMapPoint,
  type KakaoMapRoute,
} from '../../components/KakaoMap'
import type { ItineraryDay } from './types'

const DAY_COLORS = ['#176b52', '#d16d3b', '#5577b8', '#8a5aad', '#bf9b30']

export function ItineraryRouteMap({ days }: { days: ItineraryDay[] }) {
  const navigate = useNavigate()
  const routes = useMemo<KakaoMapRoute[]>(
    () => days.map((day) => ({
      id: day.date,
      color: DAY_COLORS[(day.dayNumber - 1) % DAY_COLORS.length],
      points: day.items
        .filter((item) => item.latitude !== null && item.longitude !== null)
        .map((item) => ({
          id: item.savedPlaceId,
          name: item.name,
          latitude: item.latitude!,
          longitude: item.longitude!,
          label: `${day.dayNumber}-${item.daySequence}`,
        })),
    })).filter((route) => route.points.length > 0),
    [days],
  )
  const points = useMemo<KakaoMapPoint[]>(
    () => routes.flatMap((route) => route.points),
    [routes],
  )
  const handleSelect = useCallback(
    (point: KakaoMapPoint) => navigate(`/saved/places/${point.id}`),
    [navigate],
  )

  if (points.length === 0) {
    return (
      <div className="map-empty-state itinerary-map-empty">
        <strong>동선을 지도에 표시할 수 없어요.</strong>
        <span>장소의 좌표가 보강되면 카카오맵 동선이 표시됩니다.</span>
      </div>
    )
  }

  return (
    <section className="itinerary-map-panel">
      <div className="itinerary-map-heading">
        <div>
          <span>ROUTE MAP</span>
          <h2>일자별 동선</h2>
        </div>
        <div className="route-legend">
          {routes.map((route, index) => (
            <span key={route.id}>
              <i style={{ backgroundColor: route.color }} />
              {index + 1}일차
            </span>
          ))}
        </div>
      </div>
      <KakaoMap
        ariaLabel="여행 계획 일자별 동선 지도"
        points={points}
        routes={routes}
        onSelect={handleSelect}
      />
    </section>
  )
}
