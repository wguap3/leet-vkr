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
          <svg className={styles.logoIcon} width="22" height="22" viewBox="0 0 22 22" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect x="1" y="1" width="20" height="20" rx="5" fill="url(#logo-grad)" />
            <path d="M14.5 4.5L8 11H13L7.5 17.5" stroke="white" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"/>
            <defs>
              <linearGradient id="logo-grad" x1="1" y1="1" x2="21" y2="21" gradientUnits="userSpaceOnUse">
                <stop stopColor="#8b5cf6"/>
                <stop offset="1" stopColor="#6d28d9"/>
              </linearGradient>
            </defs>
          </svg>
          Stackr
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
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" style={{flexShrink:0}}>
                <circle cx="12" cy="12" r="3"/>
                <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>
              </svg>
              Админ
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
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
                    <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
                  </svg>
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
