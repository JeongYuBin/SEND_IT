import type { ReactNode } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import { useAuthStore } from '../stores/authStore'

type IconName = 'home' | 'route' | 'bookmark' | 'person' | 'settings'

const items: Array<{ to: string; label: string; icon: IconName }> = [
  { to: '/', label: '홈', icon: 'home' },
  { to: '/itineraries', label: '여행', icon: 'route' },
  { to: '/saved', label: '저장', icon: 'bookmark' },
  { to: '/profile', label: '내 정보', icon: 'person' },
  { to: '/settings', label: '설정', icon: 'settings' },
]

const icons: Record<IconName, ReactNode> = {
  home: <><path d="M3.5 10.5 12 3l8.5 7.5" /><path d="M5.5 9.5v10h13v-10M9.5 19.5v-6h5v6" /></>,
  route: <><circle cx="6" cy="18" r="2.5" /><circle cx="18" cy="6" r="2.5" /><path d="M8.5 18h2.25a3 3 0 0 0 3-3v-6a3 3 0 0 1 3-3h.75" /></>,
  bookmark: <path d="M6 3.5h12v17l-6-3.8-6 3.8z" />,
  person: <><circle cx="12" cy="8" r="4" /><path d="M4.5 21a7.5 7.5 0 0 1 15 0" /></>,
  settings: <><circle cx="12" cy="12" r="3.2" /><path d="M19.4 15a1.7 1.7 0 0 0 .34 1.88l.06.06-2.83 2.83-.06-.06a1.7 1.7 0 0 0-1.88-.34 1.7 1.7 0 0 0-1.03 1.56V21h-4v-.08a1.7 1.7 0 0 0-1.03-1.56 1.7 1.7 0 0 0-1.88.34l-.06.06-2.83-2.83.06-.06A1.7 1.7 0 0 0 4.6 15a1.7 1.7 0 0 0-1.56-1.03H3v-4h.08A1.7 1.7 0 0 0 4.64 8.9a1.7 1.7 0 0 0-.34-1.88l-.06-.06 2.83-2.83.06.06A1.7 1.7 0 0 0 9 4.53 1.7 1.7 0 0 0 10.03 3H10V3h4v.08A1.7 1.7 0 0 0 15.07 4.6a1.7 1.7 0 0 0 1.88-.34l.06-.06 2.83 2.83-.06.06A1.7 1.7 0 0 0 19.44 9 1.7 1.7 0 0 0 21 10.03V14a1.7 1.7 0 0 0-1.6 1Z" /></>,
}

export function MobileBottomNavigation() {
  const authenticated = useAuthStore((state) => Boolean(state.accessToken))
  const { pathname } = useLocation()
  if (!authenticated || pathname === '/login' || pathname === '/signup' || pathname === '/share-target') return null

  return (
    <nav className="mobile-bottom-nav" aria-label="주요 메뉴">
      {items.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.to === '/'}
          className={({ isActive }) => isActive ? 'active' : ''}
        >
          <span aria-hidden="true"><svg viewBox="0 0 24 24">{icons[item.icon]}</svg></span>
          <small>{item.label}</small>
        </NavLink>
      ))}
    </nav>
  )
}
