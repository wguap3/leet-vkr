import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom'
import Navbar from './components/Navbar'
import Toast from './components/Toast'
import ErrorBoundary from './components/ErrorBoundary'
import { getMe } from './api/auth'
import LandingPage from './pages/LandingPage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import ProblemsPage from './pages/ProblemsPage'
import LearnPage from './pages/LearnPage'
import { lazy, Suspense, useEffect } from 'react'
const ProblemDetailPage = lazy(() => import('./pages/ProblemDetailPage'))
import ContestsPage from './pages/ContestsPage'
import ContestDetailPage from './pages/ContestDetailPage'
import ProfilePage from './pages/ProfilePage'
import PublicProfilePage from './pages/PublicProfilePage'
import LeaderboardPage from './pages/LeaderboardPage'
import CreateProblemPage from './pages/CreateProblemPage'
import CreateContestPage from './pages/CreateContestPage'
import ContestAnalyticsPage from './pages/ContestAnalyticsPage'
import ContestStandingsPage from './pages/ContestStandingsPage'
import EditProblemPage from './pages/EditProblemPage'
import EditContestPage from './pages/EditContestPage'
import ContestInvitePage from './pages/ContestInvitePage'
import AdminPanel from './pages/AdminPanel'
import TestCasesPage from './pages/TestCasesPage'
import { useAuthStore } from './store/authStore'
import type { Role } from './types'

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const location = useLocation()
  const { isAuthenticated } = useAuthStore()
  return isAuthenticated() ? <>{children}</> : <Navigate to={`/login?redirect=${encodeURIComponent(location.pathname + location.search)}`} replace />
}

function OrganizerRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, user } = useAuthStore()
  const location = useLocation()
  const allowed: Role[] = ['ORGANIZER', 'ADMIN']
  if (!isAuthenticated()) return <Navigate to={`/login?redirect=${encodeURIComponent(location.pathname + location.search)}`} replace />
  if (!user || !allowed.includes(user.role)) return <Navigate to="/problems" replace />
  return <>{children}</>
}

function AdminRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, user } = useAuthStore()
  if (!isAuthenticated()) return <Navigate to="/login" replace />
  if (!user || user.role !== 'ADMIN') return <Navigate to="/problems" replace />
  return <>{children}</>
}

export default function App() {
  const { accessToken, setUser } = useAuthStore()
  useEffect(() => { if (accessToken) getMe().then(r => setUser(r.data)).catch(() => {}) }, [accessToken, setUser])
  return (
    <BrowserRouter>
      <a className="skip-link" href="#main-content">Перейти к содержимому</a>
      <Navbar /><Toast />
      <main id="main-content"><ErrorBoundary>
      <Suspense fallback={<div className="empty-state" role="status"><span className="spinner" />Загрузка страницы…</div>}>
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/learn" element={<ProtectedRoute><LearnPage /></ProtectedRoute>} />
        <Route path="/problems" element={<ProtectedRoute><ProblemsPage /></ProtectedRoute>} />
        <Route path="/problems/new" element={<OrganizerRoute><CreateProblemPage /></OrganizerRoute>} />
        <Route path="/problems/:id/edit" element={<OrganizerRoute><EditProblemPage /></OrganizerRoute>} />
        <Route path="/problems/:id" element={<ProtectedRoute><ProblemDetailPage /></ProtectedRoute>} />
        <Route path="/contests" element={<ProtectedRoute><ContestsPage /></ProtectedRoute>} />
        <Route path="/contests/new" element={<OrganizerRoute><CreateContestPage /></OrganizerRoute>} />
        <Route path="/contests/:id/edit" element={<OrganizerRoute><EditContestPage /></OrganizerRoute>} />
        <Route path="/contests/invite/:token" element={<ContestInvitePage />} />
        <Route path="/contests/:id" element={<ProtectedRoute><ContestDetailPage /></ProtectedRoute>} />
        <Route path="/contests/:id/analytics" element={<OrganizerRoute><ContestAnalyticsPage /></OrganizerRoute>} />
        <Route path="/contests/:id/standings" element={<ProtectedRoute><ContestStandingsPage /></ProtectedRoute>} />
        <Route path="/profile" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
        <Route path="/profile/:id" element={<ProtectedRoute><PublicProfilePage /></ProtectedRoute>} />
        <Route path="/leaderboard" element={<ProtectedRoute><LeaderboardPage /></ProtectedRoute>} />
        <Route path="/problems/:id/testcases" element={<OrganizerRoute><TestCasesPage /></OrganizerRoute>} />
        <Route path="/admin" element={<AdminRoute><AdminPanel /></AdminRoute>} />
        <Route path="*" element={<div className="empty-state"><p className="eyebrow">404</p><h1>Такой страницы нет</h1><p>Вернитесь в каталог и выберите следующую задачу.</p><a href="/problems" className="btn btn-primary">К задачам</a></div>} />
      </Routes>
      </Suspense>
      </ErrorBoundary></main>
    </BrowserRouter>
  )
}
