import { useEffect, useState } from 'react'
import api from '../api/client'
import type { Tag } from '../types'
export default function TagManager() {
  const [tags, setTags] = useState<Tag[]>([])
  const [name, setName] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const load = () => api.get<Tag[]>('/tags').then(r => setTags(r.data)).catch(() => setError('Не удалось загрузить темы'))
  useEffect(() => { load() }, [])
  const create = async (event: React.FormEvent) => {
    event.preventDefault(); setBusy(true); setError('')
    try { await api.post('/tags', { name: name.trim() }); setName(''); await load() }
    catch (e: any) { setError(e.response?.data?.message || 'Не удалось создать тему') }
    finally { setBusy(false) }
  }
  const remove = async (tag: Tag) => {
    setBusy(true); setError('')
    try { await api.delete(`/tags/${tag.id}`); await load() }
    catch (e: any) { setError(e.response?.data?.message || 'Не удалось удалить тему') }
    finally { setBusy(false) }
  }
  return <section className="card" style={{ padding: 24, marginTop: 28 }}><h2 style={{ fontSize: 20 }}>Темы задач</h2><p className="page-description">Темы доступны авторам при создании задач и участникам в фильтрах каталога.</p>
    <form onSubmit={create} className="filter-panel" style={{ marginTop: 20 }}><label>Новая тема<input value={name} onChange={e => setName(e.target.value)} maxLength={50} required placeholder="Например, динамическое программирование" /></label><button className="btn btn-primary" disabled={busy || !name.trim()}>Добавить тему</button></form>
    {error && <p className="alert alert-error" role="alert">{error}</p>}
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10 }}>{tags.map(t => <span className="badge badge-pending" key={t.id}>{t.name}<button onClick={() => remove(t)} disabled={busy} aria-label={`Удалить тему ${t.name}`} style={{ marginLeft: 10 }}>×</button></span>)}</div>
  </section>
}
