import styles from './RatingBadge.module.css'

interface Props {
  rating: number
  size?: 'sm' | 'md' | 'lg'
  showRating?: boolean
}

function tierInfo(rating: number) {
  if (rating >= 2000) return { name: 'Мастер',       icon: '👑', color: '#f59e0b' }
  if (rating >= 1500) return { name: 'Эксперт',      icon: '🔥', color: '#8b5cf6' }
  if (rating >= 1000) return { name: 'Продвинутый',  icon: '💫', color: '#3b82f6' }
  if (rating >= 500)  return { name: 'Участник',     icon: '⚡', color: '#22c55e' }
  return                     { name: 'Новичок',      icon: '🔰', color: '#71717a' }
}

export default function RatingBadge({ rating, size = 'md', showRating = true }: Props) {
  const { name, icon, color } = tierInfo(rating)
  return (
    <span className={`${styles.badge} ${styles[size]}`} style={{ '--tier-color': color } as React.CSSProperties}>
      <span className={styles.icon}>{icon}</span>
      <span className={styles.name}>{name}</span>
      {showRating && <span className={styles.rating}>{rating}</span>}
    </span>
  )
}
