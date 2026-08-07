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
    enabled: authenticated,
    refetchInterval: 15000,
  })

  if (!authenticated || ['/login', '/signup', '/share-target'].includes(pathname)) return null
  const count = countQuery.data ?? 0
  return (
    <Link className="notification-shortcut" to="/notifications" aria-label={`알림 ${count}개`}>
      <span aria-hidden="true">🔔</span>
      {count > 0 && <strong>{count > 99 ? '99+' : count}</strong>}
    </Link>
  )
}
