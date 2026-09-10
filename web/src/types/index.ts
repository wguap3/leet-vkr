export type Role = 'CONTESTANT' | 'ORGANIZER' | 'ADMIN'
export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD'
export type Verdict = 'PENDING' | 'AC' | 'WA' | 'TLE' | 'RE' | 'CE'
export type ContestStatus = 'UPCOMING' | 'RUNNING' | 'FINISHED'

export interface Tag {
  id: number
  name: string
}

export interface User {
  id: number
  username: string
  email: string
  role: Role
  rating: number
  registeredAt: string
}

export interface UserProfile extends User {
  avatarUrl?: string
  totalAC: number
  totalSubmissions: number
}

export interface ProblemShort {
  id: number
  title: string
  difficulty: Difficulty
  tags: Tag[]
  solvedCount: number
  userVerdict: Verdict | null
}

export interface ProblemDetail {
  id: number
  title: string
  description: string
  difficulty: Difficulty
  timeLimitMs: number
  memoryLimitMb: number
  tags: Tag[]
  codeTemplate: string | null
  publicTestCases: TestCase[]
}

export interface TestCase {
  id: number
  inputData: string
  expectedOutput: string
  isPublic: boolean
  orderIndex: number
}

export interface Submission {
  id: number
  userId: number
  username: string
  problemId: number
  problemTitle: string
  verdict: Verdict
  executionTimeMs: number | null
  memoryUsedMb: number | null
  submittedAt: string
  sourceCode?: string
}

export interface ContestShort {
  id: number
  title: string
  startTime: string
  endTime: string
  status: ContestStatus
  problemCount: number
  participantCount: number
}

export interface ContestDetail {
  id: number
  title: string
  description: string
  startTime: string
  endTime: string
  status: ContestStatus
  problems: ProblemShort[]
  isRegistered: boolean
}

export interface LeaderboardEntry {
  rank: number
  userId: number
  username: string
  score: number
  penalty: number
  submissionCount: number
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}
