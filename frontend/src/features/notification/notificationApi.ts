import { http } from '../../api/http'
import type { AppNotification } from './types'

export async function getNotifications() {
  return (await http.get<AppNotification[]>('/notifications')).data
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
