import { Link } from 'react-router-dom'
import styles from './Roadmap.module.css'

const nodes = [
  ['Сложность алгоритмов', 500, 40],
  ['Массивы, строки и хеширование', 500, 150],
  ['Два указателя', 290, 270], ['Стек и очередь', 710, 270],
  ['Сортировки и бинарный поиск', 150, 400], ['Скользящее окно', 500, 400], ['Связанные списки', 850, 400],
  ['Деревья', 500, 530],
  ['Префиксные деревья', 150, 670], ['Куча и приоритетная очередь', 500, 670], ['Перебор с возвратом', 850, 670],
  ['Интервалы', 150, 820], ['Жадные алгоритмы', 400, 820], ['Графы: обходы и топосортировка', 680, 820], ['Одномерная динамика', 950, 820],
  ['Кратчайшие пути', 150, 970], ['Динамика по состояниям', 950, 970], ['Непересекающиеся множества (DSU)', 500, 970],
  ['Минимальные остовные деревья', 720, 1120],
] as const
const edges = [[0,1],[1,2],[1,3],[2,4],[2,5],[3,6],[4,7],[5,7],[6,7],[7,8],[7,9],[7,10],[9,11],[9,12],[9,13],[10,13],[10,14],[13,15],[13,16],[14,16],[13,17],[17,18]]

export default function LearnPage() {
  return <div className="page-container">
    <header className={styles.header}>
      <div><p className="eyebrow">ВАШ МАРШРУТ ПОДГОТОВКИ</p><h1>Роудмап по алгоритмам</h1>
        <p className="page-description">Сложность, структуры данных, поиск, сортировки, графы и динамическое программирование. Двигайтесь сверху вниз: линии показывают рекомендуемую последовательность изучения.</p></div>
      <Link to="/problems" className="btn btn-outline">К задачам →</Link>
    </header>
    <div className={styles.notice}><span className={styles.dot} /><div><strong>Карта готова · наполнение в разработке</strong><p>Здесь появятся теория и задачи по темам — от Easy до Hard. Пока можно практиковаться в общем каталоге.</p></div></div>
    <section className={styles.map} aria-label="Карта изучения алгоритмов">
      <div className={styles.canvas}>
        <svg className={styles.lines} viewBox="0 0 1140 1240" fill="none" aria-hidden="true">
          <defs><linearGradient id="road-line" x1="570" y1="0" x2="570" y2="1240" gradientUnits="userSpaceOnUse"><stop stopColor="#b6a0ff"/><stop offset="1" stopColor="#67518f"/></linearGradient></defs>
          {edges.map(([a,b]) => { const from=nodes[a], to=nodes[b]; const y=from[2]+90, end=to[2], mid=(y+end)/2; return <path key={`${a}-${b}`} d={`M ${from[1]+70} ${y} C ${from[1]+70} ${mid}, ${to[1]+70} ${mid}, ${to[1]+70} ${end}`} stroke="url(#road-line)" strokeWidth="2" /> })}
        </svg>
        <ol className={styles.topics}>{nodes.map(([title,x,y],i)=><li key={title} className={styles.node} style={{left:`${x/11.4}%`,top:y}}><span className={styles.number}>{String(i+1).padStart(2,'0')}</span><span>{title}</span></li>)}</ol>
      </div>
    </section>
    <p className={styles.footnote}>Порядок — ориентир, а не ограничение. К знакомым темам можно возвращаться в любой момент.</p>
  </div>
}
