import { useQueries } from '@tanstack/react-query'
import { getTourismAccommodations } from '../saved/savedApi'
import type { Itinerary } from './types'

type Props = {
  itinerary: Itinerary
}

export function ItineraryAccommodations({ itinerary }: Props) {
  const overnightStops = itinerary.days.slice(0, -1)
    .map((day) => {
      const lastPlace = day.items.at(-1)
      if (!lastPlace || lastPlace.latitude === null || lastPlace.longitude === null) return null
      return {
        date: day.date,
        dayNumber: day.dayNumber,
        placeName: lastPlace.name,
        latitude: lastPlace.latitude,
        longitude: lastPlace.longitude,
      }
    })
    .filter((stop): stop is NonNullable<typeof stop> => stop !== null)

  const queries = useQueries({
    queries: overnightStops.map((stop) => ({
      queryKey: ['tourism-accommodations', stop.date, stop.latitude, stop.longitude],
      queryFn: () => getTourismAccommodations(stop.latitude, stop.longitude),
      staleTime: 1000 * 60 * 30,
    })),
  })

  if (overnightStops.length === 0) return null

  return (
    <section className="itinerary-accommodations">
      <header>
        <div>
          <span className="eyebrow">STAY NEARBY</span>
          <h2>일차별 숙소 추천</h2>
        </div>
        <p>각 일차 마지막 장소 반경 10km · 가까운 순</p>
      </header>
      <div className="overnight-stay-list">
        {overnightStops.map((stop, index) => {
          const query = queries[index]
          return (
            <article className="overnight-stay" key={stop.date}>
              <div className="overnight-stay-heading">
                <span>DAY {stop.dayNumber} 숙박</span>
                <strong>{stop.placeName} 주변</strong>
                <small>{stop.date} 일정 종료 후</small>
              </div>
              {query.isLoading && <div className="empty-state">주변 숙소를 찾고 있습니다.</div>}
              {query.isError && <div className="empty-state">숙소 정보를 불러오지 못했습니다.</div>}
              {query.data?.length === 0 && (
                <div className="empty-state">반경 10km 안에 관광공사 등록 숙소가 없습니다.</div>
              )}
              {!!query.data?.length && (
                <div className="stay-card-row">
                  {query.data.slice(0, 3).map((stay) => (
                    <div className="stay-card" key={stay.contentId}>
                      {stay.imageUrl
                        ? <img src={stay.imageUrl} alt="" />
                        : <div className="stay-placeholder">숙박</div>}
                      <div>
                        <span>약 {(stay.distanceMeters / 1000).toFixed(1)}km</span>
                        <h3>{stay.name}</h3>
                        <p>{stay.address ?? '주소 정보 없음'}</p>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </article>
          )
        })}
      </div>
    </section>
  )
}
