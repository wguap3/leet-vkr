import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import * as problemsApi from '../api/problems'
import type { ProblemShort, Difficulty } from '../types'
import styles from './ProblemsPage.module.css'

const DIFFICULTIES: { value: Difficulty; label: string }[] = [
  { value: 'EASY', label: 'Лёгкая' },
  { value: 'MEDIUM', label: 'Средняя' },
  { value: 'HARD', label: 'Сложная' },
]

const DIFF_LABELS: Record<Difficulty, string> = {
  EASY: 'Лёгкая', MEDIUM: 'Средняя', HARD: 'Сложная'
}

const VERDICT_LABELS: Record<string, string> = {
  AC: 'Решена', WA: 'WA', TLE: 'TLE', RE: 'RE', CE: 'CE', PENDING: '...'
}

function diffBadge(d: Difficulty) {
  return <span className={`badge badge-${d.toLowerCase()}`}>{DIFF_LABELS[d]}</span>
}

function verdictBadge(v: string | null) {
  if (!v) return null
  return <span className={`badge badge-${v.toLowerCase()}`}>{VERDICT_LABELS[v] ?? v}</span>
}

export default function ProblemsPage() {
  const [problems, setProblems] = useState<ProblemShort[]>([])
  const [loading, setLoading] = useState(true)
  const [difficulty, setDifficulty] = useState<Difficulty | ''>('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [search, setSearch] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    setLoading(true)
    problemsApi.getProblems({ difficulty: difficulty || undefined, search: search || undefined, page, size: 20 })
      .then(({ data }) => {
        setProblems(data.content)
        setTotalPages(data.totalPages)
      })
      .finally(() => setLoading(false))
  }, [difficulty, search, page])

  return (
    <div className="page-container">
      <div className="page-header" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
        <h1>Задачи</h1>
        <div className={styles.filters}>
          <input
            type="text"
            placeholder="Поиск по названию..."
            value={searchInput}
            onChange={e => {
              const v = e.target.value
              setSearchInput(v)
              if (debounceRef.current) clearTimeout(debounceRef.current)
              debounceRef.current = setTimeout(() => { setSearch(v); setPage(0) }, 350)
            }}
            style={{ minWidth: 220 }}
          />
          <select value={difficulty} onChange={e => { setDifficulty(e.target.value as any); setPage(0) }}>
            <option value="">Все уровни</option>
            {DIFFICULTIES.map(d => <option key={d.value} value={d.value}>{d.label}</option>)}
          </select>
        </div>
      </div>

      <div className="card">
        {loading ? (
          <div className="empty-state"><span className="spinner" /></div>
        ) : problems.length === 0 ? (
          <div className="empty-state">Задачи не найдены</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>#</th>
                <th>Название</th>
                <th>Уровень</th>
                <th>Теги</th>
                <th>Решили</th>
                <th>Статус</th>
              </tr>
            </thead>
            <tbody>
              {problems.map((p, i) => (
                <tr key={p.id}>
                  <td style={{ color: 'var(--text-2)', width: 48 }}>{page * 20 + i + 1}</td>
                  <td>
                    <Link to={`/problems/${p.id}`} style={{ fontWeight: 500 }}>{p.title}</Link>
                  </td>
                  <td>{diffBadge(p.difficulty)}</td>
                  <td>
                    <div className={styles.tags}>
                      {p.tags.slice(0, 3).map(t => (
                        <span key={t.id} className={styles.tag}>{t.name}</span>
                      ))}
                    </div>
                  </td>
                  <td style={{ color: 'var(--text-2)' }}>{p.solvedCount}</td>
                  <td>{verdictBadge(p.userVerdict)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {totalPages > 1 && (
        <div className={styles.pagination}>
          <button className="btn btn-outline btn-sm" disabled={page === 0} onClick={() => setPage(p => p - 1)}>
            ← Назад
          </button>
          <span style={{ color: 'var(--text-2)', fontSize: 14 }}>
            Страница {page + 1} / {totalPages}
          </span>
          <button className="btn btn-outline btn-sm" disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>
            Вперёд →
          </button>
        </div>
      )}
    </div>
  )
}
