import api from './client'
import type { Hint, Page, ProblemDetail, ProblemShort, ProblemStats, RunCodeResult, TestCase } from '../types'

export const getProblems = (params?: {
  difficulty?: string; tagId?: number; search?: string; page?: number; size?: number
}) => api.get<Page<ProblemShort>>('/problems', { params })

export const getProblem = (id: number) =>
  api.get<ProblemDetail>(`/problems/${id}`)

export const submit = (problemId: number, sourceCode: string, contestId?: number) =>
  api.post('/submissions', { problemId, sourceCode, contestId })

export const runCode = (problemId: number, sourceCode: string, customInput: string) =>
  api.post<RunCodeResult>('/submissions/run', { problemId, sourceCode, customInput })

export const getProblemStats = (id: number) =>
  api.get<ProblemStats>(`/problems/${id}/stats`)

export const getHints = (problemId: number) =>
  api.get<Hint[]>(`/problems/${problemId}/hints`)

export const createHint = (problemId: number, data: { text: string; orderIndex: number; unlocksAfterAttempts: number }) =>
  api.post<Hint>(`/problems/${problemId}/hints`, data)

export const deleteHint = (problemId: number, hintId: number) =>
  api.delete(`/problems/${problemId}/hints/${hintId}`)

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

export const getTestCases = (problemId: number) =>
  api.get<TestCase[]>(`/problems/${problemId}/testcases`)

export const updateTestCase = (problemId: number, id: number, data: {
  inputData?: string
  expectedOutput?: string
  isPublic?: boolean
  orderIndex?: number
}) => api.put<TestCase>(`/problems/${problemId}/testcases/${id}`, data)

export const updateProblem = (id: number, data: {
  title?: string
  description?: string
  difficulty?: string
  timeLimitMs?: number
  memoryLimitMb?: number
  tagIds?: number[]
  codeTemplate?: string
}) => api.put<import('../types').ProblemDetail>(`/problems/${id}`, data)

export const getHintsFull = (problemId: number) =>
  api.get<import('../types').Hint[]>(`/problems/${problemId}/hints`)
