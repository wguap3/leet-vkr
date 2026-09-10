import api from './client'
import type { Page, ProblemDetail, ProblemShort, TestCase } from '../types'

export const getProblems = (params?: {
  difficulty?: string; tagId?: number; page?: number; size?: number
}) => api.get<Page<ProblemShort>>('/problems', { params })

export const getProblem = (id: number) =>
  api.get<ProblemDetail>(`/problems/${id}`)

export const submit = (problemId: number, sourceCode: string, contestId?: number) =>
  api.post('/submissions', { problemId, sourceCode, contestId })

export const createProblem = (data: {
  title: string
  description: string
  difficulty: string
  timeLimitMs: number
  memoryLimitMb: number
  tagIds: number[]
  codeTemplate?: string
}) => api.post<ProblemDetail>('/problems', data)

export const addTestCase = (problemId: number, data: {
  inputData: string
  expectedOutput: string
  isPublic: boolean
  orderIndex: number
}) => api.post<TestCase>(`/problems/${problemId}/testcases`, data)

export const deleteTestCase = (problemId: number, testCaseId: number) =>
  api.delete(`/problems/${problemId}/testcases/${testCaseId}`)

export const setVisible = (problemId: number, visible: boolean) =>
  api.patch(`/problems/${problemId}/visibility?visible=${visible}`)

export const getTags = () =>
  api.get<{ id: number; name: string }[]>('/tags')
