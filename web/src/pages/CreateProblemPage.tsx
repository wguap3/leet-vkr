import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import * as problemsApi from '../api/problems'
import type { Difficulty } from '../types'
import styles from './CreatePage.module.css'

interface TestCaseInput {
  inputData: string
  expectedOutput: string
  isPublic: boolean
}

interface HintInput {
  text: string
  unlocksAfterAttempts: number
}

const DIFFICULTIES: { value: Difficulty; label: string }[] = [
  { value: 'EASY', label: 'Лёгкая' },
  { value: 'MEDIUM', label: 'Средняя' },
  { value: 'HARD', label: 'Сложная' },
]

export default function CreateProblemPage() {
  const navigate = useNavigate()
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [tags, setTags] = useState<{ id: number; name: string }[]>([])

  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [difficulty, setDifficulty] = useState<Difficulty>('EASY')
  const [timeLimit, setTimeLimit] = useState(1000)
  const [memoryLimit, setMemoryLimit] = useState(128)
  const [codeTemplate, setCodeTemplate] = useState(
    'import java.util.Scanner;\n\npublic class Solution {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        // Ваш код здесь\n    }\n}'
  )
  const [selectedTags, setSelectedTags] = useState<number[]>([])
  const [testCases, setTestCases] = useState<TestCaseInput[]>([
    { inputData: '', expectedOutput: '', isPublic: true },
    { inputData: '', expectedOutput: '', isPublic: false },
  ])
  const [hints, setHints] = useState<HintInput[]>([])
  const [publish, setPublish] = useState(true)

  useEffect(() => {
    problemsApi.getTags().then(r => setTags(r.data)).catch(() => {})
  }, [])

  const addTestCase = () =>
    setTestCases(prev => [...prev, { inputData: '', expectedOutput: '', isPublic: false }])
  const removeTestCase = (i: number) =>
    setTestCases(prev => prev.filter((_, idx) => idx !== i))
  const updateTestCase = (i: number, field: keyof TestCaseInput, value: string | boolean) =>
    setTestCases(prev => prev.map((tc, idx) => idx === i ? { ...tc, [field]: value } : tc))

  const addHint = () =>
    setHints(prev => [...prev, { text: '', unlocksAfterAttempts: prev.length + 1 }])
  const removeHint = (i: number) =>
    setHints(prev => prev.filter((_, idx) => idx !== i))
  const updateHint = (i: number, field: keyof HintInput, value: string | number) =>
    setHints(prev => prev.map((h, idx) => idx === i ? { ...h, [field]: value } : h))

  const toggleTag = (id: number) =>
    setSelectedTags(prev => prev.includes(id) ? prev.filter(t => t !== id) : [...prev, id])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!title.trim()) { setError('Введите название'); return }
    if (!description.trim()) { setError('Введите условие'); return }
    const validTc = testCases.filter(tc => tc.inputData.trim() && tc.expectedOutput.trim())
    if (validTc.length === 0) { setError('Добавьте хотя бы один тест-кейс'); return }

    setSaving(true)
    setError('')
    try {
      const { data: problem } = await problemsApi.createProblem({
        title: title.trim(),
        description: description.trim(),
        difficulty,
        timeLimitMs: timeLimit,
        memoryLimitMb: memoryLimit,
        tagIds: selectedTags,
        codeTemplate: codeTemplate.trim() || undefined,
      })

      for (let i = 0; i < validTc.length; i++) {
        await problemsApi.addTestCase(problem.id, { ...validTc[i], orderIndex: i })
      }

      // Save hints
      for (let i = 0; i < hints.length; i++) {
        const h = hints[i]
        if (h.text.trim()) {
          await problemsApi.createHint(problem.id, { text: h.text.trim(), orderIndex: i, unlocksAfterAttempts: h.unlocksAfterAttempts })
        }
      }

      if (publish) {
        await problemsApi.setVisible(problem.id, true)
      }

      navigate(`/problems/${problem.id}`)
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Ошибка при сохранении')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="page-container">
      <div className="page-header">
        <h1>Новая задача</h1>
      </div>

      <form onSubmit={handleSubmit} className={styles.form}>
        {error && <div className={styles.error}>{error}</div>}

        {/* Основное */}
        <div className="card">
          <div className={styles.section}>
            <h3 className={styles.sectionTitle}>Основное</h3>
            <div className={styles.field}>
              <label>Название *</label>
              <input value={title} onChange={e => setTitle(e.target.value)} placeholder="Например: Сумма двух чисел" />
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
            <h3 className={styles.sectionTitle}>Условие задачи * <span className={styles.hint}>(поддерживается Markdown)</span></h3>
            <textarea
              className={styles.textarea}
              rows={12}
              value={description}
              onChange={e => setDescription(e.target.value)}
              placeholder="## Задача&#10;&#10;Описание задачи...&#10;&#10;## Формат ввода&#10;&#10;...&#10;&#10;## Формат вывода&#10;&#10;..."
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
            />
          </div>
        </div>

        {/* Тест-кейсы */}
        <div className="card">
          <div className={styles.section}>
            <div className={styles.sectionHeader}>
              <h3 className={styles.sectionTitle}>Тест-кейсы *</h3>
              <button type="button" className="btn btn-outline btn-sm" onClick={addTestCase}>+ Добавить</button>
            </div>
            <div className={styles.tcList}>
              {testCases.map((tc, i) => (
                <div key={i} className={styles.tcRow}>
                  <div className={styles.tcIndex}>#{i + 1}</div>
                  <div className={styles.field} style={{ flex: 1 }}>
                    <label>Ввод</label>
                    <textarea
                      rows={3}
                      className={styles.tcArea}
                      value={tc.inputData}
                      onChange={e => updateTestCase(i, 'inputData', e.target.value)}
                      placeholder="stdin"
                    />
                  </div>
                  <div className={styles.field} style={{ flex: 1 }}>
                    <label>Ожидаемый вывод</label>
                    <textarea
                      rows={3}
                      className={styles.tcArea}
                      value={tc.expectedOutput}
                      onChange={e => updateTestCase(i, 'expectedOutput', e.target.value)}
                      placeholder="stdout"
                    />
                  </div>
                  <div className={styles.tcControls}>
                    <label className={styles.checkLabel}>
                      <input
                        type="checkbox"
                        checked={tc.isPublic}
                        onChange={e => updateTestCase(i, 'isPublic', e.target.checked)}
                      />
                      Публичный
                    </label>
                    {testCases.length > 1 && (
                      <button type="button" className={styles.removeBtn} onClick={() => removeTestCase(i)}>✕</button>
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
              <h3 className={styles.sectionTitle}>Подсказки <span className={styles.hint}>(необязательно)</span></h3>
              <button type="button" className="btn btn-outline btn-sm" onClick={addHint}>+ Добавить</button>
            </div>
            {hints.length === 0 && (
              <p style={{ fontSize: 13, color: 'var(--text-4)', margin: '8px 0 0' }}>
                Подсказки открываются участникам после N неверных попыток.
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

        {/* Публикация */}
        <div className="card">
          <div className={styles.section}>
            <label className={styles.checkLabel} style={{ fontSize: 14 }}>
              <input type="checkbox" checked={publish} onChange={e => setPublish(e.target.checked)} />
              Опубликовать задачу сразу (сделать видимой для участников)
            </label>
          </div>
        </div>

        <div className={styles.actions}>
          <button type="button" className="btn btn-outline" onClick={() => navigate('/problems')}>Отмена</button>
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? 'Сохранение...' : 'Создать задачу'}
          </button>
        </div>
      </form>
    </div>
  )
}
