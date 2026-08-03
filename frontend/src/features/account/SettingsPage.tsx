import { useEffect, useState, type FormEvent } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { logout } from '../auth/authApi'
import type { TransportType } from '../itinerary/types'
import { useAuthStore } from '../../stores/authStore'
import { getProfile, updatePreferences } from './accountApi'

export function SettingsPage() {
  const navigate = useNavigate()
  const { refreshToken, clearSession, updateUser } = useAuthStore()
  const [loggingOut, setLoggingOut] = useState(false)
  const [preferredTransport, setPreferredTransport] = useState<TransportType>('PUBLIC_TRANSIT')
  const [travelWithPet, setTravelWithPet] = useState(false)
  const profileQuery = useQuery({ queryKey: ['profile'], queryFn: getProfile })
  const updateMutation = useMutation({
    mutationFn: updatePreferences,
    onSuccess: (profile) => updateUser(profile),
  })

  useEffect(() => {
    if (!profileQuery.data) return
    setPreferredTransport(profileQuery.data.preferredTransport)
    setTravelWithPet(profileQuery.data.travelWithPet)
  }, [profileQuery.data])

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    updateMutation.mutate({ preferredTransport, travelWithPet })
  }

  const handleLogout = async () => {
    setLoggingOut(true)
    try {
      if (refreshToken) await logout(refreshToken)
    } finally {
      clearSession()
      navigate('/', { replace: true })
    }
  }

  return (
    <main className="account-shell">
      <nav className="top-nav">
        <Link className="brand-link" to="/">SEND IT</Link>
        <div>
          <Link to="/itineraries">여행 계획</Link>
          <Link to="/saved">저장한 장소</Link>
          <Link to="/profile">내 정보</Link>
        </div>
      </nav>
      <header className="account-header">
        <span className="eyebrow">SETTINGS</span>
        <h1>설정</h1>
        <p>여행 계획에 사용할 기본 환경을 관리합니다.</p>
      </header>

      <form className="preference-form" onSubmit={handleSubmit}>
        <div className="preference-heading">
          <div>
            <span className="eyebrow">TRAVEL PREFERENCES</span>
            <h2>여행 기본 설정</h2>
          </div>
          <button type="submit" disabled={profileQuery.isLoading || updateMutation.isPending}>
            {updateMutation.isPending ? '저장 중...' : '설정 저장'}
          </button>
        </div>

        <label className="preference-field">
          <span>선호 이동수단</span>
          <small>새 여행 계획을 만들 때 우선 사용할 이동수단입니다.</small>
          <select
            value={preferredTransport}
            onChange={(event) => setPreferredTransport(event.target.value as TransportType)}
          >
            <option value="PUBLIC_TRANSIT">대중교통</option>
            <option value="CAR">자동차</option>
            <option value="WALKING">도보</option>
          </select>
        </label>

        <label className="preference-toggle">
          <span>
            <strong>반려동물과 함께 여행</strong>
            <small>여행 성향을 저장해 반려동물 동반 기능에서 활용합니다.</small>
          </span>
          <input
            type="checkbox"
            checked={travelWithPet}
            onChange={(event) => setTravelWithPet(event.target.checked)}
          />
        </label>

        {updateMutation.isSuccess && <p className="form-success" role="status">여행 설정을 저장했습니다.</p>}
        {updateMutation.isError && <p className="form-error" role="alert">설정을 저장하지 못했습니다. 다시 시도해 주세요.</p>}
        {profileQuery.isError && <p className="form-error" role="alert">현재 설정을 불러오지 못했습니다.</p>}
      </form>

      <section className="settings-list">
        <div>
          <span>앱 버전</span>
          <strong>0.1.0</strong>
        </div>
        <div>
          <span>데이터 저장</span>
          <strong>내 계정에 저장</strong>
        </div>
        <button type="button" onClick={handleLogout} disabled={loggingOut}>
          {loggingOut ? '로그아웃 중...' : '로그아웃'}
        </button>
      </section>
    </main>
  )
}
