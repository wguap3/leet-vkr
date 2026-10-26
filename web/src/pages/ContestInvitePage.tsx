import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getContestByInviteToken, joinByInviteToken } from '../api/contests'
import { useAuthStore } from '../store/authStore'
import type { ContestDetail } from '../types'

export default function ContestInvitePage() {
  const { token } = useParams<{ token: string }>()
  const navigate = useNavigate()
  const { user, isAuthenticated } = useAuthStore()
  const auth = isAuthenticated()

  const [contest, setContest] = useState<ContestDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [joining, setJoining] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!token) return
    getContestByInviteToken(token)
      .then(({ data }) => setContest(data))
      .catch(() => setError('Недействительная ссылка-приглашение'))
      .finally(() => setLoading(false))
  }, [token])

  const handleJoin = async () => {
    if (!auth) {
      navigate(`/login?redirect=/contests/invite/${token}`)
      return
    }
    if (!token) return
    setJoining(true)
    try {
      const { data } = await joinByInviteToken(token)
      navigate(`/contests/${data.id}`)
    } catch (err: any) {
      setError(err.response?.data?.message || 'Ошибка при регистрации')
    } finally {
      setJoining(false)
    }
  }

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '60vh' }}>
        <span className="spinner" />
      </div>
    )
  }

  if (error || !contest) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: '60vh', gap: 16 }}>
        <div style={{ fontSize: 48, display:'flex', justifyContent:'center' }}>
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--text-3)" strokeWidth="1.5">
            <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/>
            <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/>
          </svg>
        </div>
        <h2 style={{ color: 'var(--text-1)' }}>Ссылка недействительна</h2>
        <p style={{ color: 'var(--text-3)', textAlign: 'center', maxWidth: 400 }}>
          {error || 'Ссылка-приглашение не найдена или устарела. Попросите организатора прислать актуальную ссылку.'}
        </p>
        <Link to="/contests" className="btn btn-outline">← Все контесты</Link>
      </div>
    )
  }

  const isRegistered = contest.isRegistered
  const isContestant = user?.role === 'CONTESTANT'

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '70vh', padding: '32px 16px' }}>
      <div className="card" style={{ maxWidth: 480, width: '100%', padding: 32, textAlign: 'center' }}>
        <div style={{
          width: 64, height: 64, borderRadius: '50%',
          background: 'linear-gradient(135deg, var(--violet), #a78bfa)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          margin: '0 auto 20px',
          fontSize: 28
        }}>
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
            <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
          </svg>
        </div>

        <h2 style={{ color: 'var(--text-1)', marginBottom: 8, fontSize: 22 }}>
          Приглашение в контест
        </h2>
        <p style={{ color: 'var(--text-3)', fontSize: 13, marginBottom: 24 }}>
          Вас приглашают принять участие в закрытом контесте
        </p>

        <div style={{
          background: 'var(--bg-3)', border: '1px solid var(--border)',
          borderRadius: 'var(--radius-md)', padding: '20px 24px',
          marginBottom: 24, textAlign: 'left'
        }}>
          <div style={{ fontWeight: 700, fontSize: 18, color: 'var(--text-1)', marginBottom: 12 }}>
            {contest.title}
          </div>
          {contest.description && (
            <p style={{ fontSize: 13, color: 'var(--text-2)', marginBottom: 12 }}>{contest.description}</p>
          )}
          <div style={{ display: 'flex', gap: 16, fontSize: 12, color: 'var(--text-3)' }}>
            <span style={{display:'flex',alignItems:'center',gap:4}}>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
              {new Date(contest.startTime).toLocaleString('ru-RU')}
            </span>
            <span style={{display:'flex',alignItems:'center',gap:4}}>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
              до {new Date(contest.endTime).toLocaleString('ru-RU')}
            </span>
          </div>
          <div style={{ marginTop: 10, display: 'flex', gap: 8 }}>
            <span className={`badge badge-${contest.status === 'RUNNING' ? 'ac' : contest.status === 'UPCOMING' ? 'pending' : 'wa'}`}>
              {contest.status === 'RUNNING' ? 'Идёт' : contest.status === 'UPCOMING' ? 'Скоро' : 'Завершён'}
            </span>
            <span className="badge badge-wa" style={{display:'inline-flex',alignItems:'center',gap:4}}>
              <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
              Приватный
            </span>
          </div>
        </div>

        {isRegistered ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            <div className="badge badge-ac" style={{ padding: '10px 16px', fontSize: 14 }}>
              ✓ Вы уже зарегистрированы
            </div>
            <Link to={`/contests/${contest.id}`} className="btn btn-primary">
              Перейти к контесту →
            </Link>
          </div>
        ) : !auth ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            <p style={{ color: 'var(--text-3)', fontSize: 13, marginBottom: 4 }}>
              Войдите в аккаунт чтобы зарегистрироваться
            </p>
            <Link to={`/login?redirect=/contests/invite/${token}`} className="btn btn-primary">
              Войти
            </Link>
            <Link to={`/register`} className="btn btn-outline">
              Создать аккаунт
            </Link>
          </div>
        ) : !isContestant ? (
          <div>
            <p style={{ color: 'var(--text-3)', fontSize: 13 }}>
              Только участники (роль CONTESTANT) могут регистрироваться на контесты.
            </p>
            <Link to={`/contests/${contest.id}`} className="btn btn-outline" style={{ marginTop: 12 }}>
              Посмотреть контест
            </Link>
          </div>
        ) : contest.status === 'FINISHED' ? (
          <div>
            <p style={{ color: 'var(--red)', fontSize: 13, marginBottom: 12 }}>Контест уже завершён</p>
            <Link to={`/contests/${contest.id}`} className="btn btn-outline">Посмотреть результаты</Link>
          </div>
        ) : (
          <button
            className="btn btn-primary"
            style={{ width: '100%', justifyContent: 'center' }}
            onClick={handleJoin}
            disabled={joining}
          >
            {joining ? <span className="spinner" /> : '🎯 Зарегистрироваться и войти'}
          </button>
        )}

        <div style={{ marginTop: 16 }}>
          <Link to="/contests" style={{ fontSize: 12, color: 'var(--text-3)' }}>← Все контесты</Link>
        </div>
      </div>
    </div>
  )
}
