import { useEffect, useState, type FormEvent } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useAuthStore } from '../../stores/authStore'
import { getItineraries } from '../itinerary/itineraryApi'
import { getSavedPlaces } from '../saved/savedApi'
import { getProfile, updateProfile } from './accountApi'

export function ProfilePage() {
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
        </div>
      </nav>
      <header className="account-header">
        <span className="eyebrow">MY PROFILE</span>
        <h1>내 정보</h1>
        <p>여행 준비 현황과 계정 정보를 확인해 보세요.</p>
      </header>
      <section className="profile-card">
        <div className="profile-avatar" aria-hidden="true">
          {profile?.nickname?.slice(0, 1).toUpperCase() ?? 'S'}
        </div>
        <div className="profile-summary">
          <span>여행자</span>
          <h2>{profile?.nickname}</h2>
          <p>{profile?.email}</p>
          {!editing && (
            <button type="button" onClick={() => setEditing(true)}>이름 수정</button>
          )}
        </div>
      </section>
      {editing && (
        <form className="profile-edit-form" onSubmit={handleSubmit}>
          <label htmlFor="profile-nickname">이름</label>
          <input
            id="profile-nickname"
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
