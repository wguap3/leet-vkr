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
    UPCOMING: 'badge-pending', RUNNING: 'badge-ac', FINISHED: 'badge-wa'
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
                <span>🕐 {formatTime(c.startTime)}</span>
                <span>📋 {c.problemCount} задач</span>
                <span>👥 {c.participantCount} участников</span>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
