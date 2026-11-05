import { Component, type ReactNode } from 'react'
export default class ErrorBoundary extends Component<{ children: ReactNode }, { failed: boolean }> {
  state = { failed: false }
  static getDerivedStateFromError() { return { failed: true } }
  render() {
    return this.state.failed ? <div className="empty-state" role="alert"><h1>Не удалось открыть страницу</h1><p>Сохранённый код останется на устройстве. Обновите страницу и попробуйте снова.</p><button className="btn btn-primary" onClick={() => window.location.reload()}>Обновить страницу</button></div> : this.props.children
  }
}
