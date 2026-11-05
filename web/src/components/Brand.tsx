import styles from './Brand.module.css'

/** Concept 13: two offset corners; vector geometry stays crisp at small sizes. */
export function BrandMark({ size = 28 }: { size?: number }) {
  return <svg width={size} height={size} viewBox="0 0 32 32" fill="none" aria-hidden="true" focusable="false">
    <path d="M11 2H30V21H21V11H11Z" fill="#A78BFA" />
    <path d="M2 13H11V23H21V30H2Z" fill="currentColor" />
  </svg>
}

export default function Brand() {
  return <span className={styles.brand}><BrandMark /><span>stackr</span></span>
}
