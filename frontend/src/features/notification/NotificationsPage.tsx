import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { getNotifications, markAllNotificationsRead, markNotificationRead } from './notificationApi'

const formatDate = (value: string) => new Intl.DateTimeFormat('ko-KR', {
  dateStyle: 'medium', timeStyle: 'short',
}).format(new Date(value))

export function NotificationsPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const query = useQuery({ queryKey: ['notifications'], queryFn: getNotifications })
  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['notifications'] })
    queryClient.invalidateQueries({ queryKey: ['notifications', 'unread-count'] })
  }
  const readMutation = useMutation({ mutationFn: markNotificationRead, onSuccess: invalidate })
  const allMutation = useMutation({ mutationFn: markAllNotificationsRead, onSuccess: invalidate })

  const openNotification = async (id: number, read: boolean, targetUrl: string | null) => {
    if (!read) await readMutation.mutateAsync(id)
    if (targetUrl) navigate(targetUrl)
  }

  return (
    <main className="notification-page">
      <nav className="top-nav">
        <Link className="brand-link" to="/">SEND IT</Link>
        <Link to="/saved">저장한 장소</Link>
      </nav>
      <header className="notification-header">
        <div><span className="eyebrow">NOTIFICATIONS</span><h1>알림</h1></div>
        <button type="button" disabled={allMutation.isPending} onClick={() => allMutation.mutate()}>
          모두 읽음
        </button>
      </header>
      {query.isLoading && <div className="analysis-state">알림을 불러오고 있습니다.</div>}
      {query.isError && <div className="form-error">알림을 불러오지 못했습니다.</div>}
      {query.data?.length === 0 && <div className="notification-empty">아직 도착한 알림이 없습니다.</div>}
      <section className="notification-list">
        {query.data?.map((item) => (
          <button
            type="button"
            key={item.id}
            className={item.read ? 'read' : 'unread'}
            onClick={() => openNotification(item.id, item.read, item.targetUrl)}
          >
            <i aria-hidden="true" />
            <span><strong>{item.title}</strong><small>{item.message}</small></span>
            <time>{formatDate(item.createdAt)}</time>
          </button>
        ))}
      </section>
    </main>
  )
}
