import { Link } from 'react-router-dom'
import styles from './LandingPage.module.css'

const FeatureIcons = {
  zap: (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
    </svg>
  ),
  trophy: (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M8 21H5a2 2 0 0 1-2-2v-2a2 2 0 0 1 2-2h3"/><path d="M16 21h3a2 2 0 0 0 2-2v-2a2 2 0 0 0-2-2h-3"/>
      <path d="M12 21V9"/><path d="M9 9h6"/>
      <path d="M7 3a1 1 0 0 0-1 1v1a5 5 0 0 0 6 4.9A5 5 0 0 0 18 5V4a1 1 0 0 0-1-1H7z"/>
    </svg>
  ),
  trending: (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/><polyline points="17 6 23 6 23 12"/>
    </svg>
  ),
  target: (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="6"/><circle cx="12" cy="12" r="2"/>
    </svg>
  ),
  shield: (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
    </svg>
  ),
  code: (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/>
    </svg>
  ),
}

const FEATURES = [
  {
    icon: FeatureIcons.zap,
    title: 'Мгновенная проверка',
    desc: 'Код компилируется и тестируется за секунды. Видишь результат сразу после отправки.',
  },
  {
    icon: FeatureIcons.trophy,
    title: 'Контесты в реальном времени',
    desc: 'Соревнуйся с другими участниками. Таблица результатов обновляется на лету.',
  },
  {
    icon: FeatureIcons.trending,
    title: 'Рейтинговая система',
    desc: 'Решай задачи, зарабатывай рейтинг. Отслеживай прогресс на глобальной доске лидеров.',
  },
  {
    icon: FeatureIcons.target,
    title: 'Задачи разного уровня',
    desc: 'Лёгкие, средние и сложные задачи. Каждая с детальным условием и примерами.',
  },
  {
    icon: FeatureIcons.shield,
    title: 'Безопасное выполнение',
    desc: 'Код запускается в изолированном контейнере с лимитами по времени и памяти.',
  },
  {
    icon: FeatureIcons.code,
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
        <span style={{display:'flex',alignItems:'center',gap:8,justifyContent:'center'}}>
          <svg width="16" height="16" viewBox="0 0 22 22" fill="none">
            <rect x="1" y="1" width="20" height="20" rx="5" fill="url(#footer-logo-grad)" />
            <path d="M14.5 4.5L8 11H13L7.5 17.5" stroke="white" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"/>
            <defs>
              <linearGradient id="footer-logo-grad" x1="1" y1="1" x2="21" y2="21" gradientUnits="userSpaceOnUse">
                <stop stopColor="#8b5cf6"/><stop offset="1" stopColor="#6d28d9"/>
              </linearGradient>
            </defs>
          </svg>
          Stackr — дипломный проект
        </span>
      </footer>
    </div>
  )
}
