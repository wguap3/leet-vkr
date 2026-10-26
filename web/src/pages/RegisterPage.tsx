import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
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
      navigate('/problems')
    } catch (err: any) {
      if (!err.response) {
        setError('Нет связи с сервером. Убедись, что бэкенд запущен на порту 8080.')
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
        <div className={styles.logoMark}><svg width="20" height="20" viewBox="0 0 22 22" fill="none" xmlns="http://www.w3.org/2000/svg" style={{display:'inline-block',verticalAlign:'middle',marginRight:6}}>
            <rect x="1" y="1" width="20" height="20" rx="5" fill="url(#reg-logo-grad)" />
            <path d="M14.5 4.5L8 11H13L7.5 17.5" stroke="white" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"/>
            <defs>
              <linearGradient id="reg-logo-grad" x1="1" y1="1" x2="21" y2="21" gradientUnits="userSpaceOnUse">
                <stop stopColor="#8b5cf6"/>
                <stop offset="1" stopColor="#6d28d9"/>
              </linearGradient>
            </defs>
          </svg>
          Stackr</div>
        <h1 className={styles.title}>Регистрация</h1>
        <p className={styles.subtitle}>Создай аккаунт и начни решать задачи</p>
        {error && <div className={styles.error}>{error}</div>}

        {/* Role toggle */}
        <div className={styles.roleToggle}>
          <button
            type="button"
            className={`${styles.roleBtn} ${role === 'CONTESTANT' ? styles.roleActive : ''}`}
            onClick={() => setRole('CONTESTANT')}
          >
            <span className={styles.roleIcon}>🏆</span>
            <span className={styles.roleLabel}>Участник</span>
            <span className={styles.roleDesc}>Решаю задачи и контесты</span>
          </button>
          <button
            type="button"
            className={`${styles.roleBtn} ${role === 'ORGANIZER' ? styles.roleActive : ''}`}
            onClick={() => setRole('ORGANIZER')}
          >
            <span className={styles.roleIcon}>🎯</span>
            <span className={styles.roleLabel}>Организатор</span>
            <span className={styles.roleDesc}>Создаю контесты</span>
          </button>
        </div>

        <form onSubmit={handleSubmit} className={styles.form}>
          <div className={styles.field}>
            <label>Имя пользователя</label>
            <input value={form.username} onChange={set('username')} placeholder="от 3 до 50 символов" required />
          </div>
          <div className={styles.field}>
            <label>Email</label>
            <input type="email" value={form.email} onChange={set('email')} placeholder="you@example.com" required />
          </div>
          <div className={styles.field}>
            <label>Пароль</label>
            <input type="password" value={form.password} onChange={set('password')} placeholder="Минимум 8 символов" required minLength={8} />
          </div>
          <button className="btn btn-primary" style={{ width: '100%' }} disabled={loading}>
            {loading ? <span className="spinner" /> : 'Создать аккаунт'}
          </button>
        </form>
        <p className={styles.footer}>
          Уже есть аккаунт? <Link to="/login">Войти</Link>
        </p>
      </div>
    </div>
  )
}
