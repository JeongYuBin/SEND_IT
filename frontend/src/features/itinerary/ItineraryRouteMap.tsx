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
  const dayPoints = useMemo(
    () => days.map((day) => ({
      day,
      points: day.items
        .filter((item) => item.latitude !== null && item.longitude !== null)
        .map((item) => ({
          id: item.savedPlaceId,
          name: item.name,
          latitude: item.latitude!,
          longitude: item.longitude!,
          label: `${day.dayNumber}-${item.daySequence}`,
        })),
    })),
    [days],
  )
  const routes = useMemo<KakaoMapRoute[]>(
    () => days.flatMap((day) => day.items
      .filter((item) => item.routePathFromPrevious.length > 1)
      .map((item) => ({
        id: `${day.date}-${item.savedPlaceId}`,
        color: DAY_COLORS[(day.dayNumber - 1) % DAY_COLORS.length],
        points: item.routePathFromPrevious.map((point, index) => ({
          id: `${item.savedPlaceId}-route-${index}`,
          name: item.name,
          latitude: point.latitude,
          longitude: point.longitude,
        })),
      }))),
    [days],
  )
  const points = useMemo<KakaoMapPoint[]>(
    () => dayPoints.flatMap((day) => day.points),
    [dayPoints],
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
          {dayPoints.filter(({ points }) => points.length > 0).map(({ day }) => (
            <span key={day.date}>
              <i style={{
                backgroundColor: DAY_COLORS[(day.dayNumber - 1) % DAY_COLORS.length],
              }} />
              {day.dayNumber}일차
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
