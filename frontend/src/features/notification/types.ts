export type NotificationType =
  | 'ANALYSIS_COMPLETED'
  | 'ANALYSIS_NEEDS_CONFIRMATION'
  | 'ANALYSIS_FAILED'

export type AppNotification = {
  id: number
  type: NotificationType
  title: string
  message: string
  targetUrl: string | null
  read: boolean
  createdAt: string
}
