import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import * as contestsApi from '../api/contests'
import * as problemsApi from '../api/problems'
import type { ProblemShort } from '../types'
import styles from './CreatePage.module.css'

function toLocalDatetimeValue(date: Date) {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export default function CreateContestPage() {
  const navigate = useNavigate()
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [problems, setProblems] = useState<ProblemShort[]>([])
  const [loadingProblems, setLoadingProblems] = useState(true)

  const now = new Date()
  const inOneHour = new Date(now.getTime() + 60 * 60 * 1000)
  const inFourHours = new Date(now.getTime() + 4 * 60 * 60 * 1000)

  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [startTime, setStartTime] = useState(toLocalDatetimeValue(inOneHour))
  const [endTime, setEndTime] = useState(toLocalDatetimeValue(inFourHours))
  const [selectedProblems, setSelectedProblems] = useState<number[]>([])
  const [isPublic, setIsPublic] = useState(true)

  useEffect(() => {
    // load all published problems (up to 100)
    problemsApi.getProblems({ size: 100 })
      .then(r => setProblems(r.data.content))
      .finally(() => setLoadingProblems(false))
  }, [])

  const toggleProblem = (id: number) =>
    setSelectedProblems(prev => prev.includes(id) ? prev.filter(p => p !== id) : [...prev, id])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!title.trim()) { setError('Введите название контеста'); return }
    if (selectedProblems.length === 0) { setError('Выберите хотя бы одну задачу'); return }
    if (new Date(startTime) >= new Date(endTime)) { setError('Время окончания должно быть позже начала'); return }

    setSaving(true)
    setError('')
    try {
      const { data: contest } = await contestsApi.createContest({
        title: title.trim(),
        description: description.trim(),
        startTime: new Date(startTime).toISOString().slice(0, 19),
        endTime: new Date(endTime).toISOString().slice(0, 19),
        problemIds: selectedProblems,
        isPublic,
      })
      navigate(`/contests/${contest.id}`)
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Ошибка при создании контеста')
    } finally {
      setSaving(false)
    }
  }

  const DIFF_LABELS: Record<string, string> = { EASY: 'Лёгкая', MEDIUM: 'Средняя', HARD: 'Сложная' }

  return (
    <div className="page-container">
      <div className="page-header">
        <h1>Новый контест</h1>
      </div>

      <form onSubmit={handleSubmit} className={styles.form}>
        {error && <div className={styles.error}>{error}</div>}

        {/* Основное */}
        <div className="card">
          <div className={styles.section}>
            <h3 className={styles.sectionTitle}>Основное</h3>
            <div className={styles.field}>
              <label>Название *</label>
              <input value={title} onChange={e => setTitle(e.target.value)} placeholder="Например: Открытый чемпионат Stackr #2" />
            </div>
            <div className={styles.field}>
              <label>Описание <span className={styles.hint}>(поддерживается Markdown)</span></label>
              <textarea
                className={styles.textarea}
                rows={4}
                value={description}
                onChange={e => setDescription(e.target.value)}
                placeholder="Описание контеста, правила, призы..."
              />
            </div>
            <div className={styles.row}>
              <div className={styles.field}>
                <label>Начало *</label>
                <input type="datetime-local" value={startTime} onChange={e => setStartTime(e.target.value)} />
              </div>
              <div className={styles.field}>
                <label>Конец *</label>
                <input type="datetime-local" value={endTime} onChange={e => setEndTime(e.target.value)} />
              </div>
            </div>
            <label className={styles.checkLabel} style={{ fontSize: 14 }}>
              <input type="checkbox" checked={isPublic} onChange={e => setIsPublic(e.target.checked)} />
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
            {loadingProblems ? (
              <div className="empty-state"><span className="spinner" /></div>
            ) : problems.length === 0 ? (
              <div className="empty-state" style={{ padding: '24px 0' }}>
                Нет опубликованных задач. <a href="/problems/new">Создайте задачи</a> сначала.
              </div>
            ) : (
              <div className={styles.problemList}>
                {problems.map(p => (
                  <label key={p.id} className={`${styles.problemRow} ${selectedProblems.includes(p.id) ? styles.problemRowSelected : ''}`}>
                    <input
                      type="checkbox"
                      checked={selectedProblems.includes(p.id)}
                      onChange={() => toggleProblem(p.id)}
                    />
                    <span className={styles.problemTitle}>{p.title}</span>
                    <span className={`badge badge-${p.difficulty.toLowerCase()}`}>{DIFF_LABELS[p.difficulty]}</span>
                  </label>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className={styles.actions}>
          <button type="button" className="btn btn-outline" onClick={() => navigate('/contests')}>Отмена</button>
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? 'Создание...' : 'Создать контест'}
          </button>
        </div>
      </form>
    </div>
  )
}
