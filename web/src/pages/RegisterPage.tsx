import Brand from '../components/Brand'
import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import * as authApi from '../api/auth'
import { useAuthStore } from '../store/authStore'
import styles from './AuthPage.module.css'

export default function RegisterPage() {
  const [form, setForm] = useState({ username: '', email: '', password: '' })
  const [role, setRole] = useState<'CONTESTANT' | 'ORGANIZER'>('CONTESTANT')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { setTokens, setUser } = useAuthStore()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const target = params.get('redirect') || '/problems'
  const redirect = target.startsWith('/') && !target.startsWith('//') && !target.includes(String.fromCharCode(92)) ? target : '/problems'

  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm(f => ({ ...f, [k]: e.target.value }))

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await authApi.register(form.username, form.email, form.password, role)
      const { data: tokens } = await authApi.login(form.username, form.password)
      setTokens(tokens.accessToken, tokens.refreshToken)
      const { data: user } = await authApi.getMe()
      setUser(user)
      navigate(redirect)
    } catch (err: any) {
      if (!err.response) {
        setError('Не удалось связаться с сервером. Проверьте соединение и попробуйте снова.')
      } else {
        const data = err.response.data
        const msg = (typeof data === 'object' && data !== null)
          ? (data.message || data.error || `Ошибка ${err.response.status}`)
          : (typeof data === 'string' && data.trim() ? data : `Ошибка ${err.response.status}`)
        setError(msg)
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className={styles.wrapper}>
      <div className={`card ${styles.card}`}>
        <div className={styles.logoMark}><Brand /></div>
        <h1 className={styles.title}>Регистрация</h1>
        <p className={styles.subtitle}>Создай аккаунт и начни решать задачи</p>
        {error && <div role="alert" className={styles.error}>{error}</div>}

        {/* Role toggle */}
        <div className={styles.roleToggle}>
          <button
            type="button"
            className={`${styles.roleBtn} ${role === 'CONTESTANT' ? styles.roleActive : ''}`}
            onClick={() => setRole('CONTESTANT')}
          >
            <svg className={styles.roleIcon} width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M6 9H4.5a2.5 2.5 0 0 1 0-5H6"/><path d="M18 9h1.5a2.5 2.5 0 0 0 0-5H18"/><path d="M4 22h16"/><path d="M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20.24 7 22"/><path d="M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20.24 17 22"/><path d="M18 2H6v7a6 6 0 0 0 12 0V2Z"/></svg>
            <span className={styles.roleLabel}>Участник</span>
            <span className={styles.roleDesc}>Решаю задачи и контесты</span>
          </button>
          <button
            type="button"
            className={`${styles.roleBtn} ${role === 'ORGANIZER' ? styles.roleActive : ''}`}
            onClick={() => setRole('ORGANIZER')}
          >
            <svg className={styles.roleIcon} width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="6"/><circle cx="12" cy="12" r="2"/></svg>
            <span className={styles.roleLabel}>Организатор</span>
            <span className={styles.roleDesc}>Создаю задачи и контесты</span>
          </button>
        </div>

        <form onSubmit={handleSubmit} className={styles.form}>
          <div className={styles.field}>
            <label htmlFor="RegisterPage-0">Имя пользователя</label>
            <input id="RegisterPage-0" autoComplete="username" value={form.username} onChange={set('username')} placeholder="от 3 до 50 символов" required />
          </div>
          <div className={styles.field}>
            <label htmlFor="RegisterPage-1">Email</label>
            <input id="RegisterPage-1" autoComplete="email" type="email" value={form.email} onChange={set('email')} placeholder="you@example.com" required />
          </div>
          <div className={styles.field}>
            <label htmlFor="RegisterPage-2">Пароль</label>
            <input id="RegisterPage-2" autoComplete="new-password" type="password" value={form.password} onChange={set('password')} placeholder="Минимум 8 символов" required minLength={8} />
          </div>
          <button className="btn btn-primary" style={{ width: '100%' }} disabled={loading}>
            {loading ? <span className="spinner" /> : 'Создать аккаунт'}
          </button>
        </form>
        <p className={styles.footer}>
          Уже есть аккаунт? <Link to={`/login?redirect=${encodeURIComponent(redirect)}`}>Войти</Link>
        </p>
      </div>
    </div>
  )
}
