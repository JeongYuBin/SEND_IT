export type NotificationType =
  | 'ANALYSIS_COMPLETED'
  | 'ANALYSIS_NEEDS_CONFIRMATION'
  | 'ANALYSIS_FAILED'
  | 'ITINERARY_UPCOMING'

export type AppNotification = {
  id: number
  type: NotificationType
  title: string
  message: string
  targetUrl: string | null
  read: boolean
  createdAt: string
}

export type NotificationPage = {
  content: AppNotification[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}
