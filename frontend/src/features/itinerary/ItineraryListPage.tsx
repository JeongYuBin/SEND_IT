import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { getItineraries } from './itineraryApi'
import type { ItineraryStatus, TransportType } from './types'

const transportLabels: Record<TransportType, string> = {
  WALKING: '도보',
  PUBLIC_TRANSIT: '대중교통',
  CAR: '자동차',
}

const statusLabels: Record<ItineraryStatus, string> = {
  DRAFT: '작성 중',
  GENERATED: '일정 생성',
  COMPLETED: '여행 완료',
}

export function ItineraryListPage() {
  const itinerariesQuery = useQuery({
    queryKey: ['itineraries'],
    queryFn: getItineraries,
  })

  return (
    <main className="itinerary-shell">
      <nav className="top-nav">
        <Link className="brand-link" to="/">SEND IT</Link>
        <div>
          <Link to="/saved">저장한 장소</Link>
          <Link to="/">URL 저장하기</Link>
        </div>
      </nav>

      <header className="itinerary-header itinerary-list-header">
        <div>
          <span className="eyebrow">MY TRIPS</span>
          <h1>전체 여행 계획</h1>
          <p>지금까지 저장한 여행 계획을 날짜순으로 확인하고 이어서 편집해 보세요.</p>
        </div>
        <Link className="primary-button" to="/itineraries/new">+ 계획 추가</Link>
      </header>

      <section className="itinerary-saved-list">
        {itinerariesQuery.isLoading && (
          <div className="empty-state">여행 계획을 불러오고 있습니다.</div>
        )}
        {!itinerariesQuery.isLoading && (itinerariesQuery.data?.length ?? 0) === 0 && (
          <div className="empty-state itinerary-list-empty">
            <strong>아직 저장한 여행 계획이 없습니다.</strong>
            <span>저장한 장소를 골라 첫 여행 일정을 만들어 보세요.</span>
            <Link className="primary-button" to="/itineraries/new">첫 계획 만들기</Link>
          </div>
        )}
        <div className="itinerary-saved-grid">
          {itinerariesQuery.data?.map((itinerary) => (
            <Link
              className="itinerary-saved-card"
              key={itinerary.id}
              to={`/itineraries/${itinerary.id}`}
            >
              <div>
                <span>{itinerary.startDate === itinerary.endDate
                  ? itinerary.startDate
                  : `${itinerary.startDate} – ${itinerary.endDate}`}</span>
                <b>{statusLabels[itinerary.status]}</b>
              </div>
              <strong>{itinerary.title}</strong>
              <small>
                {transportLabels[itinerary.transportType]} · 장소 {itinerary.items.length}개
              </small>
              <span className="itinerary-card-action">계획 열기 →</span>
            </Link>
          ))}
        </div>
      </section>
    </main>
  )
}
