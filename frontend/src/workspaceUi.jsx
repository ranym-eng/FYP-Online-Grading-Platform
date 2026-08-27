import { useEffect, useMemo, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
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
  Pencil,
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
  const Icon = value.includes('utilisateur') || value.includes('équipe') || value.includes('user') || value.includes('team')
    ? UsersRound
    : value.includes('projet') || value.includes('project')
      ? FolderKanban
      : value.includes('phase') || value.includes('deadline')
        ? CalendarDays
        : value.includes('rapport') || value.includes('report')
          ? FileText
          : value.includes('notification')
            ? Bell
            : value.includes('note') || value.includes('évaluation') || value.includes('grade') || value.includes('evaluation')
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
    title={dark ? 'Use light theme' : 'Use dark theme'}
    aria-label={dark ? 'Use light theme' : 'Use dark theme'}
  >
    {dark ? <Sun size={18} /> : <Moon size={18} />}
    {!compact && <span>{dark ? 'Light theme' : 'Dark theme'}</span>}
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
      placeholder="Search everywhere…"
      aria-label="Search everywhere"
      role="combobox"
      aria-expanded={Boolean(normalized)}
      aria-controls="global-search-results"
    />
    {query && <button type="button" onClick={() => setQuery('')} title="Clear" aria-label="Clear search"><X size={16} /></button>}
    {normalized && <div className="search-results" id="global-search-results" role="listbox">
      {results.length ? results.map((result, index) => <button type="button" role="option" aria-selected={index === activeIndex} className={index === activeIndex ? 'active' : ''} key={result.key} onMouseEnter={() => setActiveIndex(index)} onClick={() => choose(result)}>
        <span>{result.title}</span><small>{result.meta}</small>
      </button>) : <div className="search-empty">No results</div>}
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
  { key: 'projects', resourceKey: 'projects', views: ['crud', 'evaluations', 'grading', 'reports', 'dashboard'], title: (item) => item.title || item.projectNumber || 'Project', meta: (item) => [item.projectNumber, item.track?.code || item.trackCode, 'Project'].filter(Boolean).join(' · '), query: (item) => item.projectNumber || item.title || '' },
  { key: 'users', resourceKey: 'users', views: ['crud', 'dashboard'], title: (item) => item.fullName || item.email || 'User', meta: (item) => [item.role, item.email].filter(Boolean).join(' · '), query: (item) => item.email || item.fullName || '' },
  { key: 'students', resourceKey: 'students', views: ['crud', 'dashboard'], title: (item) => item.fullName || item.studentNumber || 'Student', meta: (item) => [item.studentNumber, item.trackCode, 'Student'].filter(Boolean).join(' · '), query: (item) => item.studentNumber || item.fullName || '' },
  { key: 'evaluators', resourceKey: 'evaluators', views: ['crud', 'dashboard'], title: (item) => item.user?.fullName || item.user?.email || 'Evaluator', meta: (item) => [item.user?.role, item.department].filter(Boolean).join(' · '), query: (item) => item.user?.email || item.user?.fullName || '' },
  { key: 'tracks', resourceKey: 'tracks', views: ['crud', 'dashboard'], title: (item) => item.name || item.code || 'Track', meta: (item) => [item.code, 'Track'].filter(Boolean).join(' · '), query: (item) => item.code || item.name || '' },
  { key: 'teams', resourceKey: 'teams', views: ['crud', 'evaluations', 'dashboard'], title: (item) => item.name || 'Team', meta: (item) => [item.project?.projectNumber, item.academicYear, 'Team'].filter(Boolean).join(' · '), query: (item) => item.name || item.project?.projectNumber || '' },
  { key: 'phases', views: ['calendar', 'dashboard'], title: (item) => item.name || 'Phase', meta: (item) => [item.type, item.status, 'Phase'].filter(Boolean).join(' · '), query: (item) => item.name || '' },
  { key: 'forms', resourceKey: 'forms', views: ['crud', 'evaluations', 'dashboard'], title: (item) => item.name || 'Form', meta: (item) => [item.evaluationType, 'Form'].filter(Boolean).join(' · '), query: (item) => item.name || item.evaluationType || '' },
  { key: 'reports', resourceKey: 'reports', views: ['reports', 'crud', 'dashboard'], title: (item) => item.title || item.project?.title || 'Report', meta: (item) => [item.status, item.project?.projectNumber, 'Report'].filter(Boolean).join(' · '), query: (item) => item.title || item.project?.projectNumber || '' },
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
      notify('The two new passwords do not match.', 'danger')
      return
    }
    setPasswordBusy(true)
    try {
      await request('/api/auth/change-password', {
        method: 'POST',
        body: JSON.stringify({ currentPassword: passwords.currentPassword, newPassword: passwords.newPassword }),
      })
      setPasswords({ currentPassword: '', newPassword: '', confirmPassword: '' })
      notify('Password changed')
    } catch (error) {
      notify(error.message, 'danger')
    } finally {
      setPasswordBusy(false)
    }
  }

  return <div className="drawer-layer" role="presentation">
    <button className="drawer-backdrop" type="button" onClick={onClose} aria-label="Close profile" />
    <aside className="profile-drawer" role="dialog" aria-modal="true" aria-label="Profile and preferences">
      <header><div><span className="eyebrow">Signed-in account</span><h2>Profile and preferences</h2></div><button className="icon-button" type="button" onClick={onClose} title="Close" aria-label="Close"><X size={19} /></button></header>
      <section className="profile-identity">
        <div className="profile-avatar">{initials}</div>
        <div><strong>{session.fullName || 'SQU user'}</strong><span>{session.email}</span><small>{roleLabel}</small></div>
      </section>
      <section className="profile-facts">
        <div><UserRound size={18} /><span>Identifier</span><strong>{session.universityId || session.userId || '—'}</strong></div>
        <div><ShieldCheck size={18} /><span>Active role</span><strong>{roleLabel}</strong></div>
      </section>
      <section className="preference-section">
        <div className="preference-heading"><Settings2 size={18} /><div><strong>Appearance</strong><span>These preferences remain on this device.</span></div></div>
        <ThemeToggle theme={theme} setTheme={setTheme} />
      </section>
      {session.role === 'INDUSTRY_REPRESENTATIVE' ? <form className="profile-password" onSubmit={changePassword}>
        <div className="preference-heading"><ShieldCheck size={18} /><div><strong>Change password</strong><span>At least eight characters.</span></div></div>
        <input required type="password" autoComplete="current-password" placeholder="Current password" value={passwords.currentPassword} onChange={(event) => setPasswords({ ...passwords, currentPassword: event.target.value })} />
        <input required minLength="8" type="password" autoComplete="new-password" placeholder="New password" value={passwords.newPassword} onChange={(event) => setPasswords({ ...passwords, newPassword: event.target.value })} />
        <input required minLength="8" type="password" autoComplete="new-password" placeholder="Confirm password" value={passwords.confirmPassword} onChange={(event) => setPasswords({ ...passwords, confirmPassword: event.target.value })} />
        <button className="soft-button" disabled={passwordBusy}>{passwordBusy ? 'Updating…' : 'Update'}</button>
      </form> : <section className="profile-password">
        <div className="preference-heading"><ShieldCheck size={18} /><div><strong>Identity managed by SQU</strong><span>Sign-in and password are managed through the institutional SQU account.</span></div></div>
      </section>}
      <button type="button" className="danger-action" onClick={onLogout}><LogOut size={18} />Sign out</button>
    </aside>
  </div>
}

export function CalendarView({ phases = [], canEdit = false, onUpdatePhase }) {
  const ordered = [...phases].sort((left, right) => new Date(left.startDate || 0) - new Date(right.startDate || 0))
  const [now] = useState(() => Date.now())
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!editing) return undefined
    const close = (event) => { if (event.key === 'Escape' && !saving) setEditing(null) }
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    window.addEventListener('keydown', close)
    return () => {
      document.body.style.overflow = previousOverflow
      window.removeEventListener('keydown', close)
    }
  }, [editing, saving])

  function editPhase(phase) {
    setEditing(phase)
    setForm({
      name: phase.name || '',
      type: phase.type || phase.phaseType || 'PHASE_I',
      academicYear: phase.academicYear || '',
      startDate: toDateTimeLocal(phase.startDate),
      deadline: toDateTimeLocal(phase.deadline),
      status: phase.status || 'NOT_STARTED',
    })
  }

  async function savePhase(event) {
    event.preventDefault()
    if (!editing || !form || !onUpdatePhase) return
    setSaving(true)
    try {
      await onUpdatePhase(editing.id, form)
      setEditing(null)
      setForm(null)
    } finally {
      setSaving(false)
    }
  }

  return <section className="calendar-page page-enter">
    <header className="page-title-block"><div><span className="eyebrow">Academic planning</span><h2>FYP calendar</h2><p>Evaluation windows, deadlines and the current status of configured phases.</p></div><div className="title-icon"><CalendarDays size={24} /></div></header>
    {!ordered.length && <div className="empty-state"><CalendarDays size={28} /><h3>No scheduled phase</h3><p>Phases configured by the administration will appear here.</p></div>}
    <div className="phase-timeline">
      {ordered.map((phase) => {
        const start = phase.startDate ? new Date(phase.startDate).getTime() : null
        const end = phase.deadline ? new Date(phase.deadline).getTime() : null
        const progress = start && end && end > start ? Math.max(0, Math.min(100, ((now - start) / (end - start)) * 100)) : 0
        return <article className="phase-event" key={phase.id}>
          <div className="phase-marker"><span /></div>
          <div className="phase-event-main"><div><span className="eyebrow">{phase.type || phase.phaseType || 'FYP'}</span><h3>{phase.name}</h3></div><div className="phase-event-actions"><span className={'status-pill ' + String(phase.status || '').toLowerCase()}>{phase.status || 'NOT_STARTED'}</span>{canEdit && <button type="button" className="icon-button phase-edit-button" aria-label={'Edit ' + phase.name} title="Edit phase and deadline" onClick={() => editPhase(phase)}><Pencil size={17} /></button>}</div></div>
          <div className="phase-dates"><span>Start<strong>{formatCalendarDate(phase.startDate)}</strong></span><span>Deadline<strong>{formatCalendarDate(phase.deadline)}</strong></span><span>Academic year<strong>{phase.academicYear || '—'}</strong></span></div>
          <div className="phase-progress"><span style={{ width: progress + '%' }} /></div>
        </article>
      })}
    </div>
    {editing && form && createPortal(<div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget && !saving) setEditing(null) }}><section className="app-dialog" role="dialog" aria-modal="true" aria-label="Edit phase and deadline"><header><h2>Edit phase and deadline</h2><button type="button" className="modal-close" onClick={() => setEditing(null)} aria-label="Close" disabled={saving}><X size={18} /></button></header><div className="app-dialog-body"><form className="stack-form compact dialog-form" onSubmit={savePhase}>
      <label className="field"><span>Phase name</span><input required value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></label>
      <div className="form-grid two"><label className="field"><span>Phase type</span><select value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value })}><option value="PHASE_I">FYP I</option><option value="PHASE_II">FYP II</option></select></label><label className="field"><span>Academic year</span><input required value={form.academicYear} onChange={(event) => setForm({ ...form, academicYear: event.target.value })} /></label></div>
      <div className="form-grid two"><label className="field"><span>Start date</span><input required type="datetime-local" value={form.startDate} onChange={(event) => setForm({ ...form, startDate: event.target.value })} /></label><label className="field"><span>Deadline</span><input required type="datetime-local" min={form.startDate || undefined} value={form.deadline} onChange={(event) => setForm({ ...form, deadline: event.target.value })} /></label></div>
      <label className="field"><span>Status</span><select value={form.status} onChange={(event) => setForm({ ...form, status: event.target.value })}><option value="NOT_STARTED">Not started</option><option value="OPEN">Open</option><option value="CLOSED">Closed</option><option value="ARCHIVED">Archived</option></select></label>
      <div className="dialog-actions"><button type="button" className="ghost-button" onClick={() => setEditing(null)} disabled={saving}>Cancel</button><button className="primary-action" disabled={saving}>{saving ? 'Saving…' : 'Save changes'}</button></div>
    </form></div></section></div>, document.body)}
  </section>
}

export function AppSkeleton() {
  return <div className="app-skeleton" aria-label="Loading">
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
    <h2>{notFound ? 'This page does not exist' : 'Unable to load this workspace'}</h2>
    <p>{message || 'An unexpected error occurred. Please try again shortly.'}</p>
    {onRetry && <button className="primary-action" type="button" onClick={onRetry}><RefreshCw size={17} />Try again</button>}
  </section>
}

function formatCalendarDate(value) {
  if (!value) return 'Not defined'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return new Intl.DateTimeFormat(currentLocale(), { dateStyle: 'medium', timeStyle: 'short' }).format(date)
}

function toDateTimeLocal(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).slice(0, 16)
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
  return local.toISOString().slice(0, 16)
}
