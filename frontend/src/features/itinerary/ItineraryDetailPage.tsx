import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { getItinerary } from './itineraryApi'
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
              <span>매일 {itineraryQuery.data.dailyStartTime.slice(0, 5)}–{itineraryQuery.data.dailyEndTime.slice(0, 5)}</span>
              <span>{transportLabels[itineraryQuery.data.transportType]}</span>
            </p>
          </header>
          <section className="itinerary-notice">
            현재는 선택한 순서로 장소를 보여줍니다. 다음 단계에서 위치와 이동 수단을 반영한 일자별 경로를 생성할 예정입니다.
          </section>
          <ol className="itinerary-timeline">
            {itineraryQuery.data.items.map((item) => (
              <li key={item.savedPlaceId}>
                <span className="timeline-number">{item.sequence}</span>
                <Link to={`/saved/places/${item.savedPlaceId}`}>
                  {item.imageUrl ? <img src={item.imageUrl} alt="" /> : <div className="timeline-placeholder">{item.name.slice(0, 1)}</div>}
                  <span>
                    <small>{item.category ?? '미분류'} · 기본 체류 {item.stayMinutes}분</small>
                    <strong>{item.name}</strong>
                    <span>{item.address ?? '주소 정보 없음'}</span>
                  </span>
                </Link>
              </li>
            ))}
          </ol>
        </article>
      )}
    </main>
  )
}
