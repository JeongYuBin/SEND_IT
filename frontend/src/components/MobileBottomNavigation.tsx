import { NavLink, useLocation } from 'react-router-dom'
import { useAuthStore } from '../stores/authStore'

const items = [
  { to: '/', label: '홈', icon: '⌂' },
  { to: '/itineraries', label: '여행', icon: '✦' },
  { to: '/saved', label: '저장 장소', icon: '♡' },
  { to: '/profile', label: '내 정보', icon: '♙' },
  { to: '/settings', label: '설정', icon: '⚙' },
]

export function MobileBottomNavigation() {
  const authenticated = useAuthStore((state) => Boolean(state.accessToken))
  const { pathname } = useLocation()
  if (!authenticated || pathname === '/login' || pathname === '/signup' || pathname === '/share-target') return null

  return (
    <nav className="mobile-bottom-nav" aria-label="앱 주요 메뉴">
      {items.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.to === '/'}
          className={({ isActive }) => isActive ? 'active' : ''}
        >
          <span aria-hidden="true">{item.icon}</span>
          <small>{item.label}</small>
        </NavLink>
      ))}
    </nav>
  )
}
