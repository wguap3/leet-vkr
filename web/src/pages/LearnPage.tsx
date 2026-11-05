import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getMyProgress } from '../api/users'
import type { TagProgress } from '../types'
import RequestError from '../components/RequestError'
export default function LearnPage() {
  const [tracks, setTracks] = useState<TagProgress[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  useEffect(() => { getMyProgress().then(r => setTracks(r.data)).catch(() => setError(true)).finally(() => setLoading(false)) }, [])
  if (error) return <RequestError message="Не удалось загрузить учебные треки" />
  return <div className="page-container"><div className="page-header"><div><p className="eyebrow">УЧИТЕСЬ В СВОЁМ ТЕМПЕ</p><h1>Практика по темам</h1><p className="page-description">Сосредоточьтесь на одной теме, начните с лёгких задач и возвращайтесь к нерешённым. Прогресс сохраняется после каждого принятого решения.</p></div><Link to="/problems" className="btn btn-outline">Весь каталог →</Link></div>
    {loading ? <div className="empty-state" role="status"><span className="spinner" />Загружаем прогресс…</div> : tracks.length === 0 ? <div className="card empty-state"><h2>Треки готовятся</h2><p>Они появятся, когда организатор добавит темы к опубликованным задачам.</p><Link to="/problems" className="btn btn-primary">Открыть задачи</Link></div> :
    <div className="track-grid">{tracks.map((t, i) => { const progress = t.total ? Math.min(100, Math.round(t.solved / t.total * 100)) : 0; return <Link to={`/problems?tagId=${t.tagId}`} className="card track-card" key={t.tagId}>
      <div className="section-bar"><span className="eyebrow">ТЕМА {String(i + 1).padStart(2, '0')}</span><span className={`badge ${progress === 100 ? 'badge-ac' : 'badge-pending'}`}>{progress === 100 ? 'Завершено' : progress ? 'В процессе' : 'Начать'}</span></div>
      <h2>{t.tagName}</h2><p className="page-description">Решено {t.solved} из {t.total} задач</p>
      <progress value={t.solved} max={Math.max(1, t.total)} aria-label={`Прогресс по теме ${t.tagName}`} />
      <span className="muted">{progress}% пройдено <span style={{ float: 'right', color: 'var(--accent)' }}>К задачам →</span></span>
    </Link> })}</div>}
  </div>
}
