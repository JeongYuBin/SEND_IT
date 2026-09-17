import { useState, useRef, useEffect, type ReactNode } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import { useAuthStore } from '../stores/authStore'

type IconName = 'map' | 'route' | 'bookmark' | 'person'

const items: Array<{ to: string; label: string; icon: IconName }> = [
  { to: '/saved', label: '게시물', icon: 'bookmark' },
  { to: '/', label: '지도', icon: 'map' },
  { to: '/itineraries', label: '여행', icon: 'route' },
  { to: '/profile', label: '프로필', icon: 'person' },
]

const icons: Record<IconName, ReactNode> = {
  map: <><path d="m3.5 5.5 5-2 7 2 5-2v15l-5 2-7-2-5 2z" /><path d="M8.5 3.5v15M15.5 5.5v15" /></>,
  route: <><circle cx="6" cy="18" r="2.5" /><circle cx="18" cy="6" r="2.5" /><path d="M8.5 18h2.25a3 3 0 0 0 3-3v-6a3 3 0 0 1 3-3h.75" /></>,
  bookmark: <path d="M6 3.5h12v17l-6-3.8-6 3.8z" />,
  person: <><circle cx="12" cy="8" r="4" /><path d="M4.5 21a7.5 7.5 0 0 1 15 0" /></>,
}

export function MobileBottomNavigation() {
  const authenticated = useAuthStore((state) => Boolean(state.accessToken))
  const { pathname } = useLocation()
  const [expanded, setExpanded] = useState(() => {
    try { return localStorage.getItem('sendit-menu-expanded') !== 'false' } catch { return true }
  })
  const dragStart = useRef<number | null>(null)
  const dragged = useRef(false)
  useEffect(() => {
    try { localStorage.setItem('sendit-menu-expanded', String(expanded)) } catch { /* Session state still works. */ }
  }, [expanded])
  if (!authenticated || ['/login', '/signup', '/find-id', '/reset-password', '/share-target'].includes(pathname)) return null

  return (
    <section className={`mobile-menu-sheet ${expanded ? 'expanded' : ''}`} aria-label="메뉴 패널">
      <button className="mobile-menu-handle" type="button" aria-expanded={expanded}
        aria-controls="mobile-menu-links" aria-label={expanded ? '메뉴 접기' : '메뉴 펼치기'}
        onClick={() => { if (!dragged.current) setExpanded((value) => !value) }}
        onKeyDown={(event) => { if (event.key === 'Escape') setExpanded(false) }}
        onPointerDown={(event) => {
          dragStart.current = event.clientY
          dragged.current = false
          event.currentTarget.setPointerCapture(event.pointerId)
        }}
        onPointerMove={(event) => {
          if (dragStart.current === null) return
          const delta = event.clientY - dragStart.current
          if (Math.abs(delta) > 18) {
            dragged.current = true
            setExpanded(delta < 0)
          }
        }}
        onPointerUp={() => { dragStart.current = null }}
        onPointerCancel={() => { dragStart.current = null }}>
        <span />
      </button>
    <nav id="mobile-menu-links" className="mobile-bottom-nav" aria-label="주요 메뉴" hidden={!expanded}>
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
    </section>
  )
}
