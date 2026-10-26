import { useEffect, useState } from 'react'
import { useNavigate, useParams, Link } from 'react-router-dom'
import * as contestsApi from '../api/contests'
import * as problemsApi from '../api/problems'
import type { ProblemShort } from '../types'
import styles from './CreatePage.module.css'

const DIFF_LABELS: Record<string, string> = {
  EASY: 'Лёгкая', MEDIUM: 'Средняя', HARD: 'Сложная'
}

/** ISO string → value for <input type="datetime-local"> */
function toLocalInput(iso: string) {
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** datetime-local value → ISO-like string for backend (no timezone suffix) */
function toBackendDt(local: string) {
  return new Date(local).toISOString().slice(0, 19)
}

export default function EditContestPage() {
  const { id } = useParams<{ id: string }>()
  const cid = Number(id)
  const navigate = useNavigate()

  const [loading, setLoading]   = useState(true)
  const [saving, setSaving]     = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [error, setError]       = useState('')

  const [allProblems, setAllProblems]         = useState<ProblemShort[]>([])
  const [loadingProblems, setLoadingProblems] = useState(true)

  // Form state
  const [title, setTitle]             = useState('')
  const [description, setDescription] = useState('')
  const [startTime, setStartTime]     = useState('')
  const [endTime, setEndTime]         = useState('')
  const [selectedProblems, setSelectedProblems] = useState<number[]>([])
  const [isPublic, setIsPublic]       = useState(true)
  const [search, setSearch]           = useState('')

  // ----- Load -----
  useEffect(() => {
    Promise.all([
      contestsApi.getContest(cid),
      problemsApi.getProblems({ size: 200 }),
    ]).then(([{ data: contest }, { data: page }]) => {
      setTitle(contest.title)
      setDescription(contest.description ?? '')
      setStartTime(toLocalInput(contest.startTime))
      setEndTime(toLocalInput(contest.endTime))
      setSelectedProblems(contest.problems.map(p => p.id))
      setIsPublic(contest.isPublic)
      setAllProblems(page.content)
    }).catch(() => setError('Не удалось загрузить контест'))
      .finally(() => { setLoading(false); setLoadingProblems(false) })
  }, [cid])

  const toggleProblem = (pid: number) =>
    setSelectedProblems(prev =>
      prev.includes(pid) ? prev.filter(p => p !== pid) : [...prev, pid]
    )

  // ----- Submit -----
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!title.trim()) { setError('Введите название'); return }
    if (selectedProblems.length === 0) { setError('Выберите хотя бы одну задачу'); return }
    if (new Date(startTime) >= new Date(endTime)) {
      setError('Время окончания должно быть позже начала')
      return
    }

    setSaving(true)
    setError('')
    try {
      await contestsApi.updateContest(cid, {
        title: title.trim(),
        description: description.trim(),
        startTime: toBackendDt(startTime),
        endTime: toBackendDt(endTime),
        problemIds: selectedProblems,
        isPublic,
      })
      navigate(`/contests/${cid}`)
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Ошибка при сохранении')
    } finally {
      setSaving(false)
    }
  }

  // ----- Delete -----
  const handleDelete = async () => {
    if (!window.confirm('Удалить контест? Это действие необратимо.')) return
    setDeleting(true)
    try {
      await contestsApi.deleteContest(cid)
      navigate('/contests')
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Ошибка при удалении')
      setDeleting(false)
    }
  }

  // ----- Render -----
  if (loading) return <div className="empty-state" style={{ marginTop: 80 }}><span className="spinner" /></div>

  // Search filter for problem list
  const visibleProblems = search.trim()
    ? allProblems.filter(p => p.title.toLowerCase().includes(search.toLowerCase()))
    : allProblems

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <Link
            to={`/contests/${cid}`}
            style={{ fontSize: 12, color: 'var(--text-4)', textDecoration: 'none', display: 'block', marginBottom: 6 }}
          >
            ← К контесту
          </Link>
          <h1>Редактировать контест</h1>
        </div>
        <button
          type="button"
          className="btn btn-outline btn-sm"
          style={{ color: 'var(--red)', borderColor: 'var(--red)' }}
          onClick={handleDelete}
          disabled={deleting}
        >
          {deleting ? 'Удаление...' : '🗑 Удалить контест'}
        </button>
      </div>

      <form onSubmit={handleSubmit} className={styles.form}>
        {error && <div className={styles.error}>{error}</div>}

        {/* Основное */}
        <div className="card">
          <div className={styles.section}>
            <h3 className={styles.sectionTitle}>Основное</h3>
            <div className={styles.field}>
              <label>Название *</label>
              <input
                value={title}
                onChange={e => setTitle(e.target.value)}
                placeholder="Название контеста"
              />
            </div>
            <div className={styles.field}>
              <label>Описание <span className={styles.hint}>(Markdown)</span></label>
              <textarea
                className={styles.textarea}
                rows={4}
                value={description}
                onChange={e => setDescription(e.target.value)}
                placeholder="Описание, правила, призы..."
              />
            </div>
            <div className={styles.row}>
              <div className={styles.field}>
                <label>Начало *</label>
                <input
                  type="datetime-local"
                  value={startTime}
                  onChange={e => setStartTime(e.target.value)}
                />
              </div>
              <div className={styles.field}>
                <label>Конец *</label>
                <input
                  type="datetime-local"
                  value={endTime}
                  onChange={e => setEndTime(e.target.value)}
                />
              </div>
            </div>
            <label className={styles.checkLabel} style={{ fontSize: 14 }}>
              <input
                type="checkbox"
                checked={isPublic}
                onChange={e => setIsPublic(e.target.checked)}
              />
              Публичный контест (виден всем участникам)
            </label>
          </div>
        </div>

        {/* Задачи */}
        <div className="card">
          <div className={styles.section}>
            <h3 className={styles.sectionTitle}>
              Задачи * <span className={styles.hint}>({selectedProblems.length} выбрано)</span>
            </h3>

            {/* Quick search */}
            <input
              style={{ marginBottom: 10, maxWidth: 320 }}
              placeholder="Поиск задачи..."
              value={search}
              onChange={e => setSearch(e.target.value)}
            />

            {loadingProblems ? (
              <div className="empty-state"><span className="spinner" /></div>
            ) : allProblems.length === 0 ? (
              <div className="empty-state" style={{ padding: '24px 0' }}>
                Нет опубликованных задач.
              </div>
            ) : (
              <div className={styles.problemList}>
                {visibleProblems.map(p => (
                  <label
                    key={p.id}
                    className={`${styles.problemRow} ${selectedProblems.includes(p.id) ? styles.problemRowSelected : ''}`}
                  >
                    <input
                      type="checkbox"
                      checked={selectedProblems.includes(p.id)}
                      onChange={() => toggleProblem(p.id)}
                    />
                    <span className={styles.problemTitle}>{p.title}</span>
                    <span className={`badge badge-${p.difficulty.toLowerCase()}`}>
                      {DIFF_LABELS[p.difficulty]}
                    </span>
                  </label>
                ))}
                {visibleProblems.length === 0 && (
                  <div style={{ fontSize: 13, color: 'var(--text-4)', padding: '12px 0' }}>
                    Ничего не найдено
                  </div>
                )}
              </div>
            )}
          </div>
        </div>

        <div className={styles.actions}>
          <button type="button" className="btn btn-outline" onClick={() => navigate(`/contests/${cid}`)}>
            Отмена
          </button>
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? 'Сохранение...' : 'Сохранить изменения'}
          </button>
        </div>
      </form>
    </div>
  )
}
