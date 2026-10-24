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
        <div style={{ fontSize: 48 }}>🔗</div>
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
        }}>🔒</div>

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
            <span>📅 {new Date(contest.startTime).toLocaleString('ru-RU')}</span>
            <span>⏱ {new Date(contest.endTime).toLocaleString('ru-RU')}</span>
          </div>
          <div style={{ marginTop: 10, display: 'flex', gap: 8 }}>
            <span className={`badge badge-${contest.status === 'RUNNING' ? 'ac' : contest.status === 'UPCOMING' ? 'pending' : 'wa'}`}>
              {contest.status === 'RUNNING' ? 'Идёт' : contest.status === 'UPCOMING' ? 'Скоро' : 'Завершён'}
            </span>
            <span className="badge badge-wa">🔒 Приватный</span>
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
