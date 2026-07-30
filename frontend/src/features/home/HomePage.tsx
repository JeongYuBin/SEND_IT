import { useState, type FormEvent } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { AxiosError } from 'axios'
import { Link, useNavigate } from 'react-router-dom'
import { logout } from '../auth/authApi'
import type { ApiError } from '../auth/types'
import { createShare } from '../share/shareApi'
import { getItineraries } from '../itinerary/itineraryApi'
import { getSavedPlaces } from '../saved/savedApi'
import { useAuthStore } from '../../stores/authStore'
import type { TransportType } from '../itinerary/types'
import { PlaceImage } from '../../components/PlaceImage'

const transportLabels: Record<TransportType, string> = {
  WALKING: '도보',
  PUBLIC_TRANSIT: '대중교통',
  CAR: '자동차',
}

export function HomePage() {
  const { accessToken, refreshToken, user, clearSession } = useAuthStore()
  const navigate = useNavigate()
  const [url, setUrl] = useState('')
  const savedPlacesQuery = useQuery({
    queryKey: ['saved-places'],
    queryFn: getSavedPlaces,
    enabled: Boolean(accessToken),
  })
  const itinerariesQuery = useQuery({
    queryKey: ['itineraries'],
    queryFn: getItineraries,
    enabled: Boolean(accessToken),
  })
  const shareMutation = useMutation({
    mutationFn: createShare,
    onError: (error) => {
      const status = (error as AxiosError).response?.status
      if (status === 401 || status === 403) {
        navigate('/login', {
          replace: true,
          state: { message: '로그인이 만료되었습니다. 다시 로그인해 주세요.' },
        })
      }
    },
  })

  const handleLogout = async () => {
    try {
      if (refreshToken) {
        await logout(refreshToken)
      }
    } finally {
      clearSession()
    }
  }

  const handleShare = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!accessToken) {
      navigate('/login', { state: { returnTo: '/' } })
      return
    }
    shareMutation.mutate(url, {
      onSuccess: (result) => {
        setUrl('')
        navigate(`/shares/${result.shareId}`)
      },
    })
  }

  const shareError = (shareMutation.error as AxiosError<ApiError> | null)?.response?.data
  const today = new Date().toLocaleDateString('en-CA')
  const recentPlaces = [...(savedPlacesQuery.data ?? [])]
    .sort((a, b) => b.savedAt.localeCompare(a.savedAt))
    .slice(0, 4)
  const upcomingTrips = [...(itinerariesQuery.data ?? [])]
    .filter((itinerary) => itinerary.endDate >= today)
    .sort((a, b) => a.startDate.localeCompare(b.startDate))
    .slice(0, 3)

  return (
    <main className="shell">
      <nav className="top-nav">
        <Link className="brand-link" to="/">SEND IT</Link>
        <div>
          {accessToken ? (
            <>
              <span>{user?.nickname}님</span>
              <Link to="/saved">저장한 장소</Link>
              <button className="text-button" type="button" onClick={handleLogout}>로그아웃</button>
            </>
          ) : (
            <>
              <Link to="/login">로그인</Link>
              <Link className="nav-cta" to="/signup">시작하기</Link>
            </>
          )}
        </div>
      </nav>

      <section className="hero">
        <span className="eyebrow">SEND IT</span>
        <h1>발견한 여행지를<br />진짜 여행으로.</h1>
        <p>
          SNS와 블로그에서 찾은 장소를 저장하고,
          방문 가능한 여행 동선으로 만들어 보세요.
        </p>
        <form className="share-form" onSubmit={handleShare}>
          <label htmlFor="content-url">저장할 콘텐츠 URL</label>
          <div>
            <input
              id="content-url"
              required
              type="url"
              placeholder="https://..."
              aria-describedby="url-help"
              value={url}
              onChange={(event) => setUrl(event.target.value)}
            />
            <button type="submit" disabled={shareMutation.isPending}>
              {shareMutation.isPending ? '접수 중...' : '장소 찾기'}
            </button>
          </div>
          <small id="url-help">Instagram, YouTube, 네이버 블로그와 일반 웹페이지를 지원할 예정입니다.</small>
          {shareMutation.isSuccess && (
            <div className="share-feedback success" role="status">
              {shareMutation.data.duplicate ? '이미 저장한 콘텐츠예요.' : '저장했어요! 장소를 분석하고 있습니다.'}
              <span>접수 번호 #{shareMutation.data.shareId}</span>
            </div>
          )}
          {shareMutation.isError && (
            <div className="share-feedback error" role="alert">
              {shareError?.message ?? 'URL을 접수하지 못했습니다. 다시 시도해 주세요.'}
            </div>
          )}
        </form>
      </section>
      {accessToken && (
        <section className="home-dashboard" aria-label="내 여행 대시보드">
          <div className="home-dashboard-heading">
            <div>
              <span className="eyebrow">MY TRAVEL</span>
              <h2>{user?.nickname}님의 여행 준비</h2>
            </div>
            <Link to="/itineraries">전체 여행 계획 보기</Link>
          </div>

          <section className="home-dashboard-section">
            <header>
              <div>
                <span>UPCOMING</span>
                <h3>다가오는 여행</h3>
              </div>
              <Link to="/itineraries">계획 관리</Link>
            </header>
            {itinerariesQuery.isLoading ? (
              <div className="home-dashboard-empty">여행 계획을 불러오고 있습니다.</div>
            ) : upcomingTrips.length > 0 ? (
              <div className="home-trip-grid">
                {upcomingTrips.map((itinerary) => (
                  <Link className="home-trip-card" to={`/itineraries/${itinerary.id}`} key={itinerary.id}>
                    <span>{itinerary.startDate === itinerary.endDate
                      ? itinerary.startDate
                      : `${itinerary.startDate} – ${itinerary.endDate}`}</span>
                    <strong>{itinerary.title}</strong>
                    <small>
                      {transportLabels[itinerary.transportType]} · {itinerary.items.length}개 장소
                    </small>
                    <b>{itinerary.startDate <= today ? '여행 중' : '예정'}</b>
                  </Link>
                ))}
              </div>
            ) : (
              <div className="home-dashboard-empty">
                <span>다가오는 여행 계획이 없습니다.</span>
                <Link to="/itineraries">새 여행 계획 만들기</Link>
              </div>
            )}
          </section>

          <section className="home-dashboard-section">
            <header>
              <div>
                <span>RECENT PLACES</span>
                <h3>최근 저장한 장소</h3>
              </div>
              <Link to="/saved">전체 장소 보기</Link>
            </header>
            {savedPlacesQuery.isLoading ? (
              <div className="home-dashboard-empty">저장한 장소를 불러오고 있습니다.</div>
            ) : recentPlaces.length > 0 ? (
              <div className="home-place-grid">
                {recentPlaces.map((place) => (
                  <Link
                    className="home-place-card"
                    to={`/saved/places/${place.savedPlaceId}`}
                    key={place.savedPlaceId}
                  >
                    {place.imageUrl
                      ? (
                        <PlaceImage
                          src={place.imageUrl}
                          alt={`${place.name} 대표 이미지`}
                          className="home-place-skeleton"
                        />
                      )
                      : (
                        <span
                          className="place-image-skeleton home-place-skeleton"
                          role="img"
                          aria-label="장소 이미지 준비 중"
                        >
                          <i className="place-image-skeleton-sun" />
                          <i className="place-image-skeleton-mountain" />
                          <i className="place-image-skeleton-ground" />
                        </span>
                      )}
                    <span>
                      <small>{place.category ?? '장소'}</small>
                      <strong>{place.name}</strong>
                      <span>{place.roadAddress ?? place.address ?? '주소 정보 없음'}</span>
                    </span>
                  </Link>
                ))}
              </div>
            ) : (
              <div className="home-dashboard-empty">
                <span>아직 저장한 장소가 없습니다.</span>
                <a href="#content-url">URL로 첫 장소 저장하기</a>
              </div>
            )}
          </section>
        </section>
      )}
    </main>
  )
}
