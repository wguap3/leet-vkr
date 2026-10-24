import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import * as authApi from '../api/auth'
import styles from './Navbar.module.css'

export default function Navbar() {
  const { user, logout, isAuthenticated } = useAuthStore()
  const navigate = useNavigate()

  const handleLogout = async () => {
    try { await authApi.logout() } catch {}
    logout()
    navigate('/')
  }

  const isAdmin = user?.role === 'ADMIN'
  const canCreateContest = user?.role === 'ORGANIZER' || user?.role === 'ADMIN'
  const auth = isAuthenticated()

  return (
    <nav className={styles.nav}>
      <div className={styles.inner}>
        <Link to="/" className={styles.logo}>
          <span className={styles.logoIcon}>⚡</span>Stackr
        </Link>

        <div className={styles.links}>
          {auth && <>
            <NavLink to="/problems" className={({ isActive }) => isActive ? styles.linkActive : styles.link}>Задачи</NavLink>
            <NavLink to="/contests" className={({ isActive }) => isActive ? styles.linkActive : styles.link}>Контесты</NavLink>
            <NavLink to="/leaderboard" className={({ isActive }) => isActive ? styles.linkActive : styles.link}>Рейтинг</NavLink>
            {isAdmin && <Link to="/problems/new" className={styles.createLink}>+ Задача</Link>}
            {canCreateContest && <Link to="/contests/new" className={styles.createLink}>+ Контест</Link>}
          </>}
        </div>

        <div className={styles.right}>
          {auth ? (
            <>
              <Link to="/profile" className={styles.userBtn}>
                <div className={styles.avatar}>{user?.username?.[0]?.toUpperCase()}</div>
                {user?.username}
              </Link>
              <button className="btn btn-outline btn-sm" onClick={handleLogout}>Выйти</button>
            </>
          ) : (
            <>
              <Link to="/login" className="btn btn-ghost btn-sm">Войти</Link>
              <Link to="/register" className="btn btn-primary btn-sm">Начать бесплатно</Link>
            </>
          )}
        </div>
      </div>
    </nav>
  )
}
