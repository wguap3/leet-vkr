import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import * as problemsApi from '../api/problems'
import type { TestCase } from '../types'

interface EditState {
  inputData: string
  expectedOutput: string
  isPublic: boolean
}

export default function TestCasesPage() {
  const { id } = useParams<{ id: string }>()
  const problemId = Number(id)

  const [testCases, setTestCases] = useState<TestCase[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [editing, setEditing] = useState<Record<number, EditState>>({})
  const [saving, setSaving] = useState<Record<number, boolean>>({})

  const [showAdd, setShowAdd] = useState(false)
  const [newInput, setNewInput] = useState('')
  const [newOutput, setNewOutput] = useState('')
  const [newPublic, setNewPublic] = useState(false)
  const [adding, setAdding] = useState(false)
  const [addError, setAddError] = useState('')

  useEffect(() => { load() }, [problemId])

  async function load() {
    setLoading(true)
    try {
      const { data } = await problemsApi.getTestCases(problemId)
      setTestCases(data)
    } catch {
      setError('Не удалось загрузить тест-кейсы')
    } finally {
      setLoading(false)
    }
  }

  function startEdit(tc: TestCase) {
    setEditing(prev => ({
      ...prev,
      [tc.id]: { inputData: tc.inputData, expectedOutput: tc.expectedOutput, isPublic: tc.isPublic }
    }))
  }

  function cancelEdit(id: number) {
    setEditing(prev => { const n = { ...prev }; delete n[id]; return n })
  }

  async function saveEdit(tc: TestCase) {
    const e = editing[tc.id]
    if (!e) return
    setSaving(prev => ({ ...prev, [tc.id]: true }))
    try {
      const { data } = await problemsApi.updateTestCase(problemId, tc.id, {
        inputData: e.inputData,
        expectedOutput: e.expectedOutput,
        isPublic: e.isPublic,
        orderIndex: tc.orderIndex
      })
      setTestCases(prev => prev.map(t => t.id === tc.id ? data : t))
      cancelEdit(tc.id)
    } catch {
      alert('Ошибка при сохранении')
    } finally {
      setSaving(prev => ({ ...prev, [tc.id]: false }))
    }
  }

  async function handleDelete(tc: TestCase) {
    if (!confirm(`Удалить тест-кейс #${tc.orderIndex}?`)) return
    try {
      await problemsApi.deleteTestCase(problemId, tc.id)
      setTestCases(prev => prev.filter(t => t.id !== tc.id))
    } catch {
      alert('Ошибка при удалении')
    }
  }

  async function handleAdd(e: React.FormEvent) {
    e.preventDefault()
    setAddError('')
    if (!newInput.trim() || !newOutput.trim()) {
      setAddError('Заполните входные данные и ожидаемый результат')
      return
    }
    setAdding(true)
    try {
      const orderIndex = testCases.length > 0 ? Math.max(...testCases.map(t => t.orderIndex)) + 1 : 1
      const { data } = await problemsApi.addTestCase(problemId, {
        inputData: newInput,
        expectedOutput: newOutput,
        isPublic: newPublic,
        orderIndex
      })
      setTestCases(prev => [...prev, data])
      setNewInput('')
      setNewOutput('')
      setNewPublic(false)
      setShowAdd(false)
    } catch {
      setAddError('Ошибка при добавлении')
    } finally {
      setAdding(false)
    }
  }

  if (loading) return <div style={{ padding: '2rem' }}>Загрузка...</div>
  if (error) return <div style={{ padding: '2rem', color: 'var(--red)' }}>{error}</div>

  return (
    <div style={{ maxWidth: 900, margin: '2rem auto', padding: '0 1rem' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1.5rem' }}>
        <Link to={`/problems/${problemId}`} style={{ color: 'var(--accent)', textDecoration: 'none' }}>
          ← Назад к задаче
        </Link>
        <h2 style={{ margin: 0 }}>Управление тест-кейсами</h2>
      </div>

      {testCases.length === 0 && (
        <p style={{ color: 'var(--muted)' }}>Тест-кейсов пока нет.</p>
      )}

      {testCases.map((tc) => {
        const e = editing[tc.id]
        const isSaving = saving[tc.id]

        return (
          <div key={tc.id} style={{
            border: '1px solid var(--border)',
            borderRadius: 8,
            marginBottom: '1rem',
            padding: '1rem',
            background: 'var(--surface)'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
              <strong>Тест #{tc.orderIndex}</strong>
              <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                <span style={{
                  fontSize: '0.75rem',
                  padding: '2px 8px',
                  borderRadius: 12,
                  background: tc.isPublic ? '#1a3a1a' : 'var(--surface2)',
                  color: tc.isPublic ? 'var(--green)' : 'var(--muted)'
                }}>
                  {tc.isPublic ? 'Публичный' : 'Скрытый'}
                </span>
                {!e && (
                  <>
                    <button className="btn btn-sm" onClick={() => startEdit(tc)}>Ред.</button>
                    <button className="btn btn-sm btn-danger" onClick={() => handleDelete(tc)}>🗑</button>
                  </>
                )}
              </div>
            </div>

            {e ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                <label style={{ fontSize: '0.85rem', color: 'var(--muted)' }}>Входные данные</label>
                <textarea
                  rows={4}
                  value={e.inputData}
                  onChange={ev => setEditing(prev => ({ ...prev, [tc.id]: { ...prev[tc.id], inputData: ev.target.value } }))}
                  style={{ fontFamily: 'monospace', fontSize: '0.85rem', padding: '0.5rem', background: 'var(--surface2)', border: '1px solid var(--border)', borderRadius: 4, color: 'inherit', resize: 'vertical' }}
                />
                <label style={{ fontSize: '0.85rem', color: 'var(--muted)' }}>Ожидаемый вывод</label>
                <textarea
                  rows={4}
                  value={e.expectedOutput}
                  onChange={ev => setEditing(prev => ({ ...prev, [tc.id]: { ...prev[tc.id], expectedOutput: ev.target.value } }))}
                  style={{ fontFamily: 'monospace', fontSize: '0.85rem', padding: '0.5rem', background: 'var(--surface2)', border: '1px solid var(--border)', borderRadius: 4, color: 'inherit', resize: 'vertical' }}
                />
                <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', fontSize: '0.9rem' }}>
                  <input
                    type="checkbox"
                    checked={e.isPublic}
                    onChange={ev => setEditing(prev => ({ ...prev, [tc.id]: { ...prev[tc.id], isPublic: ev.target.checked } }))}
                  />
                  Публичный (виден участникам)
                </label>
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <button className="btn btn-primary btn-sm" onClick={() => saveEdit(tc)} disabled={isSaving}>
                    {isSaving ? 'Сохраняю...' : '✓ Сохранить'}
                  </button>
                  <button className="btn btn-sm" onClick={() => cancelEdit(tc.id)} disabled={isSaving}>Отмена</button>
                </div>
              </div>
            ) : (
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--muted)', marginBottom: '0.25rem' }}>Входные данные</div>
                  <pre style={{ margin: 0, fontSize: '0.8rem', background: 'var(--surface2)', padding: '0.5rem', borderRadius: 4, overflowX: 'auto', whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>{tc.inputData || '(пусто)'}</pre>
                </div>
                <div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--muted)', marginBottom: '0.25rem' }}>Ожидаемый вывод</div>
                  <pre style={{ margin: 0, fontSize: '0.8rem', background: 'var(--surface2)', padding: '0.5rem', borderRadius: 4, overflowX: 'auto', whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>{tc.expectedOutput}</pre>
                </div>
              </div>
            )}
          </div>
        )
      })}

      {!showAdd ? (
        <button className="btn btn-primary" onClick={() => setShowAdd(true)} style={{ marginTop: '0.5rem' }}>
          + Добавить тест-кейс
        </button>
      ) : (
        <form onSubmit={handleAdd} style={{
          border: '1px solid var(--accent)',
          borderRadius: 8,
          padding: '1rem',
          background: 'var(--surface)',
          marginTop: '0.5rem'
        }}>
          <h3 style={{ margin: '0 0 1rem' }}>Новый тест-кейс</h3>
          {addError && <div style={{ color: 'var(--red)', marginBottom: '0.75rem', fontSize: '0.9rem' }}>{addError}</div>}

          <label style={{ fontSize: '0.85rem', color: 'var(--muted)' }}>Входные данные</label>
          <textarea
            rows={4}
            value={newInput}
            onChange={e => setNewInput(e.target.value)}
            placeholder="stdin для программы..."
            style={{ display: 'block', width: '100%', marginTop: '0.25rem', marginBottom: '0.75rem', fontFamily: 'monospace', fontSize: '0.85rem', padding: '0.5rem', background: 'var(--surface2)', border: '1px solid var(--border)', borderRadius: 4, color: 'inherit', resize: 'vertical', boxSizing: 'border-box' }}
          />

          <label style={{ fontSize: '0.85rem', color: 'var(--muted)' }}>Ожидаемый вывод</label>
          <textarea
            rows={4}
            value={newOutput}
            onChange={e => setNewOutput(e.target.value)}
            placeholder="stdout от программы..."
            style={{ display: 'block', width: '100%', marginTop: '0.25rem', marginBottom: '0.75rem', fontFamily: 'monospace', fontSize: '0.85rem', padding: '0.5rem', background: 'var(--surface2)', border: '1px solid var(--border)', borderRadius: 4, color: 'inherit', resize: 'vertical', boxSizing: 'border-box' }}
          />

          <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', fontSize: '0.9rem', marginBottom: '1rem' }}>
            <input type="checkbox" checked={newPublic} onChange={e => setNewPublic(e.target.checked)} />
            Публичный (виден участникам в условии)
          </label>

          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button type="submit" className="btn btn-primary" disabled={adding}>
              {adding ? 'Добавляю...' : 'Добавить'}
            </button>
            <button type="button" className="btn" onClick={() => {
              setShowAdd(false); setAddError(''); setNewInput(''); setNewOutput(''); setNewPublic(false)
            }}>
              Отмена
            </button>
          </div>
        </form>
      )}
    </div>
  )
}
