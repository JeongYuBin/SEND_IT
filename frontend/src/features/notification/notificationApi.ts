import { http } from '../../api/http'
import type { AppNotification, NotificationPage } from './types'

export async function getNotificationsPage(page = 0, size = 20) {
  return (await http.get<NotificationPage>('/notifications/page', {
    params: { page, size },
  })).data
}

export async function getUnreadNotificationCount() {
  return (await http.get<{ count: number }>('/notifications/unread-count')).data.count
}

export async function markNotificationRead(id: number) {
  return (await http.patch<AppNotification>(`/notifications/${id}/read`)).data
}

export async function markAllNotificationsRead() {
  await http.patch('/notifications/read-all')
}

export async function deleteReadNotifications() {
  return (await http.delete<{ deletedCount: number }>('/notifications/read')).data
}
