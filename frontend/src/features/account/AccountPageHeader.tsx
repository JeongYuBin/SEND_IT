import { Link } from 'react-router-dom'

export function AccountPageHeader({ eyebrow, title, description, backTo = '/profile', backLabel = '내 정보' }: {
  eyebrow: string; title: string; description: string; backTo?: string; backLabel?: string
}) {
  return <header className="account-header">
    <Link className="account-back-link" to={backTo}><span aria-hidden="true">‹</span>{backLabel}</Link>
    <span className="eyebrow">{eyebrow}</span>
    <h1>{title}</h1>
    <p>{description}</p>
  </header>
}

export function AccountIcon({ name }: { name: 'bell' | 'settings' | 'lock' | 'logout' | 'delete' }) {
  return <svg className="account-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    {name === 'bell' && <><path d="M18 8a6 6 0 0 0-12 0c0 6-3 6-3 9h18c0-3-3-3-3-9Z" /><path d="M10 21h4" /></>}
    {name === 'settings' && <><path d="M4 6h16M4 12h16M4 18h16" /><path d="M8 4v4M16 10v4M10 16v4" /></>}
    {name === 'lock' && <><rect x="5" y="10" width="14" height="11" rx="3" /><path d="M8 10V7a4 4 0 0 1 8 0v3M12 14v3" /></>}
    {name === 'logout' && <><path d="M10 4H5v16h5M9 12h12m-4-4 4 4-4 4" /></>}
    {name === 'delete' && <><path d="M4 6h16M9 6V3h6v3M6 6l1 15h10l1-15M10 10v7M14 10v7" /></>}
  </svg>
}
