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
  if (rank === 1) return <span className={styles.gold}>🥇</span>
  if (rank === 2) return <span className={styles.silver}>🥈</span>
  if (rank === 3) return <span className={styles.bronze}>🥉</span>
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
            <span className={styles.chip}>👥 {rows.length} участников</span>
            <span className={styles.chip}>📋 {problems.length} задач</span>
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
