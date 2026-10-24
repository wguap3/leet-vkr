import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import Navbar from './components/Navbar'
import LandingPage from './pages/LandingPage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import ProblemsPage from './pages/ProblemsPage'
import ProblemDetailPage from './pages/ProblemDetailPage'
import ContestsPage from './pages/ContestsPage'
import ContestDetailPage from './pages/ContestDetailPage'
import ProfilePage from './pages/ProfilePage'
import LeaderboardPage from './pages/LeaderboardPage'
import CreateProblemPage from './pages/CreateProblemPage'
import CreateContestPage from './pages/CreateContestPage'
import ContestAnalyticsPage from './pages/ContestAnalyticsPage'
import AdminPanel from './pages/AdminPanel'
import { useAuthStore } from './store/authStore'
import type { Role } from './types'

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuthStore()
  return isAuthenticated() ? <>{children}</> : <Navigate to="/login" replace />
}

function OrganizerRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, user } = useAuthStore()
  const allowed: Role[] = ['ORGANIZER', 'ADMIN']
  if (!isAuthenticated()) return <Navigate to="/login" replace />
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
  return (
    <BrowserRouter>
      <Navbar />
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/problems" element={<ProtectedRoute><ProblemsPage /></ProtectedRoute>} />
        <Route path="/problems/new" element={<OrganizerRoute><CreateProblemPage /></OrganizerRoute>} />
        <Route path="/problems/:id" element={<ProtectedRoute><ProblemDetailPage /></ProtectedRoute>} />
        <Route path="/contests" element={<ProtectedRoute><ContestsPage /></ProtectedRoute>} />
        <Route path="/contests/new" element={<OrganizerRoute><CreateContestPage /></OrganizerRoute>} />
        <Route path="/contests/:id" element={<ProtectedRoute><ContestDetailPage /></ProtectedRoute>} />
        <Route path="/contests/:id/analytics" element={<OrganizerRoute><ContestAnalyticsPage /></OrganizerRoute>} />
        <Route path="/profile" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
        <Route path="/leaderboard" element={<ProtectedRoute><LeaderboardPage /></ProtectedRoute>} />
        <Route path="/admin" element={<AdminRoute><AdminPanel /></AdminRoute>} />
      </Routes>
    </BrowserRouter>
  )
}
