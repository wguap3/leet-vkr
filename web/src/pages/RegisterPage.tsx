import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import * as authApi from '../api/auth'
import { useAuthStore } from '../store/authStore'
import styles from './AuthPage.module.css'

export default function RegisterPage() {
  const [form, setForm] = useState({ username: '', email: '', password: '' })
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
      await authApi.register(form.username, form.email, form.password)
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
        <div className={styles.logoMark}>⚡ Stackr</div>
        <h1 className={styles.title}>Регистрация</h1>
        <p className={styles.subtitle}>Создай аккаунт и начни решать задачи</p>
        {error && <div className={styles.error}>{error}</div>}
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
