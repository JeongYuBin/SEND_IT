import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { getItinerary } from './itineraryApi'
import { ItineraryRouteMap } from './ItineraryRouteMap'
import type { ItineraryStatus, TransportType } from './types'

const transportLabels: Record<TransportType, string> = {
  WALKING: '도보',
  PUBLIC_TRANSIT: '대중교통',
  CAR: '자동차',
}

const statusLabels: Record<ItineraryStatus, string> = {
  DRAFT: '초안',
  GENERATED: '경로 생성 완료',
  COMPLETED: '여행 완료',
}

function time(value: string) {
  return value.slice(0, 5)
}

export function ItineraryDetailPage() {
  const { itineraryId } = useParams()
  const id = Number(itineraryId)
  const itineraryQuery = useQuery({
    queryKey: ['itineraries', id],
    queryFn: () => getItinerary(id),
    enabled: Number.isInteger(id) && id > 0,
  })

  if (!Number.isInteger(id) || id <= 0) {
    return <main className="itinerary-shell"><div className="form-error">올바르지 않은 여행 계획 주소입니다.</div></main>
  }

  return (
    <main className="itinerary-shell">
      <nav className="top-nav">
        <Link className="brand-link" to="/">SEND IT</Link>
        <div>
          <Link to="/itineraries">여행 계획</Link>
          <Link to="/saved">저장한 장소</Link>
        </div>
      </nav>
      {itineraryQuery.isLoading && <div className="empty-state detail-loading">여행 계획을 불러오고 있습니다.</div>}
      {itineraryQuery.isError && <div className="form-error detail-loading">여행 계획을 찾을 수 없습니다.</div>}
      {itineraryQuery.data && (
        <article className="itinerary-detail">
          <header>
            <span className="eyebrow">{statusLabels[itineraryQuery.data.status]}</span>
            <h1>{itineraryQuery.data.title}</h1>
            <p>
              {itineraryQuery.data.startDate} — {itineraryQuery.data.endDate}
              <span>매일 {time(itineraryQuery.data.dailyStartTime)}–{time(itineraryQuery.data.dailyEndTime)}</span>
              <span>{transportLabels[itineraryQuery.data.transportType]}</span>
            </p>
          </header>
          <section className="itinerary-notice">
            장소 좌표의 직선거리와 이동 수단별 평균 속도로 계산한 예상 동선입니다.
            실제 도로 상황과 대중교통 시간은 다를 수 있습니다.
          </section>
          <ItineraryRouteMap days={itineraryQuery.data.days} />
          <div className="itinerary-days">
            {itineraryQuery.data.days.map((day) => (
              <section className="itinerary-day" key={day.date}>
                <header className="itinerary-day-header">
                  <div>
                    <span>DAY {day.dayNumber}</span>
                    <h2>{day.date}</h2>
                  </div>
                  <small>{day.items.length}개 장소</small>
                </header>
                {day.exceedsDailyWindow && (
                  <div className="schedule-warning">
                    설정한 하루 종료 시간을 넘습니다. 장소 수나 체류 시간을 조정해야 합니다.
                  </div>
                )}
                {day.items.length === 0 ? (
                  <div className="day-empty">이 날짜에 배정된 장소가 없습니다.</div>
                ) : (
                  <ol className="itinerary-timeline">
                    {day.items.map((item) => (
                      <li key={item.savedPlaceId}>
                        <span className="timeline-number">{item.daySequence}</span>
                        <div className="timeline-stop">
                          {item.travelMinutesFromPrevious > 0 && (
                            <div className="travel-estimate">
                              예상 이동 {item.travelMinutesFromPrevious}분
                              {item.distanceKmFromPrevious !== null && ` · 약 ${item.distanceKmFromPrevious}km`}
                              {!item.coordinateAvailable && ' · 좌표 없음'}
                            </div>
                          )}
                          <Link to={`/saved/places/${item.savedPlaceId}`}>
                            {item.imageUrl
                              ? <img src={item.imageUrl} alt="" />
                              : <div className="timeline-placeholder">{item.name.slice(0, 1)}</div>}
                            <span>
                              <small>{time(item.arrivalTime)}–{time(item.departureTime)} · 체류 {item.stayMinutes}분</small>
                              <strong>{item.name}</strong>
                              <span>{item.address ?? '주소 정보 없음'}</span>
                            </span>
                          </Link>
                        </div>
                      </li>
                    ))}
                  </ol>
                )}
              </section>
            ))}
          </div>
        </article>
      )}
    </main>
  )
}
