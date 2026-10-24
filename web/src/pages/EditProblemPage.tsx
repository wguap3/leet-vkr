import { useEffect, useState } from 'react'
import { useNavigate, useParams, Link } from 'react-router-dom'
import * as problemsApi from '../api/problems'
import type { Difficulty } from '../types'
import styles from './CreatePage.module.css'

interface TCState {
  id?: number          // undefined = newly added, not yet on server
  inputData: string
  expectedOutput: string
  isPublic: boolean
}

interface HintState {
  id?: number
  text: string
  unlocksAfterAttempts: number
  orderIndex: number
}

const DIFFICULTIES: { value: Difficulty; label: string }[] = [
  { value: 'EASY',   label: 'Лёгкая'  },
  { value: 'MEDIUM', label: 'Средняя' },
  { value: 'HARD',   label: 'Сложная' },
]

export default function EditProblemPage() {
  const { id } = useParams<{ id: string }>()
  const pid = Number(id)
  const navigate = useNavigate()

  // Loading state
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  // Available tags
  const [tags, setTags] = useState<{ id: number; name: string }[]>([])

  // Form fields
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [difficulty, setDifficulty] = useState<Difficulty>('EASY')
  const [timeLimit, setTimeLimit] = useState(1000)
  const [memoryLimit, setMemoryLimit] = useState(128)
  const [codeTemplate, setCodeTemplate] = useState('')
  const [selectedTags, setSelectedTags] = useState<number[]>([])
  const [visible, setVisible] = useState(true)

  // Test cases
  const [testCases, setTestCases] = useState<TCState[]>([])
  const [originalTcIds, setOriginalTcIds] = useState<number[]>([])  // ids loaded from server

  // Hints
  const [hints, setHints] = useState<HintState[]>([])
  const [originalHintIds, setOriginalHintIds] = useState<number[]>([])

  // ----- Load on mount -----
  useEffect(() => {
    Promise.all([
      problemsApi.getProblem(pid),
      problemsApi.getTestCases(pid),
      problemsApi.getHints(pid),
      problemsApi.getTags(),
    ]).then(([{ data: p }, { data: tcs }, { data: hs }, { data: allTags }]) => {
      setTitle(p.title)
      setDescription(p.description)
      setDifficulty(p.difficulty)
      setTimeLimit(p.timeLimitMs)
      setMemoryLimit(p.memoryLimitMb)
      setCodeTemplate(p.codeTemplate ?? '')
      setSelectedTags(p.tags.map(t => t.id))

      const tcStates: TCState[] = tcs.map(tc => ({
        id: tc.id,
        inputData: tc.inputData,
        expectedOutput: tc.expectedOutput,
        isPublic: tc.isPublic,
      }))
      setTestCases(tcStates)
      setOriginalTcIds(tcs.map(tc => tc.id))

      const hintStates: HintState[] = hs.map((h, i) => ({
        id: h.id,
        text: h.text ?? '',
        unlocksAfterAttempts: h.unlocksAfterAttempts,
        orderIndex: i,
      }))
      setHints(hintStates)
      setOriginalHintIds(hs.map(h => h.id))

      setTags(allTags)
    }).catch(() => setError('Не удалось загрузить задачу'))
      .finally(() => setLoading(false))
  }, [pid])

  // ----- Helpers -----
  const toggleTag = (tagId: number) =>
    setSelectedTags(prev => prev.includes(tagId) ? prev.filter(t => t !== tagId) : [...prev, tagId])

  const addTC = () =>
    setTestCases(prev => [...prev, { inputData: '', expectedOutput: '', isPublic: false }])
  const removeTC = (i: number) =>
    setTestCases(prev => prev.filter((_, idx) => idx !== i))
  const updateTC = (i: number, field: keyof TCState, value: string | boolean) =>
    setTestCases(prev => prev.map((tc, idx) => idx === i ? { ...tc, [field]: value } : tc))

  const addHint = () =>
    setHints(prev => [...prev, { text: '', unlocksAfterAttempts: prev.length + 1, orderIndex: prev.length }])
  const removeHint = (i: number) =>
    setHints(prev => prev.filter((_, idx) => idx !== i))
  const updateHint = (i: number, field: keyof HintState, value: string | number) =>
    setHints(prev => prev.map((h, idx) => idx === i ? { ...h, [field]: value } : h))

  // ----- Submit -----
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!title.trim())       { setError('Введите название'); return }
    if (!description.trim()) { setError('Введите условие'); return }
    const validTc = testCases.filter(tc => tc.inputData.trim() && tc.expectedOutput.trim())
    if (validTc.length === 0) { setError('Нужен хотя бы один тест-кейс'); return }

    setSaving(true)
    setError('')
    try {
      // 1. Update main problem fields
      await problemsApi.updateProblem(pid, {
        title: title.trim(),
        description: description.trim(),
        difficulty,
        timeLimitMs: timeLimit,
        memoryLimitMb: memoryLimit,
        tagIds: selectedTags,
        codeTemplate: codeTemplate.trim() || undefined,
      })

      // 2. Test cases
      const currentIds = testCases.filter(tc => tc.id !== undefined).map(tc => tc.id as number)
      // Delete removed test cases
      for (const oldId of originalTcIds) {
        if (!currentIds.includes(oldId)) {
          await problemsApi.deleteTestCase(pid, oldId)
        }
      }
      // Update existing / add new
      for (let i = 0; i < testCases.length; i++) {
        const tc = testCases[i]
        if (!tc.inputData.trim() || !tc.expectedOutput.trim()) continue
        if (tc.id !== undefined) {
          await problemsApi.updateTestCase(pid, tc.id, {
            inputData: tc.inputData,
            expectedOutput: tc.expectedOutput,
            isPublic: tc.isPublic,
            orderIndex: i,
          })
        } else {
          await problemsApi.addTestCase(pid, {
            inputData: tc.inputData,
            expectedOutput: tc.expectedOutput,
            isPublic: tc.isPublic,
            orderIndex: i,
          })
        }
      }

      // 3. Hints — delete all originals and re-create (simplest approach)
      for (const oldId of originalHintIds) {
        await problemsApi.deleteHint(pid, oldId).catch(() => {})
      }
      for (let i = 0; i < hints.length; i++) {
        const h = hints[i]
        if (h.text.trim()) {
          await problemsApi.createHint(pid, {
            text: h.text.trim(),
            orderIndex: i,
            unlocksAfterAttempts: h.unlocksAfterAttempts,
          })
        }
      }

      // 4. Visibility
      await problemsApi.setVisible(pid, visible)

      navigate(`/problems/${pid}`)
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Ошибка при сохранении')
    } finally {
      setSaving(false)
    }
  }

  // ----- Render -----
  if (loading) return <div className="empty-state" style={{ marginTop: 80 }}><span className="spinner" /></div>

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <Link
            to={`/problems/${pid}`}
            style={{ fontSize: 12, color: 'var(--text-4)', textDecoration: 'none', display: 'block', marginBottom: 6 }}
          >
            ← К задаче
          </Link>
          <h1>Редактировать задачу</h1>
        </div>
      </div>

      <form onSubmit={handleSubmit} className={styles.form}>
        {error && <div className={styles.error}>{error}</div>}

        {/* Основное */}
        <div className="card">
          <div className={styles.section}>
            <h3 className={styles.sectionTitle}>Основное</h3>
            <div className={styles.field}>
              <label>Название *</label>
              <input value={title} onChange={e => setTitle(e.target.value)} placeholder="Название задачи" />
            </div>
            <div className={styles.row}>
              <div className={styles.field}>
                <label>Сложность</label>
                <select value={difficulty} onChange={e => setDifficulty(e.target.value as Difficulty)}>
                  {DIFFICULTIES.map(d => <option key={d.value} value={d.value}>{d.label}</option>)}
                </select>
              </div>
              <div className={styles.field}>
                <label>Лимит времени (мс)</label>
                <input type="number" min={100} max={10000} value={timeLimit} onChange={e => setTimeLimit(+e.target.value)} />
              </div>
              <div className={styles.field}>
                <label>Лимит памяти (МБ)</label>
                <input type="number" min={16} max={512} value={memoryLimit} onChange={e => setMemoryLimit(+e.target.value)} />
              </div>
            </div>
          </div>
        </div>

        {/* Теги */}
        {tags.length > 0 && (
          <div className="card">
            <div className={styles.section}>
              <h3 className={styles.sectionTitle}>Теги</h3>
              <div className={styles.tagList}>
                {tags.map(t => (
                  <button
                    key={t.id}
                    type="button"
                    className={`${styles.tagBtn} ${selectedTags.includes(t.id) ? styles.tagBtnActive : ''}`}
                    onClick={() => toggleTag(t.id)}
                  >
                    {t.name}
                  </button>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* Условие */}
        <div className="card">
          <div className={styles.section}>
            <h3 className={styles.sectionTitle}>
              Условие задачи * <span className={styles.hint}>(поддерживается Markdown)</span>
            </h3>
            <textarea
              className={styles.textarea}
              rows={12}
              value={description}
              onChange={e => setDescription(e.target.value)}
              placeholder="## Задача&#10;&#10;Описание задачи..."
            />
          </div>
        </div>

        {/* Шаблон кода */}
        <div className="card">
          <div className={styles.section}>
            <h3 className={styles.sectionTitle}>Шаблон кода</h3>
            <textarea
              className={styles.textarea}
              rows={8}
              value={codeTemplate}
              onChange={e => setCodeTemplate(e.target.value)}
              placeholder="public class Solution { ... }"
            />
          </div>
        </div>

        {/* Тест-кейсы */}
        <div className="card">
          <div className={styles.section}>
            <div className={styles.sectionHeader}>
              <h3 className={styles.sectionTitle}>Тест-кейсы *</h3>
              <button type="button" className="btn btn-outline btn-sm" onClick={addTC}>+ Добавить</button>
            </div>
            <div className={styles.tcList}>
              {testCases.map((tc, i) => (
                <div key={i} className={styles.tcRow}>
                  <div className={styles.tcIndex}>
                    #{i + 1}
                    {tc.id !== undefined && (
                      <span title="Сохранён на сервере" style={{ fontSize: 9, color: 'var(--green)', display: 'block' }}>●</span>
                    )}
                  </div>
                  <div className={styles.field} style={{ flex: 1 }}>
                    <label>Ввод</label>
                    <textarea
                      rows={3}
                      className={styles.tcArea}
                      value={tc.inputData}
                      onChange={e => updateTC(i, 'inputData', e.target.value)}
                      placeholder="stdin"
                    />
                  </div>
                  <div className={styles.field} style={{ flex: 1 }}>
                    <label>Ожидаемый вывод</label>
                    <textarea
                      rows={3}
                      className={styles.tcArea}
                      value={tc.expectedOutput}
                      onChange={e => updateTC(i, 'expectedOutput', e.target.value)}
                      placeholder="stdout"
                    />
                  </div>
                  <div className={styles.tcControls}>
                    <label className={styles.checkLabel}>
                      <input
                        type="checkbox"
                        checked={tc.isPublic}
                        onChange={e => updateTC(i, 'isPublic', e.target.checked)}
                      />
                      Публичный
                    </label>
                    {testCases.length > 1 && (
                      <button type="button" className={styles.removeBtn} onClick={() => removeTC(i)}>✕</button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Подсказки */}
        <div className="card">
          <div className={styles.section}>
            <div className={styles.sectionHeader}>
              <h3 className={styles.sectionTitle}>
                Подсказки <span className={styles.hint}>(необязательно)</span>
              </h3>
              <button type="button" className="btn btn-outline btn-sm" onClick={addHint}>+ Добавить</button>
            </div>
            {hints.length === 0 && (
              <p style={{ fontSize: 13, color: 'var(--text-4)', margin: '8px 0 0' }}>
                Подсказки открываются после N неверных попыток.
              </p>
            )}
            <div className={styles.tcList}>
              {hints.map((h, i) => (
                <div key={i} className={styles.tcRow}>
                  <div className={styles.tcIndex}>#{i + 1}</div>
                  <div className={styles.field} style={{ flex: 1 }}>
                    <label>Текст подсказки</label>
                    <textarea
                      rows={2}
                      className={styles.tcArea}
                      value={h.text}
                      onChange={e => updateHint(i, 'text', e.target.value)}
                      placeholder="Подумай о..."
                    />
                  </div>
                  <div className={styles.field} style={{ width: 160 }}>
                    <label>После N попыток</label>
                    <input
                      type="number"
                      min={0}
                      max={100}
                      value={h.unlocksAfterAttempts}
                      onChange={e => updateHint(i, 'unlocksAfterAttempts', +e.target.value)}
                    />
                  </div>
                  <div className={styles.tcControls}>
                    <button type="button" className={styles.removeBtn} onClick={() => removeHint(i)}>✕</button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Видимость */}
        <div className="card">
          <div className={styles.section}>
            <label className={styles.checkLabel} style={{ fontSize: 14 }}>
              <input type="checkbox" checked={visible} onChange={e => setVisible(e.target.checked)} />
              Задача видна участникам (опубликована)
            </label>
          </div>
        </div>

        <div className={styles.actions}>
          <button type="button" className="btn btn-outline" onClick={() => navigate(`/problems/${pid}`)}>
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
