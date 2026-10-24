import { useEffect, useState } from 'react'
import * as usersApi from '../api/users'
import type { Role } from '../types'
import styles from './AdminPanel.module.css'

const ROLES: Role[] = ['CONTESTANT', 'ORGANIZER', 'ADMIN']

const ROLE_LABELS: Record<Role, string> = {
  CONTESTANT: 'Участник',
  ORGANIZER: 'Организатор',
  ADMIN: 'Администратор',
}

const ROLE_COLORS: Record<Role, string> = {
  CONTESTANT: 'var(--text-3)',
  ORGANIZER: 'var(--violet)',
  ADMIN: 'var(--yellow)',
}

export default function AdminPanel() {
  const [users, setUsers] = useState<usersApi.UserAdmin[]>([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [pending, setPending] = useState<Record<number, boolean>>({})

  useEffect(() => {
    load()
  }, [page])

  function load() {
    setLoading(true)
    usersApi.getAllUsers({ page, size: 20 })
      .then(({ data }) => {
        setUsers(data.content)
        setTotalPages(data.totalPages)
      })
      .finally(() => setLoading(false))
  }

  async function handleRoleChange(id: number, role: Role) {
    setPending(p => ({ ...p, [id]: true }))
    try {
      await usersApi.changeRole(id, role)
      setUsers(u => u.map(x => x.id === id ? { ...x, role } : x))
    } finally {
      setPending(p => ({ ...p, [id]: false }))
    }
  }

  async function handleToggleActive(id: number, current: boolean) {
    setPending(p => ({ ...p, [id]: true }))
    try {
      await usersApi.setActive(id, !current)
      setUsers(u => u.map(x => x.id === id ? { ...x, active: !current } : x))
    } finally {
      setPending(p => ({ ...p, [id]: false }))
    }
  }

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1>Управление пользователями</h1>
          <p style={{ color: 'var(--text-3)', fontSize: 14, marginTop: 4 }}>
            {users.length > 0 && `${page * 20 + 1}–${page * 20 + users.length}`}
          </p>
        </div>
      </div>

      <div className="card" style={{ overflow: 'hidden' }}>
        {loading ? (
          <div className="empty-state"><span className="spinner" /></div>
        ) : users.length === 0 ? (
          <div className="empty-state">Нет пользователей</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Пользователь</th>
                <th>Email</th>
                <th>Роль</th>
                <th>Рейтинг</th>
                <th>Статус</th>
                <th>Действия</th>
              </tr>
            </thead>
            <tbody>
              {users.map(u => (
                <tr key={u.id} className={!u.active ? styles.inactive : ''}>
                  <td style={{ color: 'var(--text-3)', width: 48 }}>{u.id}</td>
                  <td>
                    <div className={styles.userCell}>
                      <div className={styles.avatar}>{u.username[0].toUpperCase()}</div>
                      <span style={{ fontWeight: 500 }}>{u.username}</span>
                    </div>
                  </td>
                  <td style={{ color: 'var(--text-2)' }}>{u.email}</td>
                  <td>
                    <select
                      className={styles.roleSelect}
                      style={{ color: ROLE_COLORS[u.role] }}
                      value={u.role}
                      disabled={pending[u.id]}
                      onChange={e => handleRoleChange(u.id, e.target.value as Role)}
                    >
                      {ROLES.map(r => (
                        <option key={r} value={r}>{ROLE_LABELS[r]}</option>
                      ))}
                    </select>
                  </td>
                  <td style={{ color: 'var(--violet)', fontWeight: 600 }}>{u.rating}</td>
                  <td>
                    <span className={`badge ${u.active ? styles.badgeActive : styles.badgeBanned}`}>
                      {u.active ? 'Активен' : 'Заблокирован'}
                    </span>
                  </td>
                  <td>
                    <button
                      className={`btn btn-sm ${u.active ? 'btn-outline' : 'btn-secondary'}`}
                      disabled={pending[u.id]}
                      onClick={() => handleToggleActive(u.id, u.active)}
                    >
                      {pending[u.id] ? <span className="spinner" style={{ width: 12, height: 12 }} /> : null}
                      {u.active ? 'Заблокировать' : 'Разблокировать'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {totalPages > 1 && (
        <div style={{ display: 'flex', justifyContent: 'center', gap: 12, marginTop: 20 }}>
          <button className="btn btn-outline btn-sm" disabled={page === 0} onClick={() => setPage(p => p - 1)}>
            ← Назад
          </button>
          <span style={{ color: 'var(--text-2)', fontSize: 14, alignSelf: 'center' }}>
            {page + 1} / {totalPages}
          </span>
          <button className="btn btn-outline btn-sm" disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>
            Вперёд →
          </button>
        </div>
      )}
    </div>
  )
}
