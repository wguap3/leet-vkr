import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import * as authApi from '../api/auth'
import { useAuthStore } from '../store/authStore'
import styles from './AuthPage.module.css'

export default function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { setTokens, setUser } = useAuthStore()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const redirect = searchParams.get('redirect') || '/problems'

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const { data } = await authApi.login(username, password)
      setTokens(data.accessToken, data.refreshToken)
      const { data: user } = await authApi.getMe()
      setUser(user)
      navigate(redirect)
    } catch (err: any) {
      const msg = err.response?.data?.message || err.response?.data?.error || 'Неверный логин или пароль'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className={styles.wrapper}>
      <div className={`card ${styles.card}`}>
        <div className={styles.logoMark}>
          <svg width="20" height="20" viewBox="0 0 22 22" fill="none" xmlns="http://www.w3.org/2000/svg" style={{display:'inline-block',verticalAlign:'middle',marginRight:6}}>
            <rect x="1" y="1" width="20" height="20" rx="5" fill="url(#login-logo-grad)" />
            <path d="M14.5 4.5L8 11H13L7.5 17.5" stroke="white" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"/>
            <defs>
              <linearGradient id="login-logo-grad" x1="1" y1="1" x2="21" y2="21" gradientUnits="userSpaceOnUse">
                <stop stopColor="#8b5cf6"/>
                <stop offset="1" stopColor="#6d28d9"/>
              </linearGradient>
            </defs>
          </svg>
          Stackr
        </div>
        <h1 className={styles.title}>Добро пожаловать</h1>
        <p className={styles.subtitle}>Войди в свой аккаунт</p>
        {error && <div className={styles.error}>{error}</div>}
        <form onSubmit={handleSubmit} className={styles.form}>
          <div className={styles.field}>
            <label>Логин</label>
            <input value={username} onChange={e => setUsername(e.target.value)}
              placeholder="username" required autoFocus />
          </div>
          <div className={styles.field}>
            <label>Пароль</label>
            <input type="password" value={password} onChange={e => setPassword(e.target.value)}
              placeholder="••••••••" required />
          </div>
          <button className="btn btn-primary" style={{ width: '100%', marginTop: 4 }} disabled={loading}>
            {loading ? <span className="spinner" /> : 'Войти'}
          </button>
        </form>
        <p className={styles.footer}>
          Нет аккаунта? <Link to="/register">Зарегистрироваться</Link>
        </p>
      </div>
    </div>
  )
}
