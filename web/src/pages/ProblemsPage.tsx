import { notify } from '../components/Toast'
import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import * as problemsApi from '../api/problems'
import { useAuthStore } from '../store/authStore'
import type { ProblemShort, Tag } from '../types'
import styles from './ProblemsPage.module.css'

const levels: Record<string, string> = { EASY: 'Лёгкая', MEDIUM: 'Средняя', HARD: 'Сложная' }
export default function ProblemsPage() {
  const { user } = useAuthStore()
  const canCreate = user?.role === 'ORGANIZER' || user?.role === 'ADMIN'
  const [params, setParams] = useSearchParams()
  const mine = canCreate && params.get('mine') === 'true'
  const [problems, setProblems] = useState<ProblemShort[]>([])
  const [tags, setTags] = useState<Tag[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [difficulty, setDifficulty] = useState('')
  const [tagId, setTagId] = useState(params.get('tagId') || '')
  const [solved, setSolved] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [total, setTotal] = useState(0)
  const [input, setInput] = useState('')
  const [search, setSearch] = useState('')
  const deletePanel = useRef<HTMLElement>(null)
  const [deleting, setDeleting] = useState<ProblemShort | null>(null)
  useEffect(() => {
    if (deleting) { deletePanel.current?.scrollIntoView({ block: 'center' }); deletePanel.current?.focus({ preventScroll: true }) }
  }, [deleting])
  const [busy, setBusy] = useState(false)
  const [deleteError, setDeleteError] = useState('')
  const [blocked, setBlocked] = useState(false)
  const [canHide, setCanHide] = useState(false)
  const [retry, setRetry] = useState(0)
  useEffect(() => { setTagId(params.get('tagId') || ''); setPage(0) }, [params])
  useEffect(() => { problemsApi.getTags().then(r => setTags(r.data)).catch(() => {}) }, [])
  useEffect(() => { const timer = setTimeout(() => { setSearch(input); setPage(0) }, 300); return () => clearTimeout(timer) }, [input])
  useEffect(() => {
    let active = true
    setLoading(true); setError('')
    problemsApi.getProblems({ difficulty: difficulty || undefined, tagId: tagId ? Number(tagId) : undefined,
      solved: solved === '' ? undefined : solved === 'true', mine, search: search || undefined, page, size: 20, sort: 'id,desc' })
      .then(({ data }) => { if (active) { setProblems(data.content); setTotalPages(data.totalPages); setTotal(data.totalElements) } })
      .catch(() => { if (active) setError('Не удалось загрузить задачи. Проверьте соединение и попробуйте снова.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [difficulty, tagId, solved, mine, search, page, retry])
  const remove = async (hide = false) => {
    if (!deleting) return
    setBusy(true); setDeleteError(''); setBlocked(false)
    try {
      if (hide) await problemsApi.setVisible(deleting.id, false)
      else await problemsApi.deleteProblem(deleting.id)
      setDeleting(null); setCanHide(false)
      notify(hide ? 'Задача снята с публикации' : 'Задача удалена')
      if (!hide && problems.length === 1 && page > 0) setPage(n => n - 1)
      else setRetry(n => n + 1)
    } catch (err: any) {
      setBlocked(err.response?.status === 409)
      setCanHide(err.response?.status === 409 && !!deleting.visible)
      setDeleteError(err.response?.data?.message || 'Не удалось удалить задачу. Попробуйте снова.')
    } finally { setBusy(false) }
  }
  const reset = () => { setInput(''); setDifficulty(''); setTagId(''); setSolved(''); setPage(0) }
  const filtered = input || difficulty || tagId || solved
  return <div className="page-container">
    <div className="page-header">
      <div><p className="eyebrow">ПРАКТИКА · JAVA 21</p><h1>{mine ? 'Мои задачи' : 'Каждая задача — шаг вперёд'}</h1>
      <p className="page-description">{mine ? 'От первого черновика до готового задания для соревнования.' : 'Выберите тему, напишите решение и получите результат проверки.'}</p></div>
      {canCreate && <Link className="btn btn-primary btn-lg" to="/problems/new">+ Создать задачу</Link>}
    </div>
    <div className="section-bar"><div className="tabs" style={{ marginBottom: 0 }}>
      <button className={`tab ${!mine ? 'tab-active' : ''}`} onClick={() => { setParams({}); setPage(0) }}>Каталог</button>
      {canCreate && <button className={`tab ${mine ? 'tab-active' : ''}`} onClick={() => { setParams({ mine: 'true' }); setPage(0) }}>Мои задачи</button>}
    </div><span className="muted">{total} задач</span></div>
    <div className="filter-panel">
      <label className="search-field">Поиск<input value={input} onChange={e => setInput(e.target.value)} placeholder="Название задачи…" type="search" /></label>
      <label>Сложность<select value={difficulty} onChange={e => { setDifficulty(e.target.value); setPage(0) }}><option value="">Все уровни</option>{Object.entries(levels).map(([v, t]) => <option key={v} value={v}>{t}</option>)}</select></label>
      <label>Тема<select value={tagId} onChange={e => { setTagId(e.target.value); setPage(0) }}><option value="">Все темы</option>{tags.map(t => <option value={t.id} key={t.id}>{t.name}</option>)}</select></label>
      <label>Прогресс<select value={solved} onChange={e => { setSolved(e.target.value); setPage(0) }}><option value="">Все задачи</option><option value="true">Решённые</option><option value="false">Не решённые</option></select></label>
      {filtered && <button className="btn btn-ghost" onClick={reset}>Сбросить</button>}
    </div>
    {mine && deleting && <section ref={deletePanel} tabIndex={-1} className="card" aria-label="Подтверждение удаления задачи" style={{ padding:20, marginBottom:20 }}>
      <h2 style={{ fontSize:18 }}>Удалить «{deleting.title}»?</h2>
      <p className="page-description">Условие, тесты и подсказки будут удалены без возможности восстановления. Задачи с посылками защищены от удаления. Из ваших завершённых контестов без посылок задача будет убрана автоматически.</p>
      {deleteError && <p role="alert" style={{ color:'var(--red)', marginTop:12 }}>{deleteError}</p>}
      <div style={{ display:'flex', flexWrap:'wrap', gap:8, marginTop:16 }}>
        <button className="btn btn-outline" disabled={busy} onClick={() => { setDeleting(null); setDeleteError(''); setCanHide(false); setBlocked(false) }}>Отмена</button>
        {canHide ? <button className="btn btn-primary" disabled={busy} onClick={() => remove(true)}>Снять с публикации</button> : !blocked &&
          <button className="btn btn-primary" disabled={busy} onClick={() => remove()}>{busy ? 'Удаляем…' : 'Подтвердить удаление'}</button>}
      </div>
    </section>}
    <div className="card table-scroll" aria-busy={loading}>
      {error ? <div className="empty-state" role="alert"><h2>Нет соединения</h2><p>{error}</p><button className="btn btn-secondary" onClick={() => setRetry(n => n + 1)}>Повторить</button></div>
      : loading ? <div className="empty-state" role="status"><span className="spinner" />Загружаем задачи…</div>
      : problems.length === 0 ? <div className="empty-state"><span className="empty-icon">⌘</span><h2>{filtered ? 'Ничего не нашлось' : mine ? 'Начните с первой задачи' : 'Здесь появятся задачи'}</h2><p>{filtered ? 'Попробуйте другую тему или сбросьте фильтры.' : mine ? 'Добавьте условие, примеры и скрытые тесты.' : 'Организатор ещё не опубликовал задания.'}</p>{filtered ? <button className="btn btn-secondary" onClick={reset}>Сбросить фильтры</button> : canCreate && <Link to="/problems/new" className="btn btn-primary">Создать задачу</Link>}</div>
      : <table className="table"><thead><tr><th>Задача</th><th>Сложность</th><th>Темы</th><th>Решили</th><th>{mine ? 'Публикация' : 'Ваш прогресс'}</th>{mine && <th>Действия</th>}</tr></thead><tbody>{problems.map(p => <tr key={p.id}>
        <td><span className="problem-number">{String(p.id).padStart(3, '0')}</span><Link to={`/problems/${p.id}`} className="problem-title">{p.title}</Link></td>
        <td><span className={`badge badge-${p.difficulty.toLowerCase()}`}>{levels[p.difficulty]}</span></td>
        <td><div className={styles.tags}>{p.tags.slice(0, 3).map(t => <span className={styles.tag} key={t.id}>{t.name}</span>)}</div></td>
        <td className="muted">{p.solvedCount}</td><td>{mine ? <span className={`badge ${p.visible ? 'badge-ac' : 'badge-pending'}`}>{p.visible ? 'Опубликована' : 'Черновик'}</span> : p.userVerdict === 'AC' ? <span className="badge badge-ac">✓ Решена</span> : <span className="muted">Не решена</span>}</td>
        {mine && <td><div style={{ display:'flex', gap:8 }}><Link className="btn btn-outline btn-sm" to={`/problems/${p.id}/edit`}>Изменить</Link><button className="btn btn-outline btn-sm" aria-label={`Удалить ${p.title}`} disabled={busy} onClick={() => { setDeleting(p); setDeleteError(''); setCanHide(false); setBlocked(false) }}>Удалить</button></div></td>}
      </tr>)}</tbody></table>}
    </div>
    {totalPages > 1 && <div className={styles.pagination}><button className="btn btn-outline" disabled={page === 0} onClick={() => setPage(n => n - 1)}>← Назад</button><span className="muted">{page + 1} / {totalPages}</span><button className="btn btn-outline" disabled={page >= totalPages - 1} onClick={() => setPage(n => n + 1)}>Далее →</button></div>}
  </div>
}
