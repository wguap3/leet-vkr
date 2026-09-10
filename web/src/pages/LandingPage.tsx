import { Link } from 'react-router-dom'
import styles from './LandingPage.module.css'

const FEATURES = [
  {
    icon: '⚡',
    title: 'Мгновенная проверка',
    desc: 'Код компилируется и тестируется за секунды. Видишь результат сразу после отправки.',
  },
  {
    icon: '🏆',
    title: 'Контесты в реальном времени',
    desc: 'Соревнуйся с другими участниками. Таблица результатов обновляется на лету.',
  },
  {
    icon: '📈',
    title: 'Рейтинговая система',
    desc: 'Решай задачи, зарабатывай рейтинг. Отслеживай прогресс на глобальной доске лидеров.',
  },
  {
    icon: '🎯',
    title: 'Задачи разного уровня',
    desc: 'Лёгкие, средние и сложные задачи. Каждая с детальным условием и примерами.',
  },
  {
    icon: '🔒',
    title: 'Безопасное выполнение',
    desc: 'Код запускается в изолированном контейнере с лимитами по времени и памяти.',
  },
  {
    icon: '✏️',
    title: 'Встроенный редактор',
    desc: 'Monaco Editor с подсветкой синтаксиса Java — как в VS Code, прямо в браузере.',
  },
]

const CODE_EXAMPLE = `import java.util.Scanner;

public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        long max = Long.MIN_VALUE, cur = 0;
        for (int i = 0; i < n; i++) {
            cur += sc.nextInt();
            max = Math.max(max, cur);
            if (cur < 0) cur = 0;
        }
        System.out.println(max); // AC ✓
    }
}`

export default function LandingPage() {
  return (
    <div className={styles.page}>

      {/* ── Hero ── */}
      <section className={styles.hero}>
        <div className={styles.glow} />
        <div className={styles.heroInner}>
          <div className={styles.pill}>
            <span className={styles.pillDot} />
            Открытая платформа для алгоритмических задач
          </div>
          <h1 className={styles.heroTitle}>
            Прокачай алгоритмическое<br />
            <span className={styles.gradient}>мышление</span>
          </h1>
          <p className={styles.heroSub}>
            Stackr — платформа для решения задач по программированию.<br />
            Участвуй в контестах, улучшай рейтинг, побеждай.
          </p>
          <div className={styles.heroCta}>
            <Link to="/register" className="btn btn-primary btn-lg">
              Начать бесплатно →
            </Link>
            <Link to="/problems" className="btn btn-secondary btn-lg">
              Смотреть задачи
            </Link>
          </div>
          <div className={styles.heroStats}>
            <div className={styles.stat}><strong>Java</strong><span>язык</span></div>
            <div className={styles.statDiv} />
            <div className={styles.stat}><strong>3</strong><span>уровня сложности</span></div>
            <div className={styles.statDiv} />
            <div className={styles.stat}><strong>∞</strong><span>задач</span></div>
          </div>
        </div>

        {/* Code preview */}
        <div className={styles.codeCard}>
          <div className={styles.codeHeader}>
            <div className={styles.codeTitle}>Solution.java</div>
            <span className={`badge badge-ac`} style={{ fontSize: 12 }}>AC — 28ms</span>
          </div>
          <pre className={styles.codeBody}><code>{CODE_EXAMPLE}</code></pre>
        </div>
      </section>

      {/* ── Features ── */}
      <section className={styles.features}>
        <div className={styles.sectionInner}>
          <div className={styles.sectionLabel}>Возможности</div>
          <h2 className={styles.sectionTitle}>Всё для продуктивной практики</h2>
          <div className={styles.featureGrid}>
            {FEATURES.map(f => (
              <div key={f.title} className={styles.featureCard}>
                <div className={styles.featureIcon}>{f.icon}</div>
                <h3 className={styles.featureTitle}>{f.title}</h3>
                <p className={styles.featureDesc}>{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── CTA strip ── */}
      <section className={styles.ctaStrip}>
        <div className={styles.ctaInner}>
          <h2 className={styles.ctaTitle}>Готов начать?</h2>
          <p className={styles.ctaSub}>Регистрация бесплатная. Первую задачу можно решить через минуту.</p>
          <Link to="/register" className="btn btn-primary btn-lg">Создать аккаунт</Link>
        </div>
      </section>

      {/* ── Footer ── */}
      <footer className={styles.footer}>
        <span>⚡ Stackr — дипломный проект</span>
      </footer>
    </div>
  )
}
