import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import api from '../api/client'
import type { UserProfile } from '../types'
import RatingBadge from '../components/RatingBadge'
import styles from './ProfilePage.module.css'

const ROLE_LABELS: Record<string, string> = {
  CONTESTANT: 'Участник', ORGANIZER: 'Организатор', ADMIN: 'Администратор'
}

export default function PublicProfilePage() {
  const { id } = useParams<{ id: string }>()
  const [profile, setProfile] = useState<UserProfile | null>(null)
  const [loading, setLoading] = useState(true)
  const [notFound, setNotFound] = useState(false)

  useEffect(() => {
    if (!id) return
    api.get<UserProfile>(`/users/${id}`)
      .then(({ data }) => setProfile(data))
      .catch(() => setNotFound(true))
      .finally(() => setLoading(false))
  }, [id])

  if (loading) return <div className="empty-state" style={{ marginTop: 80 }}><span className="spinner" /></div>
  if (notFound || !profile) return (
    <div className="page-container">
      <div className="empty-state" style={{ marginTop: 80 }}>
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" style={{marginBottom:12,color:'var(--text-3)'}}><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        <p style={{ fontWeight: 600, marginBottom: 8 }}>Пользователь не найден</p>
        <Link to="/leaderboard" className="btn btn-outline btn-sm">← Рейтинг</Link>
      </div>
    </div>
  )

  return (
    <div className="page-container">
      <div style={{ marginBottom: 20 }}>
        <Link to="/leaderboard" style={{ color: 'var(--text-3)', fontSize: 13, textDecoration: 'none' }}>
          ← Рейтинг
        </Link>
      </div>

      <div className={styles.profileHeader}>
        <div className={styles.avatar}>
          {profile.avatarUrl
            ? <img src={profile.avatarUrl} alt={profile.username} />
            : <span>{profile.username[0].toUpperCase()}</span>
          }
        </div>
        <div>
          <h1>{profile.username}</h1>
          <div className={styles.badges} style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap', marginTop: 6 }}>
            <span className="badge badge-pending">{ROLE_LABELS[profile.role] ?? profile.role}</span>
            <RatingBadge rating={profile.rating} size="sm" />
          </div>
          {profile.registeredAt && (
            <p style={{ color: 'var(--text-3)', fontSize: 12, marginTop: 6 }}>
              Регистрация: {new Date(profile.registeredAt).toLocaleDateString('ru-RU')}
            </p>
          )}
        </div>
      </div>

      <div className={styles.stats}>
        <div className={`card ${styles.statCard}`}>
          <div className={styles.statValue}>{profile.rating}</div>
          <div className={styles.statLabel}>Рейтинг</div>
        </div>
        <div className={`card ${styles.statCard}`}>
          <div className={styles.statValue}>{profile.totalAC}</div>
          <div className={styles.statLabel}>Задач решено</div>
        </div>
        <div className={`card ${styles.statCard}`}>
          <div className={styles.statValue}>{profile.totalSubmissions}</div>
          <div className={styles.statLabel}>Всего посылок</div>
        </div>
        {profile.totalSubmissions > 0 && (
          <div className={`card ${styles.statCard}`}>
            <div className={styles.statValue}>
              {Math.round((profile.totalAC / profile.totalSubmissions) * 100)}%
            </div>
            <div className={styles.statLabel}>Успех</div>
          </div>
        )}
      </div>
    </div>
  )
}
