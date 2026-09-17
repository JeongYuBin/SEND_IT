import { useEffect, useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useAuthStore } from '../../stores/authStore'
import { getItineraries } from '../itinerary/itineraryApi'
import { getSavedPlaces } from '../saved/savedApi'
import { getProfile, updateProfile } from './accountApi'
import { AccountIcon, AccountPageHeader } from './AccountPageHeader'

export function ProfilePage() {
  const queryClient = useQueryClient()
  const { user, updateUser } = useAuthStore()
  const [editing, setEditing] = useState(false)
  const [nickname, setNickname] = useState(user?.nickname ?? '')
  const profileQuery = useQuery({ queryKey: ['profile'], queryFn: getProfile })
  const itinerariesQuery = useQuery({ queryKey: ['itineraries'], queryFn: getItineraries })
  const savedPlacesQuery = useQuery({ queryKey: ['saved-places'], queryFn: getSavedPlaces })
  const profile = profileQuery.data ?? user
  const updateMutation = useMutation({
    mutationFn: updateProfile,
    onSuccess: (updated) => {
      queryClient.setQueryData(['profile'], updated)
      updateUser(updated)
      setNickname(updated.nickname)
      setEditing(false)
    },
  })

  useEffect(() => {
    if (profileQuery.data) {
      updateUser(profileQuery.data)
      setNickname(profileQuery.data.nickname)
    }
  }, [profileQuery.data, updateUser])

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const value = nickname.trim()
    if (value) updateMutation.mutate(value)
  }

  return (
    <main className="account-shell">
      <nav className="top-nav">
        <Link className="brand-link" to="/">SEND IT</Link>
        <div>
          <Link to="/itineraries">여행 계획</Link>
          <Link to="/saved">저장한 장소</Link>
          <Link to="/settings">설정</Link>
          <Link to="/notifications">알림</Link>
        </div>
      </nav>
      <AccountPageHeader eyebrow="MY PROFILE" title="내 정보" description="여행 준비 현황과 계정 정보를 확인해 보세요." backTo="/" backLabel="지도" />
      {profileQuery.isError && <p className="account-feedback" role="alert">최신 계정 정보를 불러오지 못했습니다. <button type="button" onClick={() => void profileQuery.refetch()}>다시 시도</button></p>}
      <section className="profile-card">
        <div className="profile-avatar" aria-hidden="true">
          {profile?.nickname?.slice(0, 1).toUpperCase() ?? 'S'}
        </div>
        <div className="profile-summary">
          <span>여행자</span>
          <h2>{profile?.nickname ?? '불러오는 중…'}</h2>
          <p>{profile?.email}</p>
          {!editing && (
            <button type="button" aria-expanded={editing} aria-controls="profile-edit-form" onClick={() => setEditing(true)}>이름 수정</button>
          )}
        </div>
      </section>
      {editing && (
        <form id="profile-edit-form" className="profile-edit-form" onSubmit={handleSubmit}>
          <label htmlFor="profile-nickname">이름</label>
          <input
            id="profile-nickname"
            autoComplete="nickname"
            autoFocus
            required
            maxLength={50}
            value={nickname}
            onChange={(event) => setNickname(event.target.value)}
          />
          <div>
            <button
              type="button"
              onClick={() => {
                setNickname(profile?.nickname ?? '')
                setEditing(false)
                updateMutation.reset()
              }}
            >
              취소
            </button>
            <button type="submit" disabled={updateMutation.isPending || !nickname.trim()}>
              {updateMutation.isPending ? '저장 중...' : '변경 저장'}
            </button>
          </div>
          {updateMutation.isError && (
            <p className="form-error" role="alert">이름을 변경하지 못했습니다. 다시 시도해 주세요.</p>
          )}
        </form>
      )}
      <section className="profile-stats" aria-label="나의 여행 현황">
        <Link to="/itineraries">
          <strong>{itinerariesQuery.data?.length ?? '—'}</strong>
          <span>여행 계획</span>
        </Link>
        <Link to="/saved">
          <strong>{savedPlacesQuery.data?.length ?? '—'}</strong>
          <span>저장한 장소</span>
        </Link>
      </section>
      <section className="profile-quick-links" aria-label="계정 메뉴">
        <Link to="/notifications"><AccountIcon name="bell" /><span>알림<small>여행과 장소의 새로운 소식</small></span><span className="account-chevron" aria-hidden="true">›</span></Link>
        <Link to="/settings"><AccountIcon name="settings" /><span>설정<small>계정 및 보안 관리</small></span><span className="account-chevron" aria-hidden="true">›</span></Link>
      </section>
      {(itinerariesQuery.isError || savedPlacesQuery.isError) && <p className="account-feedback" role="alert">여행 현황 일부를 불러오지 못했습니다. <button type="button" onClick={() => {
        if (itinerariesQuery.isError) void itinerariesQuery.refetch()
        if (savedPlacesQuery.isError) void savedPlacesQuery.refetch()
      }}>다시 시도</button></p>}
    </main>
  )
}
