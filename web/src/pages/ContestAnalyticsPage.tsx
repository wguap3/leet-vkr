import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import * as contestsApi from '../api/contests'
import type { ContestAnalytics } from '../types'
import styles from './ContestAnalyticsPage.module.css'

function Cell({ result }: { result?: ContestAnalytics['participants'][0]['results'][number] }) {
  if (!result) return <td className={styles.cellEmpty}>—</td>

  if (result.solved) {
    return (
      <td className={styles.cellAc}>
        <div className={styles.cellInner}>
          <span className={styles.cellIcon}>✓</span>
          <span className={styles.cellSub}>
            {result.attempts} поп.
            {result.firstAcMinutes != null && ` · ${result.firstAcMinutes} мин`}
          </span>
        </div>
      </td>
    )
  }
  return (
    <td className={styles.cellWa}>
      <div className={styles.cellInner}>
        <span className={styles.cellIcon}>✗</span>
        <span className={styles.cellSub}>{result.attempts} поп.</span>
      </div>
    </td>
  )
}

export default function ContestAnalyticsPage() {
  const { id } = useParams<{ id: string }>()
  const [analytics, setAnalytics] = useState<ContestAnalytics | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    contestsApi.getContestAnalytics(Number(id))
      .then(({ data }) => setAnalytics(data))
      .catch(() => setError('Не удалось загрузить аналитику'))
      .finally(() => setLoading(false))
  }, [id])

  if (loading) return <div className="empty-state" style={{ marginTop: 80 }}><span className="spinner" /></div>
  if (error)   return <div className="empty-state">{error}</div>
  if (!analytics) return null

  const totalSolved = (p: ContestAnalytics['participants'][0]) =>
    Object.values(p.results).filter(r => r.solved).length

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <Link to={`/contests/${id}`} className={styles.back}>← Контест</Link>
          <h1>{analytics.contestTitle} — Аналитика</h1>
        </div>
        <div className={styles.summary}>
          <span className={styles.summaryChip}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{verticalAlign:'middle',marginRight:4}}><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>{analytics.participants.length} участников
          </span>
          <span className={styles.summaryChip}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{verticalAlign:'middle',marginRight:4}}><rect x="9" y="2" width="6" height="4" rx="1"/><path d="M17 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/></svg>{analytics.problems.length} задач
          </span>
        </div>
      </div>

      {analytics.participants.length === 0 ? (
        <div className="card">
          <div className="empty-state">Посылок пока не было</div>
        </div>
      ) : (
        <div className={styles.tableWrap}>
          <table className={`table ${styles.analyticsTable}`}>
            <thead>
              <tr>
                <th className={styles.thRank}>#</th>
                <th className={styles.thUser}>Участник</th>
                <th className={styles.thSolved}>Решено</th>
                {analytics.problems.map(p => (
                  <th key={p.id} className={styles.thProblem}>
                    <Link to={`/problems/${p.id}`} className={styles.problemLink}>{p.title}</Link>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {analytics.participants.map((participant, i) => {
                const solved = totalSolved(participant)
                return (
                  <tr key={participant.userId}>
                    <td className={styles.tdRank}>
                      {i < 3 ? (
                      <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
                        <circle cx="11" cy="11" r="10" fill={['#f5c842','#c0c0c0','#cd7f32'][i]} opacity="0.2"/>
                        <circle cx="11" cy="11" r="10" stroke={['#f5c842','#c0c0c0','#cd7f32'][i]} strokeWidth="1.5"/>
                        <text x="11" y="15" textAnchor="middle" fontSize="10" fontWeight="700" fill={['#f5c842','#c0c0c0','#cd7f32'][i]}>{i+1}</text>
                      </svg>
                    ) : `#${i + 1}`}
                    </td>
                    <td>
                      <div className={styles.userCell}>
                        <div className={styles.avatar}>{participant.username[0].toUpperCase()}</div>
                        <span>{participant.username}</span>
                      </div>
                    </td>
                    <td className={styles.tdSolved}>
                      <span className={styles.solvedBadge} style={{
                        color: solved > 0 ? 'var(--green)' : 'var(--text-3)',
                        background: solved > 0 ? 'var(--green-dim)' : 'var(--bg-4)'
                      }}>
                        {solved}/{analytics.problems.length}
                      </span>
                    </td>
                    {analytics.problems.map(p => (
                      <Cell key={p.id} result={participant.results[p.id]} />
                    ))}
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Legend */}
      <div className={styles.legend}>
        <span className={styles.legendItem}><span className={styles.cellAcDot} />Решено</span>
        <span className={styles.legendItem}><span className={styles.cellWaDot} />Не решено</span>
        <span className={styles.legendItem} style={{ color: 'var(--text-4)' }}>Числа: попытки · минуты до AC</span>
      </div>
    </div>
  )
}
