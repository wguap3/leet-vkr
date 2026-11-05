import Brand from '../components/Brand'
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
  const target = searchParams.get('redirect') || '/problems'
  const redirect = target.startsWith('/') && !target.startsWith('//') && !target.includes(String.fromCharCode(92)) ? target : '/problems'

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
        <div className={styles.logoMark}><Brand />
        </div>
        <h1 className={styles.title}>Добро пожаловать</h1>
        <p className={styles.subtitle}>Войди в свой аккаунт</p>
        {error && <div role="alert" className={styles.error}>{error}</div>}
        <form onSubmit={handleSubmit} className={styles.form}>
          <div className={styles.field}>
            <label htmlFor="LoginPage-0">Логин</label>
            <input id="LoginPage-0" autoComplete="username" value={username} onChange={e => setUsername(e.target.value)}
              placeholder="username" required autoFocus />
          </div>
          <div className={styles.field}>
            <label htmlFor="LoginPage-1">Пароль</label>
            <input id="LoginPage-1" autoComplete="current-password" type="password" value={password} onChange={e => setPassword(e.target.value)}
              placeholder="••••••••" required />
          </div>
          <button className="btn btn-primary" style={{ width: '100%', marginTop: 4 }} disabled={loading}>
            {loading ? <span className="spinner" /> : 'Войти'}
          </button>
        </form>
        <p className={styles.footer}>
          Нет аккаунта? <Link to={`/register?redirect=${encodeURIComponent(redirect)}`}>Зарегистрироваться</Link>
        </p>
      </div>
    </div>
  )
}
