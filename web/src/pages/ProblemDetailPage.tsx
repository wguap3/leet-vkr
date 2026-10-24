import { useEffect, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import Editor from '@monaco-editor/react'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import * as problemsApi from '../api/problems'
import type { Hint, ProblemDetail, ProblemStats, RunCodeResult, Submission } from '../types'
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
const STATUS_COLORS: Record<string, string> = {
  OK: 'var(--green)', CE: 'var(--red)', TLE: 'var(--yellow)', RE: 'var(--red)'
}

function VerdictBadge({ v }: { v: string }) {
  return <span className={`badge badge-${v.toLowerCase()}`}>{VERDICT_LABELS[v] ?? v}</span>
}

function StatBar({ label, count, total, color }: { label: string; count: number; total: number; color: string }) {
  const pct = total > 0 ? (count / total) * 100 : 0
  return (
    <div className={styles.statBar}>
      <div className={styles.statBarLabel}>
        <span style={{ color }}>{label}</span>
        <span>{count}</span>
      </div>
      <div className={styles.statBarTrack}>
        <div className={styles.statBarFill} style={{ width: `${pct}%`, background: color }} />
      </div>
    </div>
  )
}

export default function ProblemDetailPage() {
  const { id } = useParams<{ id: string }>()
  const pid = Number(id)
  const storageKey = `stackr_code_${id}`

  const [problem, setProblem] = useState<ProblemDetail | null>(null)
  const [code, setCode] = useState(() => {
    try { return localStorage.getItem(storageKey) || DEFAULT_TEMPLATE } catch { return DEFAULT_TEMPLATE }
  })
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [lastSubmission, setLastSubmission] = useState<Submission | null>(null)
  const [tab, setTab] = useState<'description' | 'hints' | 'stats' | 'submissions'>('description')

  // Custom test
  const [customInput, setCustomInput] = useState('')
  const [runResult, setRunResult] = useState<RunCodeResult | null>(null)
  const [running, setRunning] = useState(false)
  const [showCustomTest, setShowCustomTest] = useState(false)

  // Hints
  const [hints, setHints] = useState<Hint[]>([])
  const [hintsLoading, setHintsLoading] = useState(false)

  // Stats
  const [stats, setStats] = useState<ProblemStats | null>(null)
  const [statsLoading, setStatsLoading] = useState(false)

  // Autosave
  const autoSaveTimer = useRef<ReturnType<typeof setTimeout>>()
  const handleCodeChange = (v: string | undefined) => {
    const val = v ?? ''
    setCode(val)
    clearTimeout(autoSaveTimer.current)
    autoSaveTimer.current = setTimeout(() => {
      try { localStorage.setItem(storageKey, val) } catch {}
    }, 800)
  }

  useEffect(() => {
    problemsApi.getProblem(pid)
      .then(({ data }) => {
        setProblem(data)
        // Only use template if no saved code in localStorage
        try {
          const saved = localStorage.getItem(storageKey)
          if (!saved && data.codeTemplate) setCode(data.codeTemplate)
        } catch {
          if (data.codeTemplate) setCode(data.codeTemplate)
        }
      })
      .finally(() => setLoading(false))
  }, [pid])

  // Load hints when tab opened
  useEffect(() => {
    if (tab === 'hints' && hints.length === 0) {
      setHintsLoading(true)
      problemsApi.getHints(pid)
        .then(({ data }) => setHints(data))
        .finally(() => setHintsLoading(false))
    }
  }, [tab, pid])

  // Load stats when tab opened
  useEffect(() => {
    if (tab === 'stats' && !stats) {
      setStatsLoading(true)
      problemsApi.getProblemStats(pid)
        .then(({ data }) => setStats(data))
        .finally(() => setStatsLoading(false))
    }
  }, [tab, pid])

  const handleSubmit = async () => {
    setSubmitting(true)
    setTab('submissions')
    try {
      const { data } = await problemsApi.submit(pid, code)
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
            // Refresh hints after submission (unlock state may change)
            setHints([])
          }
        } catch { clearInterval(poll) }
      }, 1500)
    } catch (err: any) {
      alert(err.response?.data?.message || 'Ошибка при отправке')
    } finally {
      setSubmitting(false)
    }
  }

  const handleRun = async () => {
    setRunning(true)
    setRunResult(null)
    try {
      const { data } = await problemsApi.runCode(pid, code, customInput)
      setRunResult(data)
    } catch (err: any) {
      setRunResult({ stdout: '', stderr: err.response?.data?.message || 'Ошибка', elapsedMs: 0, status: 'RE' })
    } finally {
      setRunning(false)
    }
  }

  if (loading) return <div className="empty-state" style={{ marginTop: 80 }}><span className="spinner" /></div>
  if (!problem) return <div className="empty-state">Задача не найдена</div>

  return (
    <div className={styles.layout}>
      {/* ── Left panel ── */}
      <div className={styles.left}>
        <div className={styles.problemHeader}>
          <h2>{problem.title}</h2>
          <div className={styles.meta}>
            <span className={`badge badge-${problem.difficulty.toLowerCase()}`}>{DIFF_LABELS[problem.difficulty]}</span>
            <span className={styles.metaChip}>⏱ {problem.timeLimitMs} мс</span>
            <span className={styles.metaChip}>💾 {problem.memoryLimitMb} МБ</span>
            {Array.from(problem.tags).map(t => (
              <span key={t.id} className={styles.metaChip}>{t.name}</span>
            ))}
          </div>
          <div className={styles.tabs}>
            {(['description', 'hints', 'stats', 'submissions'] as const).map(t => (
              <button
                key={t}
                className={tab === t ? styles.activeTab : styles.tab}
                onClick={() => setTab(t)}
              >
                {{ description: 'Условие', hints: 'Подсказки', stats: 'Статистика', submissions: 'Посылки' }[t]}
              </button>
            ))}
          </div>
        </div>

        {/* DESCRIPTION */}
        {tab === 'description' && (
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
        )}

        {/* HINTS */}
        {tab === 'hints' && (
          <div className={styles.hintsPanel}>
            {hintsLoading ? (
              <div className="empty-state"><span className="spinner" /></div>
            ) : hints.length === 0 ? (
              <div className="empty-state" style={{ padding: '40px 0' }}>
                <div style={{ fontSize: 32, marginBottom: 8 }}>🔒</div>
                <div>Подсказок нет</div>
              </div>
            ) : (
              hints.map((h, i) => (
                <div key={h.id} className={`${styles.hintCard} ${h.unlocked ? styles.hintUnlocked : styles.hintLocked}`}>
                  <div className={styles.hintHeader}>
                    <span className={styles.hintNum}>{h.unlocked ? '💡' : '🔒'} Подсказка {i + 1}</span>
                    {!h.unlocked && (
                      <span className={styles.hintUnlockHint}>
                        Откроется после {h.unlocksAfterAttempts} неверных попыток
                      </span>
                    )}
                  </div>
                  {h.unlocked
                    ? <p className={styles.hintText}>{h.text}</p>
                    : <p className={styles.hintTextLocked}>Сделай ещё попытки, чтобы разблокировать</p>
                  }
                </div>
              ))
            )}
          </div>
        )}

        {/* STATS */}
        {tab === 'stats' && (
          <div className={styles.statsPanel}>
            {statsLoading ? (
              <div className="empty-state"><span className="spinner" /></div>
            ) : stats ? (
              <>
                <div className={styles.statsRow}>
                  <div className={styles.statCard}>
                    <div className={styles.statVal}>{stats.totalSubmissions}</div>
                    <div className={styles.statLbl}>Посылок</div>
                  </div>
                  <div className={styles.statCard}>
                    <div className={styles.statVal} style={{ color: 'var(--green)' }}>{stats.acceptanceRate.toFixed(1)}%</div>
                    <div className={styles.statLbl}>Принято</div>
                  </div>
                  <div className={styles.statCard}>
                    <div className={styles.statVal}>{stats.avgExecutionTimeMs ? Math.round(stats.avgExecutionTimeMs) + ' мс' : '—'}</div>
                    <div className={styles.statLbl}>Среднее время AC</div>
                  </div>
                </div>
                <div className={styles.statBars}>
                  <StatBar label="AC"  count={stats.acCount}  total={stats.totalSubmissions} color="var(--green)" />
                  <StatBar label="WA"  count={stats.waCount}  total={stats.totalSubmissions} color="var(--red)" />
                  <StatBar label="TLE" count={stats.tleCount} total={stats.totalSubmissions} color="var(--yellow)" />
                  <StatBar label="RE"  count={stats.reCount}  total={stats.totalSubmissions} color="var(--red)" />
                  <StatBar label="CE"  count={stats.ceCount}  total={stats.totalSubmissions} color="var(--text-3)" />
                </div>
              </>
            ) : null}
          </div>
        )}

        {/* SUBMISSIONS */}
        {tab === 'submissions' && (
          <div className={styles.submissionsPanel}>
            {lastSubmission ? (
              <div className={styles.resultCard}>
                <div className={styles.resultRow}>
                  <VerdictBadge v={lastSubmission.verdict} />
                  {lastSubmission.executionTimeMs != null && (
                    <span style={{ fontSize: 13, color: 'var(--text-3)' }}>{lastSubmission.executionTimeMs} мс</span>
                  )}
                  {lastSubmission.verdict === 'PENDING' && (
                    <><span className="spinner" /><span style={{ fontSize: 13, color: 'var(--text-3)' }}>Проверяется...</span></>
                  )}
                </div>
              </div>
            ) : (
              <div className="empty-state">Посылок пока нет</div>
            )}
          </div>
        )}
      </div>

      {/* ── Right: editor + custom test ── */}
      <div className={styles.right}>
        <div className={styles.editorBar}>
          <span className={styles.langChip}>Java</span>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <button
              className="btn btn-outline btn-sm"
              onClick={() => setShowCustomTest(v => !v)}
            >
              {showCustomTest ? '▾ Тест' : '▸ Тест'}
            </button>
            <button className="btn btn-primary btn-sm" onClick={handleSubmit} disabled={submitting}>
              {submitting ? <><span className="spinner" /> Отправка…</> : '▶ Отправить'}
            </button>
          </div>
        </div>

        <div className={styles.editorWrap} style={{ flex: showCustomTest ? '0 0 calc(100% - 220px)' : '1' }}>
          <Editor
            language="java"
            value={code}
            onChange={handleCodeChange}
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

        {/* Custom test panel */}
        {showCustomTest && (
          <div className={styles.customTest}>
            <div className={styles.customTestRow}>
              <div className={styles.customTestCol}>
                <div className={styles.customTestLabel}>Входные данные</div>
                <textarea
                  className={styles.customTestArea}
                  value={customInput}
                  onChange={e => setCustomInput(e.target.value)}
                  placeholder="Введи входные данные..."
                  spellCheck={false}
                />
              </div>
              <div className={styles.customTestCol}>
                <div className={styles.customTestLabel} style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span>Вывод</span>
                  {runResult && (
                    <span style={{ color: STATUS_COLORS[runResult.status] ?? 'var(--text-3)', fontSize: 11 }}>
                      {runResult.status} · {runResult.elapsedMs} мс
                    </span>
                  )}
                </div>
                <pre className={styles.customTestOutput}>
                  {runResult
                    ? (runResult.stdout || runResult.stderr || '(нет вывода)')
                    : <span style={{ color: 'var(--text-4)' }}>Нажми «Запустить»</span>
                  }
                </pre>
              </div>
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 8 }}>
              <button className="btn btn-secondary btn-sm" onClick={handleRun} disabled={running}>
                {running ? <><span className="spinner" /> Запуск…</> : '▶ Запустить'}
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
