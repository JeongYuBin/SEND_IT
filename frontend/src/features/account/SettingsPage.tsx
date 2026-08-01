import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { logout } from '../auth/authApi'
import { useAuthStore } from '../../stores/authStore'

export function SettingsPage() {
  const navigate = useNavigate()
  const { refreshToken, clearSession } = useAuthStore()
  const [loggingOut, setLoggingOut] = useState(false)

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
        <p>앱과 계정 사용 설정을 관리합니다.</p>
      </header>
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
