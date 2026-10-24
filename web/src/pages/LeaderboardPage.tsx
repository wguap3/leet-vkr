import { Link } from 'react-router-dom'
import { useEffect, useState } from 'react'
import api from '../api/client'
import type { Page, UserProfile } from '../types'
import RatingBadge from '../components/RatingBadge'
import styles from './LeaderboardPage.module.css'

const MEDALS = ['🥇', '🥈', '🥉']
const RANK_CLS = ['', styles.gold, styles.silver, styles.bronze]

export default function LeaderboardPage() {
  const [users, setUsers] = useState<UserProfile[]>([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)

  useEffect(() => {
    setLoading(true)
    api.get<Page<UserProfile>>(`/users/leaderboard?page=${page}&size=20`)
      .then(({ data }) => { setUsers(data.content); setTotalPages(data.totalPages) })
      .finally(() => setLoading(false))
  }, [page])

  return (
    <div className="page-container">
      <div className="page-header"><h1>Рейтинг</h1></div>
      <div className="card">
        {loading ? (
          <div className="empty-state"><span className="spinner" /></div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th style={{ width: 60 }}>#</th>
                <th>Участник</th>
                <th>Тир</th>
                <th style={{ textAlign: 'right' }}>Рейтинг</th>
                <th>Решено</th>
                <th>Посылок</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u, i) => {
                const rank = page * 20 + i + 1
                return (
                  <tr key={u.id}>
                    <td>
                      <span className={`${styles.rank} ${RANK_CLS[rank] ?? ''}`}>
                        {rank <= 3 ? MEDALS[rank - 1] : `#${rank}`}
                      </span>
                    </td>
                    <td>
                      <div className={styles.userCell}>
                        <div className={styles.avatar}>
                          {u.avatarUrl ? <img src={u.avatarUrl} alt={u.username} /> : u.username[0].toUpperCase()}
                        </div>
                        <Link to={`/profile/${u.id}`} style={{ fontWeight: 500, color: 'var(--text-1)', textDecoration: 'none' }} className="hover-link">{u.username}</Link>
                      </div>
                    </td>
                    <td><RatingBadge rating={u.rating} size="sm" showRating={false} /></td>
                    <td style={{ textAlign: 'right', color: 'var(--violet)', fontWeight: 700 }}>{u.rating}</td>
                    <td style={{ color: 'var(--text-2)' }}>{u.totalAC}</td>
                    <td style={{ color: 'var(--text-3)' }}>{u.totalSubmissions}</td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        )}
      </div>
      {totalPages > 1 && (
        <div style={{ display: 'flex', justifyContent: 'center', gap: 10, marginTop: 20 }}>
          <button className="btn btn-outline btn-sm" disabled={page === 0} onClick={() => setPage(p => p - 1)}>← Назад</button>
          <span style={{ fontSize: 13, color: 'var(--text-3)', alignSelf: 'center' }}>{page + 1} / {totalPages}</span>
          <button className="btn btn-outline btn-sm" disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>Вперёд →</button>
        </div>
      )}
    </div>
  )
}
