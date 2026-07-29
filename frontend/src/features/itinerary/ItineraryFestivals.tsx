import { useQuery } from '@tanstack/react-query'
import { getTourismFestivals } from '../saved/savedApi'
import type { Itinerary } from './types'

type Props = {
  itinerary: Itinerary
}

function dateRange(startDate: string | null, endDate: string | null) {
  if (!startDate && !endDate) return '행사 기간 정보 없음'
  if (startDate === endDate || !endDate) return startDate
  return `${startDate ?? endDate} – ${endDate}`
}

export function ItineraryFestivals({ itinerary }: Props) {
  const coordinates = itinerary.items
    .filter((item) => item.latitude !== null && item.longitude !== null)
    .map((item) => ({
      latitude: item.latitude!,
      longitude: item.longitude!,
    }))

  const festivalsQuery = useQuery({
    queryKey: ['tourism-events', itinerary.id, itinerary.startDate, itinerary.endDate],
    queryFn: async () => {
      const responses = await Promise.all(coordinates.map((coordinate) =>
        getTourismFestivals(
          itinerary.startDate,
          itinerary.endDate,
          coordinate.latitude,
          coordinate.longitude,
        )))
      const unique = new Map(responses.flat().map((festival) => [festival.contentId, festival]))
      return [...unique.values()]
        .sort((left, right) => left.distanceMeters - right.distanceMeters)
        .slice(0, 6)
    },
    enabled: coordinates.length > 0,
    staleTime: 1000 * 60 * 30,
  })

  if (coordinates.length === 0) return null

  return (
    <section className="itinerary-festivals">
      <header>
        <div>
          <span className="eyebrow">TRIP EVENTS</span>
          <h2>여행 기간 주변 행사</h2>
        </div>
        <p>일정 장소 반경 30km · 관광공사 데이터</p>
      </header>
      {festivalsQuery.isLoading && <div className="empty-state">여행 기간의 행사를 찾고 있습니다.</div>}
      {festivalsQuery.isError && (
        <div className="empty-state">행사 정보를 불러오지 못했습니다. 잠시 후 다시 확인해 주세요.</div>
      )}
      {festivalsQuery.data?.length === 0 && (
        <div className="empty-state">여행 기간과 동선 주변에 등록된 행사가 없습니다.</div>
      )}
      {!!festivalsQuery.data?.length && (
        <div className="itinerary-festival-grid">
          {festivalsQuery.data.map((festival) => (
            <article key={festival.contentId}>
              {festival.imageUrl
                ? <img src={festival.imageUrl} alt="" />
                : <div className="festival-placeholder">행사</div>}
              <div>
                <span>{dateRange(festival.startDate, festival.endDate)}</span>
                <h3>{festival.name}</h3>
                <p>{festival.address ?? '장소 정보 없음'}</p>
                <small>일정 장소에서 약 {(festival.distanceMeters / 1000).toFixed(1)}km</small>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}
