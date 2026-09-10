import { useEffect, useState } from 'react'
import api from '../api/client'
import type { Submission, UserProfile } from '../types'
import styles from './ProfilePage.module.css'

const VERDICT_LABELS: Record<string, string> = {
  AC: 'Принято', WA: 'Неверный ответ', TLE: 'Превышено время',
  RE: 'Ошибка выполнения', CE: 'Ошибка компиляции', PENDING: 'Проверяется'
}

const ROLE_LABELS: Record<string, string> = {
  CONTESTANT: 'Участник', ORGANIZER: 'Организатор', ADMIN: 'Администратор'
}

function VerdictBadge({ v }: { v: string }) {
  return <span className={`badge badge-${v.toLowerCase()}`}>{VERDICT_LABELS[v] ?? v}</span>
}

export default function ProfilePage() {
  const [profile, setProfile] = useState<UserProfile | null>(null)
  const [submissions, setSubmissions] = useState<Submission[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([
      api.get<UserProfile>('/users/me'),
      api.get<{ content: Submission[] }>('/submissions/my?size=10')
    ]).then(([p, s]) => {
      setProfile(p.data)
      setSubmissions(s.data.content)
    }).finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="empty-state" style={{ marginTop: 80 }}><span className="spinner" /></div>
  if (!profile) return null

  return (
    <div className="page-container">
      <div className={styles.profileHeader}>
        <div className={styles.avatar}>
          {profile.avatarUrl
            ? <img src={profile.avatarUrl} alt={profile.username} />
            : <span>{profile.username[0].toUpperCase()}</span>
          }
        </div>
        <div>
          <h1>{profile.username}</h1>
          <p style={{ color: 'var(--text-2)', fontSize: 14 }}>{profile.email}</p>
          <div className={styles.badges}>
            <span className="badge badge-pending">{ROLE_LABELS[profile.role] ?? profile.role}</span>
          </div>
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
      </div>

      <h2 style={{ fontSize: 16, fontWeight: 600, marginBottom: 12, marginTop: 28 }}>Последние посылки</h2>
      <div className="card">
        {submissions.length === 0 ? (
          <div className="empty-state">Посылок пока нет</div>
        ) : (
          <table className="table">
            <thead>
              <tr><th>Задача</th><th>Вердикт</th><th>Время</th><th>Отправлено</th></tr>
            </thead>
            <tbody>
              {submissions.map(s => (
                <tr key={s.id}>
                  <td>{s.problemTitle}</td>
                  <td><VerdictBadge v={s.verdict} /></td>
                  <td style={{ color: 'var(--text-2)', fontSize: 13 }}>
                    {s.executionTimeMs ? `${s.executionTimeMs} мс` : '—'}
                  </td>
                  <td style={{ color: 'var(--text-2)', fontSize: 13 }}>
                    {new Date(s.submittedAt).toLocaleString('ru-RU')}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
