import { NavLink, useLocation, useNavigate } from 'react-router-dom'
import { logout } from '../features/auth/authApi'
import { useAuthStore } from '../stores/authStore'

const hiddenPaths = ['/login', '/signup', '/find-id', '/reset-password', '/share-target']

const items = [
  { to: '/saved', label: '게시물' },
  { to: '/', label: '지도', end: true },
  { to: '/itineraries', label: '여행 계획' },
  { to: '/notifications', label: '알림' },
  { to: '/profile', label: '프로필' },
  { to: '/settings', label: '설정' },
]

export function DesktopSiteNavigation() {
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const { accessToken, refreshToken, clearSession } = useAuthStore()

  if (hiddenPaths.includes(pathname)) return null

  const handleLogout = async () => {
    try {
      if (refreshToken) await logout(refreshToken)
    } finally {
      clearSession()
      navigate('/', { replace: true })
    }
  }

  return <>
    <nav className="desktop-site-nav" aria-label="전체 메뉴">
      <NavLink className="desktop-site-brand" to="/">SEND IT</NavLink>
      <div className="desktop-site-links">
        {accessToken ? <>
          {items.map((item) => (
            <NavLink key={item.to} to={item.to} end={item.end}
              className={({ isActive }) => isActive ? 'active' : undefined}>
              {item.label}
            </NavLink>
          ))}
          <button type="button" onClick={handleLogout}>로그아웃</button>
        </> : <>
          <NavLink to="/login">로그인</NavLink>
          <NavLink className="desktop-site-cta" to="/signup">시작하기</NavLink>
        </>}
      </div>
    </nav>
    <div className="desktop-site-nav-spacer" aria-hidden="true" />
  </>
}
