import { useEffect, useRef, useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import { getUnreadNotifications, markAllNotificationsRead, type NotificationDto } from '../api/notifications'
import styles from './Navbar.module.css'

export default function Navbar() {
  const { user, logout, isAuthenticated } = useAuthStore()
  const navigate = useNavigate()
  const auth = isAuthenticated()
  const isAdmin = user?.role === 'ADMIN'
  const canCreate = user?.role === 'ORGANIZER' || user?.role === 'ADMIN'

  const [notifications, setNotifications] = useState<NotificationDto[]>([])
  const [open, setOpen] = useState(false)
  const dropdownRef = useRef<HTMLDivElement>(null)

  // Close dropdown on outside click
  useEffect(() => {
    if (!open) return
    const handler = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [open])

  // Poll for unread notifications every 5s when authenticated
  useEffect(() => {
    if (!auth) return
    const fetchNotifications = () => {
      getUnreadNotifications()
        .then(res => setNotifications(res.data.slice(0, 10)))
        .catch(() => {/* silent */})
    }
    fetchNotifications()
    const interval = setInterval(fetchNotifications, 5000)
    return () => clearInterval(interval)
  }, [auth])

  const handleBellClick = async () => {
    if (open) {
      // Closing: mark all as read
      if (notifications.length > 0) {
        await markAllNotificationsRead().catch(() => {/* silent */})
        setNotifications([])
      }
      setOpen(false)
    } else {
      setOpen(true)
    }
  }

  const handleNotificationClick = (link: string | null) => {
    setOpen(false)
    markAllNotificationsRead().catch(() => {/* silent */})
    setNotifications([])
    if (link) navigate(link)
  }

  const handleLogout = () => {
    logout()
    navigate('/')
  }

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
            {canCreate && (
              <div className={styles.createGroup}>
                {isAdmin && <Link to="/problems/new" className={styles.createLink}>+ Задача</Link>}
                <Link to="/contests/new" className={styles.createLink}>+ Контест</Link>
              </div>
            )}
            {isAdmin && (
              <NavLink to="/admin" className={({ isActive }) => isActive ? styles.adminLinkActive : styles.adminLink}>
                ⚙ Админ
              </NavLink>
            )}
          </>}
        </div>

        <div className={styles.right}>
          {auth ? (
            <>
              {/* Bell notification */}
              <div className={styles.bellWrap} ref={dropdownRef}>
                <button
                  className={styles.bellBtn}
                  onClick={handleBellClick}
                  title="Уведомления"
                >
                  🔔
                  {notifications.length > 0 && (
                    <span className={styles.badge}>
                      {notifications.length > 9 ? '9+' : notifications.length}
                    </span>
                  )}
                </button>

                {open && (
                  <div className={styles.dropdown}>
                    <div className={styles.dropdownHeader}>Уведомления</div>
                    {notifications.length === 0 ? (
                      <div className={styles.dropdownEmpty}>Нет новых уведомлений</div>
                    ) : (
                      notifications.map(n => (
                        <button
                          key={n.id}
                          className={styles.notifItem}
                          onClick={() => handleNotificationClick(n.link)}
                        >
                          <span className={styles.notifMsg}>{n.message}</span>
                          <span className={styles.notifTime}>
                            {new Date(n.createdAt).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })}
                          </span>
                        </button>
                      ))
                    )}
                    {notifications.length > 0 && (
                      <button
                        className={styles.dropdownMarkAll}
                        onClick={async () => {
                          await markAllNotificationsRead().catch(() => {/* silent */})
                          setNotifications([])
                          setOpen(false)
                        }}
                      >
                        Отметить все прочитанными
                      </button>
                    )}
                  </div>
                )}
              </div>

              <Link to="/profile" className={styles.userBtn}>
                <div className={styles.avatar}>{user?.username?.[0]?.toUpperCase()}</div>
                <span className={styles.username}>{user?.username}</span>
                {user?.rating != null && (
                  <span className={styles.ratingPill}>{user.rating}</span>
                )}
              </Link>
              <button className="btn btn-outline btn-sm" onClick={handleLogout}>Выйти</button>
            </>
          ) : (
            <>
              <Link to="/login" className="btn btn-ghost btn-sm">Войти</Link>
              <Link to="/register" className="btn btn-primary btn-sm">Начать →</Link>
            </>
          )}
        </div>
      </div>
    </nav>
  )
}
