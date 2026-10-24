import api from './client'

export interface NotificationDto {
  id: number
  message: string
  link: string | null
  read: boolean
  createdAt: string
}

export const getUnreadNotifications = () =>
  api.get<NotificationDto[]>('/notifications/unread')

export const markAllNotificationsRead = () =>
  api.patch('/notifications/read-all')
