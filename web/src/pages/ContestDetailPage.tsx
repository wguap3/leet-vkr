import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import * as contestsApi from '../api/contests'
import { useAuthStore } from '../store/authStore'
import type { ContestDetail, LeaderboardEntry } from '../types'
import styles from './ContestDetailPage.module.css'

const STATUS_LABELS: Record<string, string> = {
  UPCOMING: 'Скоро', RUNNING: 'Идёт', FINISHED: 'Завершён'
}
const STATUS_BADGE: Record<string, string> = {
  RUNNING: 'ac', UPCOMING: 'pending', FINISHED: 'wa'
}
const DIFF_LABELS: Record<string, string> = {
  EASY: 'Лёгкая', MEDIUM: 'Средняя', HARD: 'Сложная'
}


// ── Countdown ─────────────────────────────────────────────────────────────────
function useCountdown(targetIso: string | null) {
  const [ms, setMs] = useState(() => targetIso ? Math.max(0, new Date(targetIso).getTime() - Date.now()) : 0)
  useEffect(() => {
    if (!targetIso) return
    const tick = () => setMs(Math.max(0, new Date(targetIso).getTime() - Date.now()))
    tick()
    const id = setInterval(tick, 1000)
    return () => clearInterval(id)
  }, [targetIso])
  return ms
}

function fmtCountdown(ms: number) {
  const s = Math.floor(ms / 1000)
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  return [h, m, sec].map(v => String(v).padStart(2, '0')).join(':')
}

export default function ContestDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { user } = useAuthStore()
  const isAdmin = user?.role === 'ADMIN'
  const canOrganize = user?.role === 'ORGANIZER' || isAdmin
  const [contest, setContest] = useState<ContestDetail | null>(null)
  const [leaderboard, setLeaderboard] = useState<LeaderboardEntry[]>([])
  const [loading, setLoading] = useState(true)
  const [tab, setTab] = useState<'problems' | 'leaderboard'>('problems')
  const [registering, setRegistering] = useState(false)
  const [copySuccess, setCopySuccess] = useState(false)
  const isOwnContest = contest?.organizerId != null && user?.id != null && contest.organizerId === user.id

  const countdownTarget = contest?.status === 'RUNNING'
    ? contest.endTime
    : contest?.status === 'UPCOMING'
    ? contest.startTime
    : null
  const countdownMs = useCountdown(countdownTarget)

  const load = () => {
    contestsApi.getContest(Number(id))
      .then(({ data }) => setContest(data))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [id])

  useEffect(() => {
    if (tab === 'leaderboard') {
      contestsApi.getLeaderboard(Number(id)).then(({ data }) => setLeaderboard(data))
    }
  }, [tab, id])

  const handleRegister = async () => {
    setRegistering(true)
    try {
      await contestsApi.registerForContest(Number(id))
      load()
    } catch (err: any) {
      alert(err.response?.data?.message || 'Ошибка регистрации')
    } finally {
      setRegistering(false)
    }
  }

  if (loading) return <div className="empty-state" style={{ marginTop: 80 }}><span className="spinner" /></div>
  if (!contest) return <div className="empty-state">Контест не найден</div>

  return (
    <div className="page-container">
      <div className={styles.header}>
        <div>
          <h1>{contest.title}</h1>
          {(contest.status === 'RUNNING' || contest.status === 'UPCOMING') && (
            <div style={{
              marginTop: 10,
              fontSize: 22,
              fontWeight: 800,
              fontVariantNumeric: 'tabular-nums',
              letterSpacing: '0.04em',
              color: contest.status === 'RUNNING'
                ? (countdownMs < 5 * 60 * 1000 ? 'var(--red)' : countdownMs < 15 * 60 * 1000 ? 'var(--yellow)' : 'var(--green)')
                : 'var(--text-2)',
            }}>
              {contest.status === 'RUNNING' ? (
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" style={{display:'inline',verticalAlign:'middle',marginRight:4}}><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
              ) : (
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" style={{display:'inline',verticalAlign:'middle',marginRight:4}}><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
              )}
              {fmtCountdown(countdownMs)}
              <span style={{ fontSize: 12, fontWeight: 400, color: 'var(--text-3)', marginLeft: 6 }}>
                {contest.status === 'RUNNING' ? 'осталось' : 'до начала'}
              </span>
            </div>
          )}
          <div className={styles.meta}>
            <span className={`badge badge-${STATUS_BADGE[contest.status]}`}>
              {STATUS_LABELS[contest.status]}
            </span>
            <span className={'badge ' + (contest.isPublic ? 'badge-ac' : 'badge-wa')} style={{ fontSize: 11 }}>
              {contest.isPublic ? (
                <><svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{verticalAlign:'middle',marginRight:3}}><circle cx="12" cy="12" r="10"/><line x1="2" y1="12" x2="22" y2="12"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/></svg>Публичный</>
              ) : (
                <><svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{verticalAlign:'middle',marginRight:3}}><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>Приватный</>
              )}
            </span>
            <span style={{ fontSize: 13, color: 'var(--text-2)' }}>
              {new Date(contest.startTime).toLocaleString('ru-RU')} — {new Date(contest.endTime).toLocaleString('ru-RU')}
            </span>
          </div>
        </div>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
        <Link to={`/contests/${id}/standings`} className="btn btn-outline btn-sm" style={{display:'inline-flex',alignItems:'center',gap:5}}>
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M8 21H5a2 2 0 0 1-2-2v-2a2 2 0 0 1 2-2h3"/><path d="M16 21h3a2 2 0 0 0 2-2v-2a2 2 0 0 0-2-2h-3"/><path d="M12 21V9"/><path d="M9 9h6"/><path d="M7 3a1 1 0 0 0-1 1v1a5 5 0 0 0 6 4.9A5 5 0 0 0 18 5V4a1 1 0 0 0-1-1H7z"/></svg>
          Standings
        </Link>
        {(isOwnContest || isAdmin) && (
          <>
          <Link to={`/contests/${id}/edit`} className="btn btn-outline btn-sm" style={{display:'inline-flex',alignItems:'center',gap:5}}>
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
            Редактировать
          </Link>
          <Link to={`/contests/${id}/analytics`} className="btn btn-outline btn-sm" style={{display:'inline-flex',alignItems:'center',gap:5}}>
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>
            Аналитика
          </Link>
          </>
        )}
        {!contest.isRegistered && contest.status !== 'FINISHED' && (
          <button className="btn btn-primary" onClick={handleRegister} disabled={registering}>
            {registering ? <span className="spinner" /> : 'Зарегистрироваться'}
          </button>
        )}
        {contest.isRegistered && (
          <span className="badge badge-ac" style={{ padding: '6px 12px' }}>✓ Зарегистрирован</span>
        )}
        </div>
      </div>

      {contest.description && (
        <p style={{ color: 'var(--text-2)', fontSize: 14, marginBottom: 20 }}>{contest.description}</p>
      )}

      {!contest.isPublic && isOwnContest && contest.inviteToken && (
        <div className={styles.inviteBox}>
          <div className={styles.inviteLabel}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/>
              <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/>
            </svg>
            Приватная ссылка-приглашение
          </div>
          <div className={styles.inviteLinkRow}>
            <code className={styles.inviteLink}>
              {window.location.origin}/contests/invite/{contest.inviteToken}
            </code>
            <button
              className={styles.copyBtn + (copySuccess ? ' ' + styles.copySuccess : '')}
              onClick={() => {
                navigator.clipboard.writeText(window.location.origin + '/contests/invite/' + contest.inviteToken)
                setCopySuccess(true)
                setTimeout(() => setCopySuccess(false), 2000)
              }}
            >
              {copySuccess ? '✓ Скопировано' : 'Копировать'}
            </button>
          </div>
        </div>
      )}

      <div className={styles.tabs}>
        <button className={tab === 'problems' ? styles.activeTab : styles.tab} onClick={() => setTab('problems')}>
          Задачи ({contest.problems.length})
        </button>
        <button className={tab === 'leaderboard' ? styles.activeTab : styles.tab} onClick={() => setTab('leaderboard')}>
          Таблица результатов
        </button>
      </div>

      {tab === 'problems' ? (
        <div className="card">
          <table className="table">
            <thead>
              <tr><th>#</th><th>Название</th><th>Уровень</th></tr>
            </thead>
            <tbody>
              {contest.problems.map((p, i) => (
                <tr key={p.id}>
                  <td style={{ color: 'var(--text-2)', width: 40 }}>{String.fromCharCode(65 + i)}</td>
                  <td><Link to={`/problems/${p.id}`}>{p.title}</Link></td>
                  <td><span className={`badge badge-${p.difficulty.toLowerCase()}`}>{DIFF_LABELS[p.difficulty]}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
          {contest.problems.length === 0 && <div className="empty-state">Задач пока нет</div>}
        </div>
      ) : (
        <div className="card">
          <table className="table">
            <thead>
              <tr><th>Место</th><th>Участник</th><th>Баллы</th><th>Штраф</th><th>Посылок</th></tr>
            </thead>
            <tbody>
              {leaderboard.map(e => (
                <tr key={e.userId}>
                  <td style={{ fontWeight: 700, color: e.rank <= 3 ? 'var(--violet)' : undefined }}>#{e.rank}</td>
                  <td>{e.username}</td>
                  <td style={{ fontWeight: 600 }}>{e.score}</td>
                  <td style={{ color: 'var(--text-2)' }}>{e.penalty}</td>
                  <td style={{ color: 'var(--text-2)' }}>{e.submissionCount}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {leaderboard.length === 0 && <div className="empty-state">Нет участников</div>}
        </div>
      )}
    </div>
  )
}
