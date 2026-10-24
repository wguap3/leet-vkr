import { useEffect, useState } from 'react'
import api from '../api/client'
import { changePassword } from '../api/users'
import type { Submission, TagProgress, UserProfile } from '../types'
import RatingBadge from '../components/RatingBadge'
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
  const [progress, setProgress] = useState<TagProgress[]>([])
  const [loading, setLoading] = useState(true)
  const [oldPwd, setOldPwd] = useState('')
  const [newPwd, setNewPwd] = useState('')
  const [pwdError, setPwdError] = useState('')
  const [pwdSuccess, setPwdSuccess] = useState(false)
  const [pwdLoading, setPwdLoading] = useState(false)

  useEffect(() => {
    Promise.all([
      api.get<UserProfile>('/users/me'),
      api.get<{ content: Submission[] }>('/submissions/my?size=10'),
      api.get<TagProgress[]>('/users/me/progress'),
    ]).then(([p, s, pr]) => {
      setProfile(p.data)
      setSubmissions(s.data.content)
      setProgress(pr.data)
    }).finally(() => setLoading(false))
  }, [])

  async function handleChangePassword(e: React.FormEvent) {
    e.preventDefault()
    setPwdError('')
    setPwdSuccess(false)
    if (newPwd.length < 6) { setPwdError('Новый пароль должен быть не менее 6 символов'); return }
    setPwdLoading(true)
    try {
      await changePassword(oldPwd, newPwd)
      setPwdSuccess(true)
      setOldPwd('')
      setNewPwd('')
    } catch (err: any) {
      const msg = err?.response?.data?.message ?? 'Ошибка смены пароля'
      setPwdError(msg)
    } finally {
      setPwdLoading(false)
    }
  }

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
          <div className={styles.badges} style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap', marginTop: 6 }}>
            <span className="badge badge-pending">{ROLE_LABELS[profile.role] ?? profile.role}</span>
            <RatingBadge rating={profile.rating} size="sm" />
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

      {progress.length > 0 && (
        <>
          <h2 style={{ fontSize: 16, fontWeight: 600, marginBottom: 12, marginTop: 28 }}>Прогресс по темам</h2>
          <div className="card" style={{ padding: '20px 24px' }}>
            <div className={styles.progressGrid}>
              {progress.map(p => {
                const pct = p.total > 0 ? (p.solved / p.total) * 100 : 0
                return (
                  <div key={p.tagId} className={styles.progressItem}>
                    <div className={styles.progressLabel}>
                      <span>{p.tagName}</span>
                      <span style={{ color: 'var(--text-3)', fontSize: 12 }}>{p.solved}/{p.total}</span>
                    </div>
                    <div className={styles.progressTrack}>
                      <div
                        className={styles.progressFill}
                        style={{ width: `${pct}%`, background: pct >= 80 ? 'var(--green)' : pct >= 40 ? 'var(--violet)' : 'var(--blue)' }}
                      />
                    </div>
                  </div>
                )
              })}
            </div>
          </div>
        </>
      )}

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
      <h2 style={{ fontSize: 16, fontWeight: 600, marginBottom: 12, marginTop: 28 }}>Сменить пароль</h2>
      <div className="card" style={{ padding: '20px 24px', maxWidth: 420 }}>
        <form onSubmit={handleChangePassword} style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <div>
            <label style={{ fontSize: 13, color: 'var(--text-2)', display: 'block', marginBottom: 4 }}>
              Текущий пароль
            </label>
            <input
              type="password"
              className="input"
              value={oldPwd}
              onChange={e => setOldPwd(e.target.value)}
              required
              autoComplete="current-password"
            />
          </div>
          <div>
            <label style={{ fontSize: 13, color: 'var(--text-2)', display: 'block', marginBottom: 4 }}>
              Новый пароль
            </label>
            <input
              type="password"
              className="input"
              value={newPwd}
              onChange={e => setNewPwd(e.target.value)}
              required
              minLength={6}
              autoComplete="new-password"
            />
          </div>
          {pwdError && (
            <p style={{ color: 'var(--red)', fontSize: 13, margin: 0 }}>{pwdError}</p>
          )}
          {pwdSuccess && (
            <p style={{ color: 'var(--green)', fontSize: 13, margin: 0 }}>Пароль успешно изменён ✓</p>
          )}
          <button type="submit" className="btn btn-primary" disabled={pwdLoading} style={{ alignSelf: 'flex-start' }}>
            {pwdLoading ? 'Сохранение…' : 'Сменить пароль'}
          </button>
        </form>
      </div>

    </div>
  )
}
