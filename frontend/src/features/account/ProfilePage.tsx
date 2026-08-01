import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useAuthStore } from '../../stores/authStore'
import { getItineraries } from '../itinerary/itineraryApi'
import { getSavedPlaces } from '../saved/savedApi'

export function ProfilePage() {
  const user = useAuthStore((state) => state.user)
  const itinerariesQuery = useQuery({ queryKey: ['itineraries'], queryFn: getItineraries })
  const savedPlacesQuery = useQuery({ queryKey: ['saved-places'], queryFn: getSavedPlaces })

  return (
    <main className="account-shell">
      <nav className="top-nav">
        <Link className="brand-link" to="/">SEND IT</Link>
        <Link to="/settings">설정</Link>
      </nav>
      <header className="account-header">
        <span className="eyebrow">MY PROFILE</span>
        <h1>내 정보</h1>
        <p>여행 준비 현황과 계정 정보를 확인해 보세요.</p>
      </header>
      <section className="profile-card">
        <div className="profile-avatar" aria-hidden="true">
          {user?.nickname?.slice(0, 1).toUpperCase() ?? 'S'}
        </div>
        <div>
          <span>여행자</span>
          <h2>{user?.nickname}</h2>
          <p>{user?.email}</p>
        </div>
      </section>
      <section className="profile-stats" aria-label="나의 여행 현황">
        <Link to="/itineraries">
          <strong>{itinerariesQuery.data?.length ?? 0}</strong>
          <span>여행 계획</span>
        </Link>
        <Link to="/saved">
          <strong>{savedPlacesQuery.data?.length ?? 0}</strong>
          <span>저장한 장소</span>
        </Link>
      </section>
    </main>
  )
}
