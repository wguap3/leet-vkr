import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import * as contestsApi from '../api/contests'
import type { ContestStandings } from '../types'
import styles from './ContestStandingsPage.module.css'

type Cell = ContestStandings['rows'][0]['cells'][number]
type Problem = ContestStandings['problems'][0]

function ProblemCell({ cell, letter }: { cell: Cell | undefined; letter: string }) {
  if (!cell) {
    return <td className={styles.cellEmpty}>—</td>
  }
  if (cell.solved) {
    return (
      <td className={styles.cellAc}>
        <div className={styles.cellInner}>
          <span className={styles.cellCheck}>+{cell.wrongBefore > 0 ? cell.wrongBefore : ''}</span>
          {cell.solvedAt != null && (
            <span className={styles.cellTime}>{cell.solvedAt} мин</span>
          )}
        </div>
      </td>
    )
  }
  return (
    <td className={styles.cellWa}>
      <div className={styles.cellInner}>
        <span className={styles.cellCheck}>
          {cell.wrongBefore > 0 ? `−${cell.wrongBefore}` : '?'}
        </span>
      </div>
    </td>
  )
}

function rankLabel(rank: number) {
  const colors = ['#f5c842', '#c0c0c0', '#cd7f32']
  if (rank <= 3) return (
    <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
      <circle cx="11" cy="11" r="10" fill={colors[rank-1]} opacity="0.2"/>
      <circle cx="11" cy="11" r="10" stroke={colors[rank-1]} strokeWidth="1.5"/>
      <text x="11" y="15" textAnchor="middle" fontSize="10" fontWeight="700" fill={colors[rank-1]}>{rank}</text>
    </svg>
  )
  return <span>#{rank}</span>
}

function fmtPenalty(pen: number) {
  if (pen === 0) return '0'
  const h = Math.floor(pen / 60)
  const m = pen % 60
  return h > 0 ? `${h}ч ${m}м` : `${m}м`
}

export default function ContestStandingsPage() {
  const { id } = useParams<{ id: string }>()
  const [standings, setStandings] = useState<ContestStandings | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    contestsApi.getContestStandings(Number(id))
      .then(({ data }) => setStandings(data))
      .catch(() => setError('Не удалось загрузить турнирную таблицу'))
      .finally(() => setLoading(false))
  }, [id])

  if (loading) return <div className="empty-state" style={{ marginTop: 80 }}><span className="spinner" /></div>
  if (error)   return <div className="empty-state">{error}</div>
  if (!standings) return null

  const { problems, rows, contestTitle } = standings

  return (
    <div className="page-container">
      <div className={styles.header}>
        <div>
          <Link to={`/contests/${id}`} className={styles.back}>← Контест</Link>
          <h1 className={styles.title}>{contestTitle} — Standings</h1>
          <div className={styles.meta}>
            <span className={styles.chip}><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{verticalAlign:'middle',marginRight:4}}><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>{rows.length} участников</span>
            <span className={styles.chip}><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{verticalAlign:'middle',marginRight:4}}><rect x="9" y="2" width="6" height="4" rx="1"/><path d="M17 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/></svg>{problems.length} задач</span>
          </div>
        </div>
      </div>

      {rows.length === 0 ? (
        <div className="card">
          <div className={`empty-state ${styles.emptyState}`}>Посылок пока нет</div>
        </div>
      ) : (
        <div className={`card ${styles.tableWrap}`}>
          <table className={`table ${styles.standingsTable}`}>
            <thead>
              <tr>
                <th className={styles.thRank}>#</th>
                <th className={styles.thUser}>Участник</th>
                <th className={styles.thSolved}>Решено</th>
                <th className={styles.thPenalty}>Штраф</th>
                {problems.map((p: Problem) => (
                  <th key={p.id} className={styles.thProb}>
                    <Link to={`/problems/${p.id}`} title={p.title}
                      style={{ color: 'var(--text-2)', textDecoration: 'none', fontWeight: 600 }}>
                      {p.letter}
                    </Link>
                    <div style={{ fontSize: 10, color: 'var(--text-4)', fontWeight: 400, maxWidth: 80, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      {p.title}
                    </div>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {rows.map(row => (
                <tr key={row.userId}>
                  <td className={styles.tdRank}>{rankLabel(row.rank)}</td>
                  <td>
                    <div className={styles.userCell}>
                      <div className={styles.avatar}>{row.username[0].toUpperCase()}</div>
                      <Link to={`/profile/${row.userId}`} className={styles.usernameLink}>
                        {row.username}
                      </Link>
                    </div>
                  </td>
                  <td className={styles.tdSolved}
                    style={{ color: row.solved > 0 ? 'var(--green)' : 'var(--text-3)' }}>
                    {row.solved}
                  </td>
                  <td className={styles.tdPenalty}>{fmtPenalty(row.penalty)}</td>
                  {problems.map((p: Problem) => (
                    <ProblemCell key={p.id} cell={row.cells[p.id]} letter={p.letter} />
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <div className={styles.legend}>
        <span className={styles.legendItem}><span className={styles.dotGreen} /> Решено (+N = N неверных до AC)</span>
        <span className={styles.legendItem}><span className={styles.dotRed} /> Не решено (−N попыток)</span>
        <span style={{ color: 'var(--text-4)' }}>Штраф = время_AC + 20 мин × неверных</span>
      </div>
    </div>
  )
}
