import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
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

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const { data } = await authApi.login(username, password)
      setTokens(data.accessToken, data.refreshToken)
      const { data: user } = await authApi.getMe()
      setUser(user)
      navigate('/problems')
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
        <div className={styles.logoMark}>⚡ Stackr</div>
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
