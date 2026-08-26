import { useEffect, useMemo, useRef, useState } from 'react'
import {
  AlertTriangle,
  Bell,
  CalendarDays,
  ChartNoAxesColumnIncreasing,
  ClipboardCheck,
  Database,
  FileText,
  FolderKanban,
  LayoutDashboard,
  LogOut,
  Moon,
  RefreshCw,
  Search,
  Settings2,
  ShieldCheck,
  Sun,
  Upload,
  UserRound,
  UsersRound,
  X,
} from 'lucide-react'
import { currentLocale } from './i18n.js'

const VIEW_ICONS = {
  dashboard: LayoutDashboard,
  notifications: Bell,
  imports: Upload,
  crud: Database,
  evaluations: ClipboardCheck,
  extensions: RefreshCw,
  grading: ChartNoAxesColumnIncreasing,
  reports: FileText,
  calendar: CalendarDays,
}

export function ViewIcon({ view, size = 18 }) {
  const Icon = VIEW_ICONS[view] || FolderKanban
  return <Icon size={size} aria-hidden="true" />
}

export function MetricIcon({ label }) {
  const value = String(label || '').toLowerCase()
  const Icon = value.includes('utilisateur') || value.includes('équipe')
    ? UsersRound
    : value.includes('projet')
      ? FolderKanban
      : value.includes('phase') || value.includes('deadline')
        ? CalendarDays
        : value.includes('rapport')
          ? FileText
          : value.includes('notification')
            ? Bell
            : value.includes('note') || value.includes('évaluation')
              ? ClipboardCheck
              : ChartNoAxesColumnIncreasing
  return <Icon size={19} aria-hidden="true" />
}

export function ThemeToggle({ theme, setTheme, compact = false }) {
  const dark = theme === 'dark'
  return <button
    type="button"
    className={'theme-toggle ' + (compact ? 'compact' : '')}
    onClick={() => setTheme(dark ? 'light' : 'dark')}
    title={dark ? 'Activer le thème clair' : 'Activer le thème sombre'}
    aria-label={dark ? 'Activer le thème clair' : 'Activer le thème sombre'}
  >
    {dark ? <Sun size={18} /> : <Moon size={18} />}
    {!compact && <span>{dark ? 'Thème clair' : 'Thème sombre'}</span>}
  </button>
}

export function GlobalSearch({ allowedViews, datasets, onNavigate }) {
  const [query, setQuery] = useState('')
  const [activeIndex, setActiveIndex] = useState(0)
  const searchRef = useRef(null)
  const normalized = normalizeSearch(query)
  const results = useMemo(() => {
    if (!normalized) return []
    const allowedIds = new Set(allowedViews.map((view) => view.id))
    const navigation = allowedViews
      .filter((view) => normalizeSearch([view.label, ...(VIEW_SEARCH_ALIASES[view.id] || [])].join(' ')).includes(normalized))
      .map((view) => ({ key: 'view-' + view.id, title: view.label, meta: 'Navigation', view: view.id }))
    const dataResults = SEARCHABLE_DATASETS.flatMap((config) => {
      const view = config.views.find((candidate) => allowedIds.has(candidate)) || 'dashboard'
      return (datasets[config.key] || [])
        .filter((item) => normalizeSearch(JSON.stringify(item)).includes(normalized))
        .slice(0, 4)
        .map((item) => ({
          key: config.key + '-' + item.id,
          title: config.title(item),
          meta: config.meta(item),
          view,
          resourceKey: config.resourceKey,
          query: config.query(item),
          projectId: config.key === 'projects' ? item.id : item.project?.id || item.projectId,
        }))
    })
    return [...navigation, ...dataResults].slice(0, 9)
  }, [allowedViews, datasets, normalized])

  useEffect(() => {
    if (!normalized) return undefined
    const close = (event) => { if (!searchRef.current?.contains(event.target)) setQuery('') }
    document.addEventListener('pointerdown', close)
    return () => document.removeEventListener('pointerdown', close)
  }, [normalized])

  function choose(result) {
    onNavigate(result.view, result)
    setQuery('')
  }

  return <div className="global-search" ref={searchRef}>
    <Search size={18} aria-hidden="true" />
    <input
      value={query}
      onChange={(event) => { setQuery(event.target.value); setActiveIndex(0) }}
      onKeyDown={(event) => {
        if (event.key === 'ArrowDown' && results.length) { event.preventDefault(); setActiveIndex((current) => (current + 1) % results.length) }
        if (event.key === 'ArrowUp' && results.length) { event.preventDefault(); setActiveIndex((current) => (current - 1 + results.length) % results.length) }
        if (event.key === 'Enter' && results[activeIndex]) { event.preventDefault(); choose(results[activeIndex]) }
        if (event.key === 'Escape') setQuery('')
      }}
      placeholder="Rechercher partout…"
      aria-label="Rechercher partout"
      role="combobox"
      aria-expanded={Boolean(normalized)}
      aria-controls="global-search-results"
    />
    {query && <button type="button" onClick={() => setQuery('')} title="Effacer" aria-label="Effacer la recherche"><X size={16} /></button>}
    {normalized && <div className="search-results" id="global-search-results" role="listbox">
      {results.length ? results.map((result, index) => <button type="button" role="option" aria-selected={index === activeIndex} className={index === activeIndex ? 'active' : ''} key={result.key} onMouseEnter={() => setActiveIndex(index)} onClick={() => choose(result)}>
        <span>{result.title}</span><small>{result.meta}</small>
      </button>) : <div className="search-empty">Aucun résultat</div>}
    </div>}
  </div>
}

const VIEW_SEARCH_ALIASES = {
  dashboard: ['dashboard', 'accueil', 'home', 'overview'],
  calendar: ['calendrier', 'calendar', 'phase', 'deadline', 'échéance'],
  notifications: ['notification', 'alerte', 'message'],
  imports: ['import', 'excel', 'classeur', 'initialisation'],
  crud: ['données', 'data', 'utilisateur', 'étudiant', 'projet', 'équipe'],
  evaluations: ['évaluation', 'evaluation', 'fiche', 'notation'],
  extensions: ['prolongation', 'extension', 'échéance'],
  grading: ['note', 'grade', 'résultat', 'consolidation'],
  reports: ['rapport', 'report', 'export', 'matlab'],
}

const SEARCHABLE_DATASETS = [
  { key: 'projects', resourceKey: 'projects', views: ['crud', 'evaluations', 'grading', 'reports', 'dashboard'], title: (item) => item.title || item.projectNumber || 'Projet', meta: (item) => [item.projectNumber, item.track?.code || item.trackCode, 'Projet'].filter(Boolean).join(' · '), query: (item) => item.projectNumber || item.title || '' },
  { key: 'users', resourceKey: 'users', views: ['crud', 'dashboard'], title: (item) => item.fullName || item.email || 'Utilisateur', meta: (item) => [item.role, item.email].filter(Boolean).join(' · '), query: (item) => item.email || item.fullName || '' },
  { key: 'students', resourceKey: 'students', views: ['crud', 'dashboard'], title: (item) => item.fullName || item.studentNumber || 'Étudiant', meta: (item) => [item.studentNumber, item.trackCode, 'Étudiant'].filter(Boolean).join(' · '), query: (item) => item.studentNumber || item.fullName || '' },
  { key: 'evaluators', resourceKey: 'evaluators', views: ['crud', 'dashboard'], title: (item) => item.user?.fullName || item.user?.email || 'Évaluateur', meta: (item) => [item.user?.role, item.department].filter(Boolean).join(' · '), query: (item) => item.user?.email || item.user?.fullName || '' },
  { key: 'tracks', resourceKey: 'tracks', views: ['crud', 'dashboard'], title: (item) => item.name || item.code || 'Filière', meta: (item) => [item.code, 'Filière'].filter(Boolean).join(' · '), query: (item) => item.code || item.name || '' },
  { key: 'teams', resourceKey: 'teams', views: ['crud', 'evaluations', 'dashboard'], title: (item) => item.name || 'Équipe', meta: (item) => [item.project?.projectNumber, item.academicYear, 'Équipe'].filter(Boolean).join(' · '), query: (item) => item.name || item.project?.projectNumber || '' },
  { key: 'phases', views: ['calendar', 'dashboard'], title: (item) => item.name || 'Phase', meta: (item) => [item.type, item.status, 'Phase'].filter(Boolean).join(' · '), query: (item) => item.name || '' },
  { key: 'forms', resourceKey: 'forms', views: ['crud', 'evaluations', 'dashboard'], title: (item) => item.name || 'Fiche', meta: (item) => [item.evaluationType, 'Fiche'].filter(Boolean).join(' · '), query: (item) => item.name || item.evaluationType || '' },
  { key: 'reports', resourceKey: 'reports', views: ['reports', 'crud', 'dashboard'], title: (item) => item.title || item.project?.title || 'Rapport', meta: (item) => [item.status, item.project?.projectNumber, 'Rapport'].filter(Boolean).join(' · '), query: (item) => item.title || item.project?.projectNumber || '' },
]

function normalizeSearch(value) {
  return String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('fr').trim()
}

export function ProfileDrawer({
  open,
  onClose,
  session,
  request,
  notify,
  theme,
  setTheme,
  language,
  setLanguage,
  onLogout,
  roleLabel,
}) {
  const [passwords, setPasswords] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' })
  const [passwordBusy, setPasswordBusy] = useState(false)
  if (!open) return null
  const initials = String(session.fullName || session.email || 'SQU')
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0])
    .join('')
    .toUpperCase()

  async function changePassword(event) {
    event.preventDefault()
    if (passwords.newPassword !== passwords.confirmPassword) {
      notify('Les deux nouveaux mots de passe ne correspondent pas.', 'danger')
      return
    }
    setPasswordBusy(true)
    try {
      await request('/api/auth/change-password', {
        method: 'POST',
        body: JSON.stringify({ currentPassword: passwords.currentPassword, newPassword: passwords.newPassword }),
      })
      setPasswords({ currentPassword: '', newPassword: '', confirmPassword: '' })
      notify('Mot de passe modifié')
    } catch (error) {
      notify(error.message, 'danger')
    } finally {
      setPasswordBusy(false)
    }
  }

  return <div className="drawer-layer" role="presentation">
    <button className="drawer-backdrop" type="button" onClick={onClose} aria-label="Fermer le profil" />
    <aside className="profile-drawer" role="dialog" aria-modal="true" aria-label="Profil et préférences">
      <header><div><span className="eyebrow">Compte connecté</span><h2>Profil et préférences</h2></div><button className="icon-button" type="button" onClick={onClose} title="Fermer" aria-label="Fermer"><X size={19} /></button></header>
      <section className="profile-identity">
        <div className="profile-avatar">{initials}</div>
        <div><strong>{session.fullName || 'Utilisateur SQU'}</strong><span>{session.email}</span><small>{roleLabel}</small></div>
      </section>
      <section className="profile-facts">
        <div><UserRound size={18} /><span>Identifiant</span><strong>{session.universityId || session.userId || '—'}</strong></div>
        <div><ShieldCheck size={18} /><span>Rôle actif</span><strong>{roleLabel}</strong></div>
      </section>
      <section className="preference-section">
        <div className="preference-heading"><Settings2 size={18} /><div><strong>Apparence</strong><span>Ces préférences restent sur cet appareil.</span></div></div>
        <ThemeToggle theme={theme} setTheme={setTheme} />
        <div className="profile-language" data-no-translate>
          <span>Langue</span>
          <div>{['fr', 'en'].map((code) => <button key={code} type="button" className={language === code ? 'active' : ''} onClick={() => setLanguage(code)}>{code.toUpperCase()}</button>)}</div>
        </div>
      </section>
      {session.role === 'INDUSTRY_REPRESENTATIVE' ? <form className="profile-password" onSubmit={changePassword}>
        <div className="preference-heading"><ShieldCheck size={18} /><div><strong>Changer le mot de passe</strong><span>Au moins huit caractères.</span></div></div>
        <input required type="password" autoComplete="current-password" placeholder="Mot de passe actuel" value={passwords.currentPassword} onChange={(event) => setPasswords({ ...passwords, currentPassword: event.target.value })} />
        <input required minLength="8" type="password" autoComplete="new-password" placeholder="Nouveau mot de passe" value={passwords.newPassword} onChange={(event) => setPasswords({ ...passwords, newPassword: event.target.value })} />
        <input required minLength="8" type="password" autoComplete="new-password" placeholder="Confirmer le mot de passe" value={passwords.confirmPassword} onChange={(event) => setPasswords({ ...passwords, confirmPassword: event.target.value })} />
        <button className="soft-button" disabled={passwordBusy}>{passwordBusy ? 'Modification…' : 'Mettre à jour'}</button>
      </form> : <section className="profile-password">
        <div className="preference-heading"><ShieldCheck size={18} /><div><strong>Identité gérée par SQU</strong><span>La connexion et le mot de passe sont administrés par le compte institutionnel SQU.</span></div></div>
      </section>}
      <button type="button" className="danger-action" onClick={onLogout}><LogOut size={18} />Se déconnecter</button>
    </aside>
  </div>
}

export function CalendarView({ phases = [] }) {
  const ordered = [...phases].sort((left, right) => new Date(left.startDate || 0) - new Date(right.startDate || 0))
  const [now] = useState(() => Date.now())

  return <section className="calendar-page page-enter">
    <header className="page-title-block"><div><span className="eyebrow">Planification académique</span><h2>Calendrier FYP</h2><p>Fenêtres d’évaluation, échéances et état actuel des phases configurées.</p></div><div className="title-icon"><CalendarDays size={24} /></div></header>
    {!ordered.length && <div className="empty-state"><CalendarDays size={28} /><h3>Aucune phase planifiée</h3><p>Les phases configurées par l’administration apparaîtront ici.</p></div>}
    <div className="phase-timeline">
      {ordered.map((phase) => {
        const start = phase.startDate ? new Date(phase.startDate).getTime() : null
        const end = phase.deadline ? new Date(phase.deadline).getTime() : null
        const progress = start && end && end > start ? Math.max(0, Math.min(100, ((now - start) / (end - start)) * 100)) : 0
        return <article className="phase-event" key={phase.id}>
          <div className="phase-marker"><span /></div>
          <div className="phase-event-main"><div><span className="eyebrow">{phase.phaseType || 'FYP'}</span><h3>{phase.name}</h3></div><span className={'status-pill ' + String(phase.status || '').toLowerCase()}>{phase.status || 'NOT_STARTED'}</span></div>
          <div className="phase-dates"><span>Début<strong>{formatCalendarDate(phase.startDate)}</strong></span><span>Échéance<strong>{formatCalendarDate(phase.deadline)}</strong></span><span>Année académique<strong>{phase.academicYear || '—'}</strong></span></div>
          <div className="phase-progress"><span style={{ width: progress + '%' }} /></div>
        </article>
      })}
    </div>
  </section>
}

export function AppSkeleton() {
  return <div className="app-skeleton" aria-label="Chargement">
    <div className="skeleton-line wide" />
    <div className="skeleton-line medium" />
    <div className="skeleton-metrics">{[0, 1, 2, 3].map((item) => <div key={item} />)}</div>
    <div className="skeleton-grid"><div /><div /></div>
  </div>
}

export function ErrorState({ message, onRetry, notFound = false }) {
  return <section className="error-state page-enter">
    <div className="error-code">{notFound ? '404' : '500'}</div>
    <AlertTriangle size={28} />
    <h2>{notFound ? 'Cette page n’existe pas' : 'Impossible de charger cet espace'}</h2>
    <p>{message || 'Une erreur inattendue est survenue. Réessayez dans quelques instants.'}</p>
    {onRetry && <button className="primary-action" type="button" onClick={onRetry}><RefreshCw size={17} />Réessayer</button>}
  </section>
}

function formatCalendarDate(value) {
  if (!value) return 'Non définie'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return new Intl.DateTimeFormat(currentLocale(), { dateStyle: 'medium', timeStyle: 'short' }).format(date)
}
