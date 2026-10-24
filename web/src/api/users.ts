import api from './client'
import type { Page, UserProfile, Role } from '../types'

export interface UserAdmin {
  id: number
  username: string
  email: string
  role: Role
  rating: number
  active: boolean
  registeredAt: string
}

export const getMe = () => api.get<UserProfile>('/users/me')

export const updateMe = (data: { avatarUrl?: string }) =>
  api.put<UserProfile>('/users/me', data)

export const getMyProgress = () =>
  api.get<{ tagId: number; tagName: string; solved: number; total: number }[]>('/users/me/progress')

export const getLeaderboard = (params?: { page?: number; size?: number }) =>
  api.get<Page<UserProfile>>('/users/leaderboard', { params })

export const getProfile = (id: number) => api.get<UserProfile>(`/users/${id}`)

// ── Admin ──────────────────────────────────────────────────────────────────
export const getAllUsers = (params?: { page?: number; size?: number }) =>
  api.get<Page<UserAdmin>>('/users', { params })

export const changeRole = (id: number, role: Role) =>
  api.patch(`/users/${id}/role?role=${role}`)

export const setActive = (id: number, active: boolean) =>
  api.patch(`/users/${id}/active?active=${active}`)

export const changePassword = (oldPassword: string, newPassword: string) =>
  api.post('/users/me/password', { oldPassword, newPassword })
