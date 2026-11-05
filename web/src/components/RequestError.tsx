export default function RequestError({ message = 'Не удалось загрузить страницу' }: { message?: string }) {
  return <div className="empty-state" role="alert"><h2>{message}</h2><p>Проверьте соединение и повторите попытку.</p><button className="btn btn-secondary" onClick={() => window.location.reload()}>Повторить</button></div>
}
