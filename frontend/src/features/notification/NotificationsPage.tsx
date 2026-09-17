import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { AccountIcon, AccountPageHeader } from '../account/AccountPageHeader'
import {
  deleteReadNotifications,
  getNotificationsPage,
  markAllNotificationsRead,
  markNotificationRead,
} from './notificationApi'

const formatDate = (value: string) => new Intl.DateTimeFormat('ko-KR', {
  dateStyle: 'medium', timeStyle: 'short',
}).format(new Date(value))

export function NotificationsPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const query = useInfiniteQuery({
    queryKey: ['notifications', 'page'],
    queryFn: ({ pageParam }) => getNotificationsPage(pageParam),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => lastPage.last ? undefined : lastPage.page + 1,
  })
  const notifications = query.data?.pages.flatMap((page) => page.content) ?? []
  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['notifications'] })
    queryClient.invalidateQueries({ queryKey: ['notifications', 'unread-count'] })
  }
  const readMutation = useMutation({ mutationFn: markNotificationRead, onSuccess: invalidate })
  const allMutation = useMutation({ mutationFn: markAllNotificationsRead, onSuccess: invalidate })
  const deleteReadMutation = useMutation({
    mutationFn: deleteReadNotifications,
    onSuccess: () => {
      setShowDeleteConfirm(false)
      invalidate()
    },
  })

  const openNotification = async (id: number, read: boolean, targetUrl: string | null) => {
    if (!read) {
      try { await readMutation.mutateAsync(id) }
      catch { return }
    }
    if (targetUrl) navigate(targetUrl)
  }

  const deleteRead = () => {
    const readCount = notifications.filter((item) => item.read).length
    if (readCount === 0) return
    setShowDeleteConfirm(true)
  }

  return (
    <main className="notification-page">
      <nav className="top-nav">
        <Link className="brand-link" to="/">SEND IT</Link>
        <Link to="/saved">저장한 장소</Link>
      </nav>
      <AccountPageHeader eyebrow="NOTIFICATIONS" title="알림" description="여행과 저장한 장소의 새로운 소식을 확인하세요." />
      <div className="notification-header">
        <div className="notification-actions">
          <button type="button" disabled={allMutation.isPending} onClick={() => allMutation.mutate()}>
            모두 읽음
          </button>
          <button
            type="button"
            disabled={deleteReadMutation.isPending || !notifications.some((item) => item.read)}
            onClick={deleteRead}
          >
            {deleteReadMutation.isPending ? '삭제 중...' : '읽은 알림 삭제'}
          </button>
        </div>
      </div>
      {query.isLoading && <div className="analysis-state">알림을 불러오고 있습니다.</div>}
      {query.isError && <div className="account-feedback" role="alert">알림을 불러오지 못했습니다. <button type="button" onClick={() => void query.refetch()}>다시 시도</button></div>}
      {(readMutation.isError || allMutation.isError || deleteReadMutation.isError) && <p className="account-feedback" role="alert">알림을 처리하지 못했습니다. 다시 시도해 주세요.</p>}
      {notifications.length === 0 && query.isSuccess && <div className="notification-empty"><AccountIcon name="bell" /><strong>아직 도착한 알림이 없어요</strong><span>새로운 소식이 생기면 여기에 알려드릴게요.</span></div>}
      <section className="notification-list">
        {notifications.map((item) => (
          <button
            type="button"
            key={item.id}
            className={item.read ? 'read' : 'unread'}
            onClick={() => openNotification(item.id, item.read, item.targetUrl)}
          >
            <i aria-hidden="true" />
            <span><strong>{!item.read && <span className="account-sr-only">읽지 않은 알림: </span>}{item.title}</strong><small>{item.message}</small></span>
            <time dateTime={item.createdAt}>{formatDate(item.createdAt)}</time>
          </button>
        ))}
      </section>
      {query.hasNextPage && (
        <button
          className="shared-content-more"
          type="button"
          disabled={query.isFetchingNextPage}
          onClick={() => query.fetchNextPage()}
        >
          {query.isFetchingNextPage ? '불러오는 중...' : '알림 더 보기'}
        </button>
      )}
      <ConfirmDialog
        open={showDeleteConfirm}
        title="읽은 알림을 삭제할까요?"
        description="읽은 알림을 모두 삭제합니다. 삭제한 알림은 복구할 수 없습니다."
        confirmLabel="읽은 알림 삭제"
        pending={deleteReadMutation.isPending}
        onCancel={() => setShowDeleteConfirm(false)}
        onConfirm={() => deleteReadMutation.mutate()}
      />
    </main>
  )
}
