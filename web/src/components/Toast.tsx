import { useEffect, useState } from 'react'
export function notify(message: string) { window.dispatchEvent(new CustomEvent('stackr-notice', { detail: message })) }
export default function Toast() {
  const [message, setMessage] = useState('')
  useEffect(() => {
    let timer: ReturnType<typeof setTimeout>
    const show = (event: Event) => { setMessage((event as CustomEvent<string>).detail); clearTimeout(timer); timer = setTimeout(() => setMessage(''), 7000) }
    window.addEventListener('stackr-notice', show)
    return () => { window.removeEventListener('stackr-notice', show); clearTimeout(timer) }
  }, [])
  return message ? <div className="toast" role="alert"><span>{message}</span><button aria-label="Закрыть уведомление" onClick={() => setMessage('')}>×</button></div> : null
}
