import { useQuery } from '@tanstack/react-query'
import { Link, useLocation } from 'react-router-dom'
import { getUnreadNotificationCount } from '../features/notification/notificationApi'
import { useAuthStore } from '../stores/authStore'

export function NotificationShortcut() {
  const authenticated = useAuthStore((state) => Boolean(state.accessToken))
  const { pathname } = useLocation()
  const countQuery = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: getUnreadNotificationCount,
    enabled: authenticated && pathname !== '/',
    refetchInterval: 15000,
  })

  if (!authenticated || ['/', '/login', '/signup', '/find-id', '/reset-password', '/share-target', '/notifications'].includes(pathname)) return null
  const count = countQuery.data ?? 0
  return (
    <Link className="notification-shortcut" to="/notifications" aria-label={`알림 ${count}개`}>
      <svg aria-hidden="true" viewBox="0 0 24 24">
        <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" />
        <path d="M10 21h4" />
      </svg>
      {count > 0 && <strong>{count > 99 ? '99+' : count}</strong>}
    </Link>
  )
}
