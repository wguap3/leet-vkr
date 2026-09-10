import api from './client'
import type { UserProfile } from '../types'

export const register = (username: string, email: string, password: string) =>
  api.post('/auth/register', { username, email, password })

export const login = (username: string, password: string) =>
  api.post<{ accessToken: string; refreshToken: string; expiresIn: number }>(
    '/auth/login', { username, password }
  )

export const getMe = () => api.get<UserProfile>('/users/me')

export const logout = () => api.post('/auth/logout')
