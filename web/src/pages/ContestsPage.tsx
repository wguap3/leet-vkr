import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import * as contestsApi from '../api/contests'
import type { ContestShort, ContestStatus } from '../types'
import styles from './ContestsPage.module.css'

const STATUSES: { value: ContestStatus; label: string }[] = [
  { value: 'UPCOMING', label: 'Предстоящие' },
  { value: 'RUNNING', label: 'Идут сейчас' },
  { value: 'FINISHED', label: 'Завершённые' },
]

const STATUS_LABELS: Record<ContestStatus, string> = {
  UPCOMING: 'Скоро', RUNNING: 'Идёт', FINISHED: 'Завершён'
}

function statusBadge(s: ContestStatus) {
  const map: Record<ContestStatus, string> = {
    UPCOMING: `badge-upcoming`, RUNNING: `badge-running`, FINISHED: `badge-finished`
  }
  return <span className={`badge ${map[s]}`}>{STATUS_LABELS[s]}</span>
}

function formatTime(iso: string) {
  return new Date(iso).toLocaleString('ru-RU')
}

export default function ContestsPage() {
  const [contests, setContests] = useState<ContestShort[]>([])
  const [status, setStatus] = useState<ContestStatus | ''>('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    contestsApi.getContests(status || undefined)
      .then(({ data }) => setContests(data))
      .finally(() => setLoading(false))
  }, [status])

  return (
    <div className="page-container">
      <div className="page-header" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <h1>Контесты</h1>
        <select value={status} onChange={e => setStatus(e.target.value as any)}>
          <option value="">Все</option>
          {STATUSES.map(s => <option key={s.value} value={s.value}>{s.label}</option>)}
        </select>
      </div>

      {loading ? (
        <div className="empty-state"><span className="spinner" /></div>
      ) : contests.length === 0 ? (
        <div className="empty-state">Контестов нет</div>
      ) : (
        <div className={styles.grid}>
          {contests.map(c => (
            <Link key={c.id} to={`/contests/${c.id}`} className={`card ${styles.contestCard}`}>
              <div className={styles.cardTop}>
                <h3 className={styles.cardTitle}>{c.title}</h3>
                {statusBadge(c.status)}
              </div>
              <div className={styles.cardMeta}>
                <span style={{display:'flex',alignItems:'center',gap:4}}>
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                  {formatTime(c.startTime)}
                </span>
                <span style={{display:'flex',alignItems:'center',gap:4}}>
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
                  {c.problemCount} задач
                </span>
                <span style={{display:'flex',alignItems:'center',gap:4}}>
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
                  {c.participantCount} участников
                </span>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
