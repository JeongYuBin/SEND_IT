import type { ReactNode } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import { useAuthStore } from '../stores/authStore'

type IconName = 'home' | 'inbox' | 'route' | 'bookmark' | 'person'

const items: Array<{ to: string; label: string; icon: IconName }> = [
  { to: '/', label: 'URL', icon: 'home' },
  { to: '/shares', label: '콘텐츠', icon: 'inbox' },
  { to: '/saved', label: '저장', icon: 'bookmark' },
  { to: '/itineraries', label: '여행', icon: 'route' },
  { to: '/profile', label: '내 정보', icon: 'person' },
]

const icons: Record<IconName, ReactNode> = {
  home: <><path d="M3.5 10.5 12 3l8.5 7.5" /><path d="M5.5 9.5v10h13v-10M9.5 19.5v-6h5v6" /></>,
  inbox: <><path d="M4 5h16v14H4z" /><path d="M4 14h4l2 2h4l2-2h4" /></>,
  route: <><circle cx="6" cy="18" r="2.5" /><circle cx="18" cy="6" r="2.5" /><path d="M8.5 18h2.25a3 3 0 0 0 3-3v-6a3 3 0 0 1 3-3h.75" /></>,
  bookmark: <path d="M6 3.5h12v17l-6-3.8-6 3.8z" />,
  person: <><circle cx="12" cy="8" r="4" /><path d="M4.5 21a7.5 7.5 0 0 1 15 0" /></>,
}

export function MobileBottomNavigation() {
  const authenticated = useAuthStore((state) => Boolean(state.accessToken))
  const { pathname } = useLocation()
  if (!authenticated || ['/login', '/signup', '/find-id', '/reset-password', '/share-target'].includes(pathname)) return null

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
