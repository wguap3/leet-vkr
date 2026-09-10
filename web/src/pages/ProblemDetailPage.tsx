import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import Editor from '@monaco-editor/react'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import * as problemsApi from '../api/problems'
import type { ProblemDetail, Submission } from '../types'
import styles from './ProblemDetailPage.module.css'

const DEFAULT_TEMPLATE = `import java.util.*;
import java.io.*;

public class Solution {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        // Ваш код здесь
    }
}
`

const DIFF_LABELS: Record<string, string> = { EASY: 'Easy', MEDIUM: 'Medium', HARD: 'Hard' }
const VERDICT_LABELS: Record<string, string> = {
  AC: 'Принято ✓', WA: 'Неверный ответ', TLE: 'Превышено время',
  RE: 'Ошибка выполнения', CE: 'Ошибка компиляции', PENDING: 'Проверяется...'
}

function VerdictBadge({ v }: { v: string }) {
  return <span className={`badge badge-${v.toLowerCase()}`}>{VERDICT_LABELS[v] ?? v}</span>
}

export default function ProblemDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [problem, setProblem] = useState<ProblemDetail | null>(null)
  const [code, setCode] = useState(DEFAULT_TEMPLATE)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [lastSubmission, setLastSubmission] = useState<Submission | null>(null)
  const [tab, setTab] = useState<'description' | 'submissions'>('description')

  useEffect(() => {
    problemsApi.getProblem(Number(id))
      .then(({ data }) => {
        setProblem(data)
        if (data.codeTemplate) setCode(data.codeTemplate)
      })
      .finally(() => setLoading(false))
  }, [id])

  const handleSubmit = async () => {
    setSubmitting(true)
    setTab('submissions')
    try {
      const { data } = await problemsApi.submit(Number(id), code)
      setLastSubmission(data)
      let attempts = 0
      const poll = setInterval(async () => {
        attempts++
        try {
          const { data: sub } = await import('../api/client').then(m =>
            m.default.get(`/submissions/${data.id}`)
          )
          if (sub.verdict !== 'PENDING' || attempts > 20) {
            clearInterval(poll)
            setLastSubmission(sub)
          }
        } catch { clearInterval(poll) }
      }, 1500)
    } catch (err: any) {
      alert(err.response?.data?.message || 'Ошибка при отправке')
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) return <div className="empty-state" style={{ marginTop: 80 }}><span className="spinner" /></div>
  if (!problem) return <div className="empty-state">Задача не найдена</div>

  return (
    <div className={styles.layout}>
      {/* ── Left: description / submissions ── */}
      <div className={styles.left}>
        <div className={styles.problemHeader}>
          <h2>{problem.title}</h2>
          <div className={styles.meta}>
            <span className={`badge badge-${problem.difficulty.toLowerCase()}`}>{DIFF_LABELS[problem.difficulty]}</span>
            <span className={styles.metaChip}>⏱ {problem.timeLimitMs} мс</span>
            <span className={styles.metaChip}>💾 {problem.memoryLimitMb} МБ</span>
            {problem.tags.map(t => (
              <span key={t.id} className={styles.metaChip}>{t.name}</span>
            ))}
          </div>
          <div className={styles.tabs}>
            <button className={tab === 'description' ? styles.activeTab : styles.tab} onClick={() => setTab('description')}>Условие</button>
            <button className={tab === 'submissions' ? styles.activeTab : styles.tab} onClick={() => setTab('submissions')}>Посылки</button>
          </div>
        </div>

        {tab === 'description' ? (
          <div className={styles.description}>
            <ReactMarkdown remarkPlugins={[remarkGfm]}>{problem.description}</ReactMarkdown>
            {problem.publicTestCases.length > 0 && (
              <div className={styles.examples}>
                <div className={styles.examplesLabel}>Примеры</div>
                {problem.publicTestCases.map((tc, i) => (
                  <div key={tc.id} className={styles.example}>
                    <div className={styles.exampleHead}>Пример {i + 1}</div>
                    <div className={styles.ioGrid}>
                      <div><strong>Ввод</strong><pre>{tc.inputData}</pre></div>
                      <div><strong>Вывод</strong><pre>{tc.expectedOutput}</pre></div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        ) : (
          <div className={styles.submissionsPanel}>
            {lastSubmission ? (
              <div className={styles.resultCard}>
                <div className={styles.resultRow}>
                  <VerdictBadge v={lastSubmission.verdict} />
                  {lastSubmission.executionTimeMs != null && (
                    <span style={{ fontSize: 13, color: 'var(--text-3)' }}>{lastSubmission.executionTimeMs} мс</span>
                  )}
                  {lastSubmission.verdict === 'PENDING' && <><span className="spinner" /><span style={{ fontSize: 13, color: 'var(--text-3)' }}>Проверяется...</span></>}
                </div>
              </div>
            ) : (
              <div className="empty-state">Посылок пока нет</div>
            )}
          </div>
        )}
      </div>

      {/* ── Right: editor ── */}
      <div className={styles.right}>
        <div className={styles.editorBar}>
          <span className={styles.langChip}>Java</span>
          <button className="btn btn-primary btn-sm" onClick={handleSubmit} disabled={submitting}>
            {submitting ? <><span className="spinner" /> Отправка…</> : '▶ Отправить'}
          </button>
        </div>
        <div className={styles.editorWrap}>
          <Editor
            language="java"
            value={code}
            onChange={v => setCode(v ?? '')}
            theme="vs-dark"
            options={{
              fontSize: 14,
              fontFamily: "'JetBrains Mono', 'Fira Code', monospace",
              fontLigatures: true,
              minimap: { enabled: false },
              scrollBeyondLastLine: false,
              tabSize: 4,
              padding: { top: 14 },
              lineNumbersMinChars: 3,
              renderLineHighlight: 'gutter',
              smoothScrolling: true,
            }}
          />
        </div>
      </div>
    </div>
  )
}
