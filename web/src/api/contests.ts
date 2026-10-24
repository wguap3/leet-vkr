import api from './client'
import type { ContestAnalytics, ContestDetail, ContestShort, LeaderboardEntry } from '../types'

export const getContests = (status?: string) =>
  api.get<ContestShort[]>('/contests', { params: { status } })

export const getContest = (id: number) =>
  api.get<ContestDetail>(`/contests/${id}`)

export const registerForContest = (id: number) =>
  api.post(`/contests/${id}/register`)

export const getLeaderboard = (id: number) =>
  api.get<LeaderboardEntry[]>(`/contests/${id}/leaderboard`)

export const getContestAnalytics = (id: number) =>
  api.get<ContestAnalytics>(`/contests/${id}/analytics`)

export const createContest = (data: {
  title: string
  description: string
  startTime: string
  endTime: string
  problemIds: number[]
  isPublic: boolean
}) => api.post<ContestDetail>('/contests', data)

export const getContestStandings = (id: number) =>
  api.get<import('../types').ContestStandings>(`/contests/${id}/standings`)
