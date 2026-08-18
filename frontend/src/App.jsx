import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { ArrowRight, Bell, CheckCheck, CheckCircle2, ChevronRight, CircleAlert, Download, Eye, EyeOff, FileSpreadsheet, LogOut, Mail, MailOpen, Menu, PanelLeftClose, PanelLeftOpen, Pencil, RefreshCw, ShieldCheck, Trash2, X } from 'lucide-react'
import squLogo from './assets/Sultan_Qaboos_University_Logo.png'
import squMark from './assets/sultan-qaboos-university-logo-png_seeklogo-271991.png'
import { apiRequest, downloadFile, itemName, pretty, unwrapList } from './api.js'
import { EVALUATION_TYPES, ROLES, actorTemplates, resourceConfigs, views } from './config.js'
import { SCORING_TEMPLATES, calculateTemplate, normalizeScore, performanceBand, scoreKey, sectionAverage } from './gradingTemplates.js'
import { currentLocale, getInitialLanguage, setLanguagePreference, translateText, useAutoTranslate } from './i18n.js'
import { AppSkeleton, CalendarView, ErrorState, GlobalSearch, MetricIcon, ProfileDrawer, ThemeToggle, ViewIcon } from './workspaceUi.jsx'
import './App.css'
import './design-system.css'

const seedProjectId = '50000000-0000-0000-0000-000000000001'
const homeViewByRole = {
  ADMIN: 'dashboard',
  SUPERVISOR: 'dashboard',
  FACULTY_EVALUATOR: 'dashboard',
  INDUSTRY_REPRESENTATIVE: 'dashboard',
  COORDINATOR: 'dashboard',
}
const primarySidebarViews = new Set(['dashboard', 'calendar', 'notifications'])

function normalizeRole(role) {
  return ROLES.includes(role) ? role : null
}

function homeViewForRole(role) {
  return homeViewByRole[normalizeRole(role)] || 'dashboard'
}

function phaseTypeForEvaluation(evaluationType) {
  if (!evaluationType) return null
  return ['SUPERVISOR_PHASE_I', 'REPORT_PHASE_I', 'ORAL_PHASE_I'].includes(evaluationType)
    ? 'PHASE_I'
    : 'PHASE_II'
}

function initialTheme() {
  const saved = localStorage.getItem('fyp-theme')
  if (saved === 'light' || saved === 'dark') return saved
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

function initialSidebarCollapsed() {
  return localStorage.getItem('fyp-sidebar-collapsed') === 'true'
}

function normalizeSession(raw, fallbackRole = null) {
  const session = raw?.data || raw || {}
  const role = normalizeRole(session.role || fallbackRole)
  if (!role) throw new Error('This account role is not allowed to access the platform')
  return { ...session, role }
}

function readStoredSession() {
  try {
    const session = JSON.parse(localStorage.getItem('fyp-session') || 'null')
    return session && normalizeRole(session.role) ? session : null
  } catch {
    return null
  }
}

function initialForm(fields = []) {
  return fields.reduce((acc, field) => ({ ...acc, [field.name]: field.defaultValue ?? (field.type === 'checkbox' ? false : '') }), {})
}

function serialize(form, fields = []) {
  const data = {}
  fields.forEach((field) => {
    let value = form[field.name]
    if (field.type === 'number') value = value === '' ? 0 : Number(value)
    if (field.type === 'checkbox') value = Boolean(value)
    if (field.type === 'multiData') value = Array.isArray(value) ? value : String(value || '').split(',').map((x) => x.trim()).filter(Boolean)
    if (field.type === 'datetime-local' && value) value = new Date(value).toISOString().slice(0, 19)
    data[field.name] = value
  })
  return data
}

function App() {
  const [session, setSession] = useState(readStoredSession)
  const [activeView, setActiveView] = useState(() => homeViewForRole(session?.role))
  const [language, setLanguage] = useState(getInitialLanguage)
  const [theme, setTheme] = useState(initialTheme)
  const [toast, setToast] = useState(null)
  useAutoTranslate(language)

  useEffect(() => {
    if (session) localStorage.setItem('fyp-session', JSON.stringify(session))
    else localStorage.removeItem('fyp-session')
  }, [session])

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    localStorage.setItem('fyp-theme', theme)
  }, [theme])

  const notify = useCallback((message, type = 'success') => {
    setToast({ message, type })
    window.clearTimeout(window.__toast)
    window.__toast = window.setTimeout(() => setToast(null), 4200)
  }, [])

  useEffect(() => {
    if (!session?.token) return undefined
    let active = true
    apiRequest('/api/auth/validate-token', {}, session.token).catch((error) => {
      if (!active || (error.status !== 401 && error.status !== 403)) return
      setSession(null)
      notify('Votre session a expiré. Reconnectez-vous.', 'danger')
    })
    return () => { active = false }
  }, [notify, session?.token])

  function openWorkspace(rawSession, fallbackRole) {
    const next = normalizeSession(rawSession, fallbackRole)
    setSession(next)
    setActiveView(homeViewForRole(next.role))
    notify('Bienvenue dans votre espace ' + pretty(next.role))
  }

  if (!session) return <AuthScreen onSession={openWorkspace} notify={notify} toast={toast} language={language} setLanguage={setLanguage} theme={theme} setTheme={setTheme} />
  return <Shell session={session} activeView={activeView} setActiveView={setActiveView} onLogout={() => setSession(null)} notify={notify} toast={toast} language={language} setLanguage={setLanguage} theme={theme} setTheme={setTheme} />
}

function LanguageSwitcher({ language, setLanguage }) {
  return <div className="language-switcher" data-no-translate aria-label="Language">
    {['fr', 'en'].map((code) => <button key={code} type="button" className={language === code ? 'active' : ''} onClick={() => { setLanguagePreference(code); setLanguage(code) }} aria-pressed={language === code}>{code.toUpperCase()}</button>)}
  </div>
}

function AuthScreen({ onSession, notify, toast, language, setLanguage, theme, setTheme }) {
  const [busy, setBusy] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  const [mode, setMode] = useState('login')
  const [loginForm, setLoginForm] = useState({ email: 'admin@squ.edu.om', password: 'Admin@123' })
  const [recovery, setRecovery] = useState({ email: '', token: '', newPassword: '' })

  async function login(event) {
    event.preventDefault()
    setBusy(true)
    try {
      const result = await apiRequest('/api/auth/login', { method: 'POST', body: JSON.stringify(loginForm) })
      onSession(result.data)
    } catch (error) {
      notify(error.message, 'danger')
    } finally {
      setBusy(false)
    }
  }

  async function requestReset(event) {
    event.preventDefault()
    setBusy(true)
    try {
      await apiRequest('/api/auth/forgot-password', { method: 'POST', body: JSON.stringify({ email: recovery.email }) })
      setMode('reset')
      notify('Un jeton de réinitialisation a été envoyé. En local, consultez Mailpit.')
    } catch (error) {
      notify(error.message, 'danger')
    } finally {
      setBusy(false)
    }
  }

  async function resetPassword(event) {
    event.preventDefault()
    setBusy(true)
    try {
      await apiRequest('/api/auth/reset-password', { method: 'POST', body: JSON.stringify({ token: recovery.token, newPassword: recovery.newPassword }) })
      setLoginForm({ email: recovery.email, password: '' })
      setRecovery({ email: '', token: '', newPassword: '' })
      setMode('login')
      notify('Mot de passe réinitialisé. Vous pouvez vous connecter.')
    } catch (error) {
      notify(error.message, 'danger')
    } finally {
      setBusy(false)
    }
  }

  const heading = mode === 'login' ? 'Bienvenue' : mode === 'forgot' ? 'Récupérer le compte' : 'Nouveau mot de passe'
  const description = mode === 'login'
    ? 'Connectez-vous avec le compte attribué par l’administration FYP.'
    : mode === 'forgot'
      ? 'Saisissez l’adresse du compte importé par l’administration.'
      : 'Saisissez le jeton reçu par e-mail et choisissez un nouveau mot de passe.'

  return <main className="auth-screen">
    <section className="auth-visual">
      <LogoLockup />
      <div className="auth-copy page-enter">
        <div className="auth-kicker"><ShieldCheck size={17} /><span>Département de génie électrique et informatique</span></div>
        <h1>Final Year<br />Grading</h1>
        <p>Une plateforme institutionnelle pour orchestrer les projets, les jurys et les évaluations FYP I et FYP II avec précision.</p>
        <div className="auth-proof"><span><strong>2</strong> phases académiques</span><span><strong>7</strong> grilles officielles</span><span><strong>5</strong> espaces sécurisés</span></div>
      </div>
      <div className="auth-photo-credit"><span>Exposition annuelle des projets FYP</span><strong>Sultan Qaboos University</strong></div>
    </section>
    <div className="auth-panel-shell">
      <section className="auth-panel page-enter">
        <div className="auth-panel-tools"><ThemeToggle theme={theme} setTheme={setTheme} compact /><LanguageSwitcher language={language} setLanguage={setLanguage} /></div>
        <div className="brand-badge"><img src={squMark} alt="SQU" /><div><span>Portail académique sécurisé</span><small>College of Engineering</small></div></div>
        <form className="stack-form auth-form" onSubmit={mode === 'login' ? login : mode === 'forgot' ? requestReset : resetPassword}>
          <div className="auth-form-heading"><span className="eyebrow">Accès institutionnel</span><h2>{heading}</h2><p>{description}</p></div>
          {mode === 'login' && <><AuthField icon={Mail} label="Adresse e-mail" type="email" value={loginForm.email} onChange={(email) => setLoginForm({ ...loginForm, email })} autoComplete="username" /><AuthField icon={ShieldCheck} label="Mot de passe" type={showPassword ? 'text' : 'password'} value={loginForm.password} onChange={(password) => setLoginForm({ ...loginForm, password })} autoComplete="current-password" action={<button type="button" onClick={() => setShowPassword((value) => !value)} title={showPassword ? 'Masquer le mot de passe' : 'Afficher le mot de passe'} aria-label={showPassword ? 'Masquer le mot de passe' : 'Afficher le mot de passe'}>{showPassword ? <EyeOff size={18} /> : <Eye size={18} />}</button>} /></>}
          {mode === 'forgot' && <AuthField icon={Mail} label="Adresse e-mail" type="email" value={recovery.email} onChange={(email) => setRecovery({ ...recovery, email })} autoComplete="email" />}
          {mode === 'reset' && <><AuthField icon={ShieldCheck} label="Jeton reçu" type="text" value={recovery.token} onChange={(token) => setRecovery({ ...recovery, token })} autoComplete="one-time-code" /><AuthField icon={ShieldCheck} label="Nouveau mot de passe" type="password" minLength="8" value={recovery.newPassword} onChange={(newPassword) => setRecovery({ ...recovery, newPassword })} autoComplete="new-password" /></>}
          <button className="primary-action auth-submit" disabled={busy}>{busy ? <><span className="button-spinner" />Traitement…</> : <>{mode === 'login' ? 'Se connecter' : mode === 'forgot' ? 'Envoyer le jeton' : 'Changer le mot de passe'}<ArrowRight size={18} /></>}</button>
          {mode === 'login' ? <button className="auth-link" type="button" onClick={() => { setRecovery((current) => ({ ...current, email: loginForm.email })); setMode('forgot') }}>Mot de passe oublié ?</button> : <button className="auth-link" type="button" onClick={() => setMode('login')}>Retour à la connexion</button>}
          <div className="auth-security-note"><ShieldCheck size={16} /><span>Session personnelle, accès contrôlé par rôle et traçabilité des actions.</span></div>
          {mode === 'login' && <div className="demo-account"><span>Compte de démonstration</span><strong>admin@squ.edu.om</strong><code>Admin@123</code></div>}
        </form>
      </section>
    </div>
    {toast && <Toast {...toast} />}
  </main>
}

function AuthField({ icon: Icon, label, value, onChange, action, ...inputProps }) {
  return <label className="field auth-field"><span>{label}</span><div className="input-with-icon"><Icon size={18} aria-hidden="true" /><input {...inputProps} value={value} onChange={(event) => onChange(event.target.value)} />{action}</div></label>
}

function Shell({ session, activeView, setActiveView, onLogout, notify, toast, language, setLanguage, theme, setTheme }) {
  const [datasets, setDatasets] = useState({})
  const [personalNotifications, setPersonalNotifications] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [sidebarCollapsed, setSidebarCollapsed] = useState(initialSidebarCollapsed)
  const [sidebarClock, setSidebarClock] = useState(() => Date.now())
  const [profileOpen, setProfileOpen] = useState(false)
  const activeRole = normalizeRole(session.role)
  const allowedViews = views.filter((view) => view.roles.includes(activeRole))
  const activeViewLabel = allowedViews.find((view) => view.id === activeView)?.label || 'Espace FYP'
  const unreadCount = personalNotifications.filter((item) => !item.readAt).length
  const request = useCallback((path, options = {}) => apiRequest(path, options, session.token), [session.token])

  const loadPersonalNotifications = useCallback(async () => {
    try {
      setPersonalNotifications(unwrapList(await request('/api/notifications/me')))
    } catch {
      setPersonalNotifications([])
    }
  }, [request])

  const loadCore = useCallback(async () => {
    setLoading(true)
    setError('')
    const endpoints = [['users','/api/users'],['students','/api/students'],['evaluators','/api/evaluators'],['tracks','/api/tracks'],['projects','/api/projects'],['projectAssignments','/api/projects/my-evaluation-assignments'],['teams','/api/teams'],['phases','/api/phases'],['forms','/api/evaluation-forms'],['reports','/api/reports'],['notifications','/api/notifications'],['audit','/api/audit'],['grades','/api/grades/project/' + seedProjectId]]
    try {
      const pairs = await Promise.all(endpoints.map(async ([key, path]) => {
        try { return [key, unwrapList(await request(path))] } catch { return [key, []] }
      }))
      setDatasets(Object.fromEntries(pairs))
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [request])

  useEffect(() => {
    const timer = window.setTimeout(loadCore, 0)
    return () => window.clearTimeout(timer)
  }, [loadCore])
  useEffect(() => {
    const timer = window.setTimeout(loadPersonalNotifications, 0)
    const interval = window.setInterval(loadPersonalNotifications, 60000)
    return () => {
      window.clearTimeout(timer)
      window.clearInterval(interval)
    }
  }, [loadPersonalNotifications])
  useEffect(() => {
    const stillAllowed = allowedViews.some((view) => view.id === activeView)
    if (!stillAllowed) setActiveView(homeViewForRole(activeRole))
  }, [activeRole, activeView, allowedViews, setActiveView])
  useEffect(() => {
    localStorage.setItem('fyp-sidebar-collapsed', String(sidebarCollapsed))
  }, [sidebarCollapsed])
  useEffect(() => {
    const interval = window.setInterval(() => setSidebarClock(Date.now()), 3600000)
    return () => window.clearInterval(interval)
  }, [])
  useEffect(() => {
    if (!sidebarOpen) return undefined
    const previousOverflow = document.body.style.overflow
    const closeOnEscape = (event) => {
      if (event.key === 'Escape') setSidebarOpen(false)
    }
    document.body.style.overflow = 'hidden'
    window.addEventListener('keydown', closeOnEscape)
    return () => {
      document.body.style.overflow = previousOverflow
      window.removeEventListener('keydown', closeOnEscape)
    }
  }, [sidebarOpen])

  function navigate(view) {
    setActiveView(view)
    setSidebarOpen(false)
  }

  let activeContent = <ErrorState notFound message="Le module demandé n’est pas disponible pour cette session." onRetry={() => navigate('dashboard')} />
  if (activeView === 'dashboard') activeContent = <Dashboard datasets={datasets} request={request} activeRole={activeRole} notify={notify} setActiveView={navigate} allowedViews={allowedViews} />
  if (activeView === 'calendar') activeContent = <CalendarView phases={datasets.phases || []} />
  if (activeView === 'notifications') activeContent = <NotificationCenter notifications={personalNotifications} request={request} reload={loadPersonalNotifications} notify={notify} setActiveView={navigate} allowedViews={allowedViews} />
  if (activeView === 'imports') activeContent = <ImportCenter request={request} notify={notify} reload={loadCore} />
  if (activeView === 'crud') activeContent = <CrudStudio datasets={datasets} request={request} reload={loadCore} notify={notify} />
  if (activeView === 'evaluations') activeContent = <EvaluationStudio datasets={datasets} request={request} notify={notify} activeRole={activeRole} session={session} />
  if (activeView === 'extensions') activeContent = <ExtensionRequestCenter datasets={datasets} request={request} notify={notify} activeRole={activeRole} />
  if (activeView === 'grading') activeContent = <GradingCenter datasets={datasets} request={request} reload={loadCore} notify={notify} activeRole={activeRole} token={session.token} />
  if (activeView === 'reports') activeContent = <ReportCenter datasets={datasets} request={request} reload={loadCore} notify={notify} token={session.token} />
  if (activeView === 'api') activeContent = <ApiConsole request={request} notify={notify} />

  const initialLoading = loading && Object.keys(datasets).length === 0
  const sidebarSections = [
    { label: 'Vue générale', items: allowedViews.filter((view) => primarySidebarViews.has(view.id)) },
    { label: 'Modules métier', items: allowedViews.filter((view) => !primarySidebarViews.has(view.id)) },
  ].filter((section) => section.items.length > 0)
  const openPhase = (datasets.phases || []).find((phase) => phase.status === 'OPEN')
  const phaseRemainingDays = openPhase?.deadline
    ? Math.max(0, Math.ceil((new Date(openPhase.deadline).getTime() - sidebarClock) / 86400000))
    : null
  const roleInitial = String(actorTemplates[activeRole]?.title || activeRole || 'F').replace(/^Espace\s+/i, '').slice(0, 1).toUpperCase()

  return <div className={'app-shell premium-shell ' + (sidebarCollapsed ? 'sidebar-collapsed' : '')}>
    {sidebarOpen && <button type="button" className="mobile-backdrop" onClick={() => setSidebarOpen(false)} aria-label="Fermer la navigation" />}
    <aside className={'sidebar ' + (sidebarOpen ? 'open' : '')} aria-label="Navigation">
      <div className="sidebar-head">
        <LogoLockup compact />
        <div className="sidebar-head-actions">
          <button type="button" className="sidebar-collapse" onClick={() => setSidebarCollapsed((current) => !current)} aria-label={sidebarCollapsed ? 'Développer la navigation' : 'Réduire la navigation'} title={sidebarCollapsed ? 'Développer la navigation' : 'Réduire la navigation'}>{sidebarCollapsed ? <PanelLeftOpen size={18} /> : <PanelLeftClose size={18} />}</button>
          <button type="button" className="sidebar-close" onClick={() => setSidebarOpen(false)} aria-label="Fermer"><X size={19} /></button>
        </div>
      </div>
      <div className="sidebar-context">
        <span className="sidebar-role-avatar" aria-hidden="true">{roleInitial}</span>
        <div><span className="sidebar-kicker"><i />Espace actif</span><strong>{actorTemplates[activeRole]?.title}</strong><small>{actorTemplates[activeRole]?.summary}</small></div>
      </div>
      <div className={'sidebar-phase ' + (openPhase ? 'is-open' : 'is-idle')} title={openPhase?.name || 'Aucune phase ouverte'}>
        <span className="sidebar-phase-dot" aria-hidden="true" />
        <div><small>{openPhase ? 'Phase active' : 'Calendrier FYP'}</small><strong>{openPhase?.name || 'Aucune phase ouverte'}</strong></div>
        {phaseRemainingDays !== null && <b>{phaseRemainingDays}<small>j</small></b>}
      </div>
      <nav className="nav-list" aria-label="Navigation principale">{sidebarSections.map((section) => <section className="nav-section" key={section.label}>
        <div className="nav-section-title"><span>{section.label}</span><i /></div>
        <div className="nav-items">{section.items.map((view) => <button type="button" key={view.id} className={activeView === view.id ? 'active' : ''} onClick={() => navigate(view.id)} aria-current={activeView === view.id ? 'page' : undefined} title={sidebarCollapsed ? view.label : undefined}>
          <span className="nav-icon"><ViewIcon view={view.id} /></span>
          <span className="nav-label">{view.label}</span>
          {view.id === 'notifications' && unreadCount > 0 && <b>{Math.min(99, unreadCount)}</b>}
          <ChevronRight className="nav-chevron" size={15} aria-hidden="true" />
        </button>)}</div>
      </section>)}</nav>
      <div className="sidebar-footer">
        <button type="button" className="sidebar-user" onClick={() => setProfileOpen(true)} title="Profil et préférences"><span>{String(session.fullName || session.email || 'S').slice(0, 1).toUpperCase()}</span><div><strong>{session.fullName || 'Utilisateur SQU'}</strong><small>{session.email}</small></div></button>
        <button type="button" className="sidebar-logout" onClick={onLogout} title="Se déconnecter" aria-label="Se déconnecter"><LogOut size={18} /></button>
      </div>
    </aside>
    <main className="workspace">
      <header className="topbar">
        <div className="topbar-title"><button type="button" className="mobile-menu" onClick={() => setSidebarOpen(true)} aria-label="Ouvrir la navigation" aria-expanded={sidebarOpen}><Menu size={21} />{unreadCount > 0 && <i />}</button><div><span>{actorTemplates[activeRole]?.title}</span><h1>{activeViewLabel}</h1></div></div>
        <GlobalSearch allowedViews={allowedViews} datasets={datasets} onNavigate={navigate} />
        <div className="topbar-actions">
          <button type="button" className={'icon-button ' + (loading ? 'is-loading' : '')} onClick={() => { loadCore(); loadPersonalNotifications() }} title="Actualiser" aria-label="Actualiser"><RefreshCw size={18} /></button>
          <button type="button" className={'notification-button ' + (activeView === 'notifications' ? 'active' : '')} onClick={() => navigate('notifications')} title="Notifications" aria-label="Notifications"><Bell size={19} />{unreadCount > 0 && <span className="notification-badge">{Math.min(99, unreadCount)}</span>}</button>
          <ThemeToggle theme={theme} setTheme={setTheme} compact />
          <LanguageSwitcher language={language} setLanguage={setLanguage} />
          <button type="button" className="user-chip" onClick={() => setProfileOpen(true)}><span>{String(session.fullName || session.email || 'S').slice(0, 1).toUpperCase()}</span><div><strong>{session.fullName || 'Utilisateur SQU'}</strong><small>{pretty(activeRole)}</small></div></button>
        </div>
      </header>
      {loading && !initialLoading && <div className="loading-bar"><span /></div>}
      <section className="workspace-content" key={activeView}>
        {initialLoading ? <AppSkeleton /> : error ? <ErrorState message={error} onRetry={loadCore} /> : activeContent}
      </section>
    </main>
    <ProfileDrawer open={profileOpen} onClose={() => setProfileOpen(false)} session={session} request={request} notify={notify} theme={theme} setTheme={setTheme} language={language} setLanguage={setLanguage} onLogout={onLogout} roleLabel={actorTemplates[activeRole]?.title || pretty(activeRole)} />
    {toast && <Toast {...toast} />}
  </div>
}
function NotificationCenter({ notifications, request, reload, notify, setActiveView, allowedViews }) {
  const allowedViewIds = new Set(allowedViews.map((view) => view.id))
  const unreadCount = notifications.filter((item) => !item.readAt).length

  async function markRead(notification) {
    if (notification.readAt) return
    try {
      await request('/api/notifications/' + notification.id + '/read', { method: 'PATCH' })
      await reload()
    } catch (error) {
      notify(error.message, 'danger')
    }
  }

  async function markAllRead() {
    try {
      await request('/api/notifications/me/read-all', { method: 'PATCH' })
      await reload()
      notify('Toutes les notifications sont marquées comme lues')
    } catch (error) {
      notify(error.message, 'danger')
    }
  }

  async function openNotification(notification) {
    await markRead(notification)
    if (notification.actionView && allowedViewIds.has(notification.actionView)) {
      setActiveView(notification.actionView)
    }
  }

  return <section className="notification-center">
    <div className="section-head notification-heading">
      <div><span className="eyebrow">Centre personnel</span><h2>Notifications</h2><p>Alertes d’échéance, décisions de prolongation et messages destinés à votre compte.</p></div>
      <div className="notification-summary"><strong>{unreadCount}</strong><span>non lue{unreadCount === 1 ? '' : 's'}</span><button type="button" className="soft-button" onClick={markAllRead} disabled={!unreadCount}><CheckCheck size={17} />Tout marquer comme lu</button></div>
    </div>
    {!notifications.length && <EmptyState title="Aucune notification" detail="Vos prochaines alertes apparaîtront ici." />}
    <div className="notification-list">
      {notifications.map((notification) => <article key={notification.id} className={'notification-item ' + (notification.readAt ? 'read ' : 'unread ') + (notification.severity || 'INFO').toLowerCase()}>
        <div className="notification-symbol"><Bell size={19} /></div>
        <div className="notification-copy"><div className="notification-meta"><span>{pretty(notification.category || 'NOTIFICATION')}</span><time>{formatDateTime(notification.createdAt || notification.sentAt)}</time></div><h3>{notification.subject}</h3><p>{notification.body}</p></div>
        <div className="notification-actions">{!notification.readAt && <button type="button" className="icon-button" title="Marquer comme lue" aria-label="Marquer comme lue" onClick={() => markRead(notification)}><MailOpen size={18} /></button>}{notification.actionView && allowedViewIds.has(notification.actionView) && <button type="button" className="mini-button" onClick={() => openNotification(notification)}>Ouvrir</button>}</div>
      </article>)}
    </div>
  </section>
}
function Dashboard({ datasets, request, activeRole, notify, setActiveView, allowedViews }) {
  const [summary, setSummary] = useState(null)
  const [pending, setPending] = useState([])
  const allowedViewIds = new Set(allowedViews.map((view) => view.id))

  useEffect(() => {
    Promise.allSettled([request('/api/dashboard/admin/summary'), request('/api/dashboard/admin/pending-evaluations')]).then(([a, b]) => {
      if (a.status === 'fulfilled') setSummary(a.value.data)
      if (b.status === 'fulfilled') setPending(unwrapList(b.value))
    }).catch((error) => notify(error.message, 'danger'))
  }, [notify, request])

  const demoForms = (datasets.forms || []).filter((form) => form.evaluationType === 'DEMO_DAY_INDUSTRY')
  const dashboard = {
    ADMIN: {
      title: 'Tableau de bord administrateur',
      description: 'Pilotage global de la plateforme: comptes, projets, equipes, phases, evaluations, notes et rapports.',
      metrics: [['Utilisateurs', summary?.users ?? datasets.users?.length ?? 0], ['Projets', summary?.projects ?? datasets.projects?.length ?? 0], ['evaluations', summary?.evaluations ?? pending.length], ['Rapports', datasets.reports?.length ?? 0]],
      actions: [['Gestion des donnees', 'crud'], ['Imports Excel', 'imports'], ['evaluations', 'evaluations'], ['Demandes de prolongation', 'extensions'], ['Notes', 'grading'], ['Rapports', 'reports'], ['Console API', 'api']],
      primaryTitle: 'evaluations en attente', primaryRows: pending.slice(0, 8), primaryColumns: ['evaluationType','status','project','evaluator','updatedAt'],
      secondaryTitle: 'Projets actifs', secondaryRows: datasets.projects || [], secondaryColumns: ['title','academicYear','status','track'],
    },
    SUPERVISOR: {
      title: 'Tableau de bord superviseur',
      description: 'Suivi des projets encadres et saisie des fiches superviseur Phase I et Phase II.',
      metrics: [['Projets assignes', datasets.projects?.length ?? 0], ['Fiches a remplir', pending.length], ['Templates', datasets.forms?.length ?? 0], ['Deadlines', datasets.phases?.length ?? 0]],
      actions: [['Ouvrir les evaluations', 'evaluations'], ['Demander une prolongation', 'extensions'], ['Actualiser les deadlines', 'dashboard']],
      primaryTitle: 'Projets assignes', primaryRows: datasets.projects || [], primaryColumns: ['title','academicYear','status','track'],
      secondaryTitle: 'evaluations a traiter', secondaryRows: pending.slice(0, 8), secondaryColumns: ['evaluationType','status','project','evaluator','updatedAt'],
    },
    FACULTY_EVALUATOR: {
      title: 'Tableau de bord evaluateur academique',
      description: 'evaluation des rapports et soutenances avec brouillon, validation et verrouillage.',
      metrics: [['Projets assignes', datasets.projects?.length ?? 0], ['Rapports/oraux', pending.length], ['Templates actifs', datasets.forms?.length ?? 0], ['Phases', datasets.phases?.length ?? 0]],
      actions: [['Evaluer rapport/oral', 'evaluations'], ['Demander une prolongation', 'extensions'], ['Voir les deadlines', 'dashboard']],
      primaryTitle: 'Formulaires disponibles', primaryRows: datasets.forms || [], primaryColumns: ['name','evaluationType','phaseType','active'],
      secondaryTitle: 'evaluations en attente', secondaryRows: pending.slice(0, 8), secondaryColumns: ['evaluationType','status','project','evaluator','updatedAt'],
    },
    INDUSTRY_REPRESENTATIVE: {
      title: 'Tableau de bord representant industriel',
      description: 'evaluation Demo Day: prototype, impact industriel et feedback final.',
      metrics: [['equipes Demo Day', datasets.teams?.length ?? 0], ['Projets ? Evaluer', datasets.projects?.length ?? 0], ['Fiches Demo', demoForms.length], ['Soumissions', pending.length]],
      actions: [['Évaluer Demo Day', 'evaluations'], ['Consulter les notes publiées', 'grading'], ['Demander une prolongation', 'extensions']],
      primaryTitle: 'equipes Demo Day', primaryRows: datasets.teams || [], primaryColumns: ['name','section','academicYear','project'],
      secondaryTitle: 'Formulaires Demo Day', secondaryRows: demoForms, secondaryColumns: ['name','evaluationType','phaseType','active'],
    },
    COORDINATOR: {
      title: 'Tableau de bord coordinateur FYP',
      description: 'Consolidation des rapports, notes finales, notifications et suivi de completion.',
      metrics: [['Rapports', datasets.reports?.length ?? 0], ['Notes', datasets.grades?.length ?? 0], ['Notifications', datasets.notifications?.length ?? 0], ['Audit logs', datasets.audit?.length ?? 0]],
      actions: [['Consulter les rapports', 'reports'], ['Voir les notes', 'grading'], ['Suivre la progression', 'dashboard']],
      primaryTitle: 'Rapports recents', primaryRows: datasets.reports || [], primaryColumns: ['project','phase','status','recipientEmail','generatedAt'],
      secondaryTitle: 'Notes consolidees', secondaryRows: datasets.grades || [], secondaryColumns: ['phaseType','weightedScore','finalScore','published'],
    },
  }[activeRole] || {}

  const actions = (dashboard.actions || []).filter(([, view]) => allowedViewIds.has(view))
  const roleActions = actorTemplates[activeRole]?.actions || []
  const rolePanels = actorTemplates[activeRole]?.panels || []
  const phases = [...(datasets.phases || [])].sort((left, right) => new Date(left.deadline || 0) - new Date(right.deadline || 0))
  const currentPhase = phases.find((phase) => phase.status === 'OPEN') || phases.find((phase) => new Date(phase.deadline || 0) >= new Date())
  const maximumMetric = Math.max(1, ...(dashboard.metrics || []).map(([, value]) => Number(value) || 0))
  const todayLabel = new Intl.DateTimeFormat(currentLocale(), { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date())

  return <section className="dashboard-page page-enter">
    <header className="dashboard-hero">
      <div className="dashboard-hero-copy"><span className="eyebrow">{todayLabel}</span><h2>{dashboard.title}</h2><p>{dashboard.description}</p><div className="dashboard-role-tags">{roleActions.slice(0, 3).map((action) => <span key={action}><CheckCircle2 size={14} />{action}</span>)}</div></div>
      <div className="dashboard-focus"><span>Phase active</span><strong>{currentPhase?.name || 'Aucune phase ouverte'}</strong><small>{currentPhase?.deadline ? 'Échéance · ' + formatDateTime(currentPhase.deadline) : 'Le calendrier sera affiché après configuration.'}</small>{actions[0] && <button type="button" className="primary-action" onClick={() => setActiveView(actions[0][1])}>{actions[0][0]}<ArrowRight size={17} /></button>}</div>
    </header>

    <div className="metric-grid">{(dashboard.metrics || []).map(([label, value], index) => <article className={'metric tone-' + (index + 1)} key={label}><div className="metric-top"><span>{label}</span><i><MetricIcon label={label} /></i></div><strong>{value}</strong><div className="metric-track"><span style={{ width: Math.max(4, ((Number(value) || 0) / maximumMetric) * 100) + '%' }} /></div></article>)}</div>

    <div className="dashboard-columns">
      <Panel title="Accès rapide" subtitle="Les actions les plus utiles pour votre rôle." accent="green" className="quick-panel"><div className="quick-actions">{actions.map(([label, view]) => <button type="button" key={label} onClick={() => setActiveView(view)}><ViewIcon view={view} /><span>{label}</span><ArrowRight size={16} /></button>)}</div></Panel>
      <Panel title="Aperçu opérationnel" subtitle="Volumes réels actuellement chargés." accent="blue" className="analytics-panel"><div className="operation-chart">{(dashboard.metrics || []).map(([label, value]) => <div key={label}><span>{label}</span><div><i style={{ width: ((Number(value) || 0) / maximumMetric) * 100 + '%' }} /></div><strong>{value}</strong></div>)}</div></Panel>
      <Panel title="Prochaines échéances" subtitle="Phases configurées par l’administration." accent="gold" className="deadline-panel"><div className="deadline-list">{phases.slice(0, 4).map((phase) => <button type="button" key={phase.id} onClick={() => setActiveView('calendar')}><span className={'phase-dot ' + String(phase.status || '').toLowerCase()} /><div><strong>{phase.name}</strong><small>{formatDateTime(phase.deadline)}</small></div><StatusPill value={phase.status} /></button>)}{!phases.length && <EmptyState title="Aucune échéance" detail="Les phases planifiées apparaîtront ici." />}</div></Panel>
    </div>

    <section className="dashboard-capabilities"><div><span className="eyebrow">Périmètre autorisé</span><h3>Votre espace de travail</h3><p>{actorTemplates[activeRole]?.summary}</p></div><div className="capability-list">{rolePanels.map((panel) => <span key={panel}><ShieldCheck size={15} />{panel}</span>)}</div></section>

    <Panel title={dashboard.primaryTitle} wide className="dashboard-table"><DataTable rows={dashboard.primaryRows || []} columns={dashboard.primaryColumns || []} compact /></Panel>
    <Panel title={dashboard.secondaryTitle} wide className="dashboard-table"><DataTable rows={dashboard.secondaryRows || []} columns={dashboard.secondaryColumns || []} compact /></Panel>
  </section>
}

function CrudStudio({ datasets, request, reload, notify }) {
  const [resourceKey, setResourceKey] = useState('users')
  const config = resourceConfigs[resourceKey]
  return <section className="crud-studio"><div className="section-head"><div><h2>CRUD studio</h2><p>Manage every backend resource required by the FYP workflow.</p></div></div><div className="resource-tabs">{Object.entries(resourceConfigs).map(([key, cfg]) => <button key={key} className={resourceKey === key ? 'active' : ''} onClick={() => setResourceKey(key)}>{cfg.title}</button>)}</div><ResourceManager key={resourceKey} resourceKey={resourceKey} config={config} datasets={datasets} request={request} reload={reload} notify={notify} /></section>
}

function ResourceManager({ resourceKey, config, datasets, request, reload, notify }) {
  const [rows, setRows] = useState(datasets[resourceKey] || [])
  const [form, setForm] = useState(() => initialForm(config.fields || []))
  const [editing, setEditing] = useState(null)
  const [filter, setFilter] = useState('')
  const [busy, setBusy] = useState(false)
  useEffect(() => {
    const timer = window.setTimeout(() => setRows(datasets[resourceKey] || []), 0)
    return () => window.clearTimeout(timer)
  }, [datasets, resourceKey])
  const filtered = rows.filter((row) => JSON.stringify(row).toLowerCase().includes(filter.toLowerCase()))

  function edit(row) {
    const next = initialForm(config.fields || [])
    ;(config.fields || []).forEach((field) => {
      if (field.name.endsWith('Id')) next[field.name] = row[field.name.replace(/Id$/, '')]?.id || row[field.name] || ''
      else if (field.name === 'studentIds') next[field.name] = (row.students || []).map((student) => student.id)
      else if (field.type === 'datetime-local' && row[field.name]) next[field.name] = String(row[field.name]).slice(0, 16)
      else next[field.name] = row[field.name] ?? next[field.name]
    })
    setEditing(row); setForm(next)
  }

  async function submit(event) {
    event.preventDefault(); if (config.readOnly) return; setBusy(true)
    try {
      const payload = serialize(form, config.fields)
      await request(editing ? config.endpoint + '/' + editing.id : (config.customCreateEndpoint || config.endpoint), { method: editing ? 'PUT' : 'POST', body: JSON.stringify(payload) })
      notify(config.title + ' saved'); setEditing(null); setForm(initialForm(config.fields || [])); await reload(); setRows(unwrapList(await request(config.endpoint)))
    } catch (error) { notify(error.message, 'danger') } finally { setBusy(false) }
  }

  async function remove(row) {
    if (!window.confirm(translateText('Delete ' + itemName(row) + '?'))) return; setBusy(true)
    try { await request(config.endpoint + '/' + row.id, { method: 'DELETE' }); notify(config.title + ' deleted'); await reload(); setRows(unwrapList(await request(config.endpoint))) } catch (error) { notify(error.message, 'danger') } finally { setBusy(false) }
  }

  return <div className="manager-grid">
    <Panel title={config.title} subtitle={config.subtitle} wide><div className="table-toolbar"><input value={filter} onChange={(e) => setFilter(e.target.value)} placeholder="Search records" /><button className="soft-button" onClick={async () => setRows(unwrapList(await request(config.endpoint)))}>Reload</button></div><DataTable rows={filtered} columns={config.columns} onEdit={!config.readOnly ? edit : null} onDelete={!config.readOnly ? remove : null} /></Panel>
    {!config.readOnly && <Panel title={editing ? 'Edit record' : 'Create record'} accent="green"><form className="stack-form compact" onSubmit={submit}>{(config.fields || []).map((field) => <DynamicField key={field.name} field={field} value={form[field.name]} datasets={datasets} onChange={(value) => setForm({ ...form, [field.name]: value })} />)}<button className="primary-action" disabled={busy}>{busy ? 'Saving...' : editing ? 'Update' : 'Create'}</button>{editing && <button type="button" className="ghost-button" onClick={() => { setEditing(null); setForm(initialForm(config.fields || [])) }}>Cancel edit</button>}</form></Panel>}
  </div>
}
function ImportCenter({ request, notify, reload }) {
  const [kind, setKind] = useState('initialization')
  const [file, setFile] = useState(null)
  const [preview, setPreview] = useState(null)
  const [busy, setBusy] = useState(false)
  const initializationMode = kind === 'initialization'

  function resetMode(nextKind) {
    setKind(nextKind)
    setPreview(null)
    setFile(null)
  }

  async function analyze() {
    if (!file) return
    const body = new FormData()
    body.append('file', file)
    setBusy(true)
    try {
      const endpoint = initializationMode ? '/api/import/initialization/preview' : '/api/import/students/preview'
      const response = await request(endpoint, { method: 'POST', body })
      const report = response.data
      if (initializationMode) {
        setPreview({
          ...report,
          invalidRows: Math.max(0, (report.totalRows || 0) - (report.validRows || 0)),
          errors: report.errors || [],
        })
        notify(report.importable ? 'Classeur complet prêt pour l’import' : 'Classeur analysé avec des erreurs à corriger', report.importable ? 'success' : 'danger')
      } else {
        const errors = (report.errors || []).map(formatStudentImportError)
        setPreview({
          sheetName: report.sheetName,
          normalized: report.rows || [],
          totalRows: report.totalRows || 0,
          validRows: report.validRows || 0,
          invalidRows: (report.totalRows || 0) - (report.validRows || 0),
          errors,
        })
        notify(errors.length ? 'Fichier analysé avec des lignes à corriger' : 'Fichier prêt pour l’import', errors.length ? 'danger' : 'success')
      }
    } catch (error) {
      setPreview(null)
      notify(error.message, 'danger')
    } finally {
      setBusy(false)
    }
  }

  async function importToServer() {
    if (!file || !preview || preview.errors.length || preview.totalRows === 0) return
    const body = new FormData()
    body.append('file', file)
    setBusy(true)
    try {
      const endpoint = initializationMode ? '/api/import/initialization' : '/api/import/students'
      const response = await request(endpoint, { method: 'POST', body })
      const report = response.data
      if (initializationMode) {
        setPreview({ ...report, invalidRows: 0, errors: [] })
        const created = (report.sheets || []).reduce((sum, sheet) => sum + (sheet.created || 0), 0)
        const updated = (report.sheets || []).reduce((sum, sheet) => sum + (sheet.updated || 0), 0)
        notify(`Initialisation terminée : ${created} créations et ${updated} mises à jour`)
      } else {
        setPreview((current) => ({
          ...current,
          normalized: report.rows || current.normalized,
          totalRows: report.totalRows,
          validRows: report.validRows,
          invalidRows: 0,
          errors: [],
        }))
        notify(`${report.created} étudiants ajoutés, ${report.updated} mis à jour, ${report.unchanged} inchangés`)
      }
      await reload()
    } catch (error) {
      notify(error.message, 'danger')
    } finally {
      setBusy(false)
    }
  }

  const templateHref = initializationMode
    ? '/modele_initialisation_plateforme_fyp.xlsx'
    : '/modele_import_etudiants_squ.xlsx'

  return <section className="import-workspace">
    <div className="section-head">
      <div>
        <span className="eyebrow">Administration des données</span>
        <h2>{initializationMode ? 'Initialiser une année FYP' : 'Mettre à jour les étudiants'}</h2>
        <p>{initializationMode
          ? 'Chargez en une opération les étudiants, comptes acteurs, projets, équipes et affectations. Configurez ensuite les phases et leurs échéances dans la plateforme.'
          : 'Synchronisez ponctuellement le référentiel officiel SQU sans modifier les projets et les affectations.'}</p>
      </div>
      <a className="soft-button download-template" href={templateHref} download>Télécharger le modèle Excel</a>
    </div>

    <div className="import-mode" role="tablist" aria-label="Type d’import">
      <button className={initializationMode ? 'active' : ''} onClick={() => resetMode('initialization')}>Initialisation annuelle</button>
      <button className={!initializationMode ? 'active' : ''} onClick={() => resetMode('students')}>Mise à jour étudiants</button>
    </div>

    {initializationMode && <section className="import-steps" aria-label="Étapes d’initialisation">
      <div><span>01</span><strong>Télécharger</strong><small>Utiliser le modèle officiel.</small></div>
      <div><span>02</span><strong>Compléter</strong><small>Remplir les sept feuilles de données.</small></div>
      <div><span>03</span><strong>Analyser</strong><small>Corriger toutes les références invalides.</small></div>
      <div><span>04</span><strong>Importer</strong><small>Valider la transaction complète.</small></div>
    </section>}

    <section className="import-dropzone">
      <div>
        <strong>{file?.name || (initializationMode ? 'Sélectionner le classeur maître' : 'Sélectionner le fichier officiel des étudiants')}</strong>
        <span>{file ? formatFileSize(file.size) : initializationMode ? '.xlsx · 15 Mo maximum' : '.xlsx ou .csv · 10 Mo maximum'}</span>
      </div>
      <label className="file-picker">
        <input type="file" accept={initializationMode ? '.xlsx' : '.xlsx,.csv'} onChange={(event) => { setFile(event.target.files?.[0] || null); setPreview(null) }} />
        <span>Choisir le fichier</span>
      </label>
      <button className="primary-action" disabled={!file || busy} onClick={analyze}>{busy ? 'Analyse…' : 'Analyser sans enregistrer'}</button>
    </section>

    {preview && <>
      <section className="import-summary">
        <div><span>{initializationMode ? 'Feuilles contrôlées' : 'Feuille'}</span><strong>{initializationMode ? (preview.sheets || []).length : preview.sheetName}</strong></div>
        <div><span>Lignes détectées</span><strong>{preview.totalRows}</strong></div>
        <div><span>Lignes valides</span><strong>{preview.validRows}</strong></div>
        <div><span>À corriger</span><strong className={preview.invalidRows ? 'danger-text' : ''}>{preview.invalidRows}</strong></div>
      </section>

      {preview.errors.length > 0 && <div className="import-errors" role="alert">
        <strong>{preview.errors.length} erreur{preview.errors.length > 1 ? 's' : ''} bloque{preview.errors.length > 1 ? 'nt' : ''} l’import</strong>
        {preview.errors.slice(0, 20).map((error, index) => <span key={`${error.sheet || 'students'}-${error.rowNumber || index}-${error.field || index}`}>
          {initializationMode ? `${error.sheet} · ligne ${error.rowNumber || '-'} · ${error.field}: ${error.message}` : error}
        </span>)}
        {preview.errors.length > 20 && <span>…et {preview.errors.length - 20} autres erreurs dans le fichier.</span>}
      </div>}

      {initializationMode
        ? <section className="import-preview">
            <div className="section-head"><div><h3>Contrôle feuille par feuille</h3><p>Aucune donnée n’est enregistrée pendant cette analyse.</p></div></div>
            <div className="table-wrap"><table className="initialization-table"><thead><tr><th>Feuille</th><th>Lignes</th><th>Valides</th><th>Créations</th><th>Mises à jour</th><th>Inchangées</th><th>État</th></tr></thead><tbody>
              {(preview.sheets || []).map((sheet) => <tr key={sheet.sheet}>
                <td><strong>{sheet.sheet}</strong></td><td>{sheet.totalRows}</td><td>{sheet.validRows}</td><td>{sheet.created || 0}</td><td>{sheet.updated || 0}</td><td>{sheet.unchanged || 0}</td>
                <td><span className={'validation-state ' + (sheet.totalRows === sheet.validRows ? 'valid' : 'invalid')}>{sheet.totalRows === sheet.validRows ? 'Valide' : 'À corriger'}</span></td>
              </tr>)}
            </tbody></table></div>
          </section>
        : <section className="import-preview">
            <div className="section-head"><div><h3>Aperçu des étudiants</h3><p>{preview.totalRows} lignes détectées dans le fichier officiel.</p></div></div>
            <div className="table-wrap"><table><thead><tr>{['stdID', 'Cohorte', 'Nom complet', 'E-mail SQU', 'Action', 'État'].map((column) => <th key={column}>{column}</th>)}</tr></thead><tbody>
              {(preview.normalized || []).slice(0, 15).map((row) => <tr key={row.rowNumber}><td>{row.studentNumber}</td><td>{row.cohort}</td><td>{row.fullName}</td><td>{row.email}</td><td>{row.existing ? 'Mise à jour' : 'Création'}</td><td><span className={'validation-state ' + (row.errors?.length ? 'invalid' : 'valid')}>{row.errors?.length ? 'À corriger' : 'Valide'}</span></td></tr>)}
            </tbody></table></div>
          </section>}

      <div className="import-actions">
        <span>{preview.errors.length ? 'Corrigez le classeur puis relancez l’analyse.' : 'Validation terminée. L’import peut être exécuté.'}</span>
        <button className="primary-action" disabled={busy || preview.errors.length > 0 || preview.totalRows === 0} onClick={importToServer}>
          {busy ? 'Import…' : initializationMode ? 'Initialiser la plateforme' : 'Créer ou mettre à jour les étudiants'}
        </button>
      </div>
    </>}
  </section>
}

function formatStudentImportError(error) {
  const messages = {
    'Required value is missing': 'valeur obligatoire manquante',
    'Student ID must contain 5 to 12 digits': 'le stdID doit contenir 5 à 12 chiffres',
    'Invalid email address': 'adresse e-mail invalide',
    'Duplicate student ID in file': 'stdID dupliqué dans le fichier',
    'Duplicate email in file': 'e-mail dupliqué dans le fichier',
    'Email already belongs to another student': 'cet e-mail appartient déjà à un autre étudiant',
    'Cohort must use YY or YYYY format': 'la cohorte doit utiliser le format YY ou YYYY',
  }
  const message = messages[error.message] || error.message
  return `Ligne ${error.rowNumber} · ${error.field}: ${message}`
}

function formatFileSize(bytes) {
  if (bytes < 1024) return bytes + ' octets'
  return (bytes / 1024).toLocaleString(currentLocale(), { maximumFractionDigits: 1 }) + ' Ko'
}

function EvaluationStudio({ datasets, request, notify, activeRole, session }) {
  const [draft, setDraft] = useState({ projectId: '', phaseId: '', evaluatorId: '', evaluationType: activeRole === 'ADMIN' ? 'ORAL_PHASE_I' : '', trackCode: 'CSN', generalComment: '' })
  const [scoreDrafts, setScoreDrafts] = useState(() => readLocalJson('fyp-score-sheets', {}))
  const [sheetStatuses, setSheetStatuses] = useState(() => readLocalJson('fyp-score-statuses', {}))
  const [submissionIds, setSubmissionIds] = useState({})
  const [projectEvaluations, setProjectEvaluations] = useState([])
  const [showFormula, setShowFormula] = useState(false)
  const [phaseAccess, setPhaseAccess] = useState(null)
  const [saveState, setSaveState] = useState('idle')
  const [lastSavedAt, setLastSavedAt] = useState(null)
  const [currentTime, setCurrentTime] = useState(() => new Date().getTime())
  const [extensionReason, setExtensionReason] = useState('')
  const [requestedDeadline, setRequestedDeadline] = useState('')
  const [extensionBusy, setExtensionBusy] = useState(false)
  const autoSaveTimer = useRef(null)
  const personalAssignments = useMemo(() => datasets.projectAssignments || [], [datasets.projectAssignments])
  const allProjects = useMemo(() => datasets.projects || [], [datasets.projects])
  const assignedProjectIds = useMemo(() => new Set(personalAssignments.map((assignment) => assignment.projectId)), [personalAssignments])
  const evaluationProjects = useMemo(() => activeRole === 'ADMIN'
    ? allProjects
    : allProjects.filter((project) => assignedProjectIds.has(project.id)), [activeRole, allProjects, assignedProjectIds])
  const availableTrackCodes = useMemo(() => [...new Set(evaluationProjects
    .map((project) => project.track?.code || project.trackCode)
    .filter(Boolean))], [evaluationProjects])
  const trackOptions = activeRole === 'ADMIN' ? ['CSN', 'CSP', 'EIC', 'PSE'] : availableTrackCodes
  const availableEvaluationTypes = useMemo(() => {
    const roleTypes = activeRole === 'INDUSTRY_REPRESENTATIVE' ? ['DEMO_DAY_INDUSTRY'] : EVALUATION_TYPES
    return activeRole === 'ADMIN'
      ? roleTypes
      : roleTypes.filter((type) => personalAssignments.some((assignment) => assignment.projectId === draft.projectId && assignment.evaluationType === type))
  }, [activeRole, draft.projectId, personalAssignments])
  const selectedProject = evaluationProjects.find((project) => project.id === draft.projectId)
  const requiredPhaseType = phaseTypeForEvaluation(draft.evaluationType)
  const availablePhases = useMemo(() => (datasets.phases || []).filter((phase) => {
    if (requiredPhaseType && phase.type !== requiredPhaseType) return false
    return !selectedProject?.academicYear || !phase.academicYear || phase.academicYear === selectedProject.academicYear
  }), [datasets.phases, requiredPhaseType, selectedProject])
  const template = SCORING_TEMPLATES[draft.evaluationType] || SCORING_TEMPLATES.ORAL_PHASE_I
  const sheetId = [draft.trackCode, draft.projectId || 'apercu', draft.phaseId || 'phase', draft.evaluatorId || 'evaluator', draft.evaluationType].join(':')
  const activeScores = scoreDrafts[sheetId] || {}
  const status = sheetStatuses[sheetId] || 'DRAFT'
  const locked = status === 'SUBMITTED' || status === 'LOCKED'
  const selectedPhase = (datasets.phases || []).find((phase) => phase.id === draft.phaseId)
  const evaluationBlocked = Boolean(draft.phaseId && phaseAccess && !phaseAccess.allowed)
  const editingDisabled = locked || !draft.phaseId || phaseAccess?.allowed !== true
  const contextSelected = Boolean(draft.projectId && draft.phaseId && draft.evaluatorId && draft.evaluationType)
  const allEvaluators = useMemo(() => datasets.evaluators || [], [datasets.evaluators])
  const evaluatorOptions = useMemo(() => activeRole === 'ADMIN' ? allEvaluators : allEvaluators.filter((evaluator) => {
    const user = evaluator.user || {}
    return user.id === session.userId || user.email === session.email
  }), [activeRole, allEvaluators, session.email, session.userId])
  const selectedEvaluator = allEvaluators.find((evaluator) => evaluator.id === draft.evaluatorId)
  const evaluatorDisplayName = itemName(
    selectedEvaluator?.user || selectedEvaluator,
    session.fullName || session.email || (activeRole === 'INDUSTRY_REPRESENTATIVE' ? 'Membre du jury' : 'Évaluateur'),
  )

  const selectedTeam = (datasets.teams || []).find((team) => {
    const linkedProjectId = team.project?.id || team.projectId
    return linkedProjectId && linkedProjectId === draft.projectId
  })
  const importedStudents = (selectedTeam?.students || []).map(toStudentTarget)
  const studentTargets = importedStudents
  const requiresStudentTargets = template.sections.some((section) => section.target === 'student')
  const contextReady = contextSelected && (!requiresStudentTargets || studentTargets.length > 0)
  const targetIds = studentTargets.map((student) => student.id)
  const results = calculateTemplate(template, activeScores, targetIds)
  const resultTargets = template.sections.some((section) => section.target === 'student')
    ? studentTargets
    : [{ id: 'group', label: selectedTeam?.name || 'Groupe / projet', secondary: draft.trackCode }]
  const requiredCells = template.sections.reduce((total, section) => {
    const targetCount = section.target === 'student' ? studentTargets.length : 1
    return total + section.criteria.length * targetCount
  }, 0)
  const completedCells = Object.values(activeScores).filter((value) => value !== '' && value !== null && value !== undefined).length
  const effectiveDeadline = phaseAccess?.effectiveDeadline || selectedPhase?.deadline
  const remainingHours = effectiveDeadline ? (new Date(effectiveDeadline).getTime() - currentTime) / 3600000 : null
  const deadlineAlert = phaseAccess?.allowed && remainingHours !== null && remainingHours > 0 && remainingHours <= 24
    ? remainingHours <= 12 ? 'half-day' : 'one-day'
    : null

  useEffect(() => { localStorage.setItem('fyp-score-sheets', JSON.stringify(scoreDrafts)) }, [scoreDrafts])
  useEffect(() => {
    if (activeRole === 'ADMIN' || !draft.projectId) return undefined
    const nextType = availableEvaluationTypes.includes(draft.evaluationType) ? draft.evaluationType : availableEvaluationTypes[0] || ''
    if (nextType === draft.evaluationType) return undefined
    const timer = window.setTimeout(() => setDraft((current) => ({ ...current, evaluationType: nextType })), 0)
    return () => window.clearTimeout(timer)
  }, [activeRole, availableEvaluationTypes, draft.evaluationType, draft.projectId])
  useEffect(() => { localStorage.setItem('fyp-score-statuses', JSON.stringify(sheetStatuses)) }, [sheetStatuses])
  useEffect(() => {
    const interval = window.setInterval(() => setCurrentTime(new Date().getTime()), 60000)
    return () => window.clearInterval(interval)
  }, [])
  useEffect(() => {
    if (activeRole === 'ADMIN' || draft.evaluatorId || evaluatorOptions.length !== 1) return undefined
    const timer = window.setTimeout(() => setDraft((current) => ({ ...current, evaluatorId: evaluatorOptions[0].id })), 0)
    return () => window.clearTimeout(timer)
  }, [activeRole, draft.evaluatorId, evaluatorOptions])
  useEffect(() => {
    if (activeRole === 'ADMIN' || !availableTrackCodes.length || availableTrackCodes.includes(draft.trackCode)) return undefined
    const timer = window.setTimeout(() => setDraft((current) => ({ ...current, trackCode: availableTrackCodes[0] })), 0)
    return () => window.clearTimeout(timer)
  }, [activeRole, availableTrackCodes, draft.trackCode])
  useEffect(() => {
    if (activeRole !== 'INDUSTRY_REPRESENTATIVE' || !draft.projectId) return undefined
    const selectedPhaseIsAllowed = availablePhases.some((phase) => phase.id === draft.phaseId)
    const nextPhaseId = selectedPhaseIsAllowed ? draft.phaseId : availablePhases.length === 1 ? availablePhases[0].id : ''
    if (nextPhaseId === draft.phaseId) return undefined
    const timer = window.setTimeout(() => {
      setPhaseAccess(null)
      setDraft((current) => ({ ...current, phaseId: nextPhaseId }))
    }, 0)
    return () => window.clearTimeout(timer)
  }, [activeRole, availablePhases, draft.phaseId, draft.projectId])
  useEffect(() => {
    if (!draft.phaseId) return undefined
    let active = true
    request('/api/phases/' + draft.phaseId + '/evaluation-access')
      .then((response) => { if (active) setPhaseAccess(response.data) })
      .catch((error) => { if (active) setPhaseAccess({ allowed: false, reasonCode: 'ACCESS_CHECK_FAILED', message: error.message }) })
    return () => { active = false }
  }, [draft.phaseId, request])
  useEffect(() => {
    if (!contextReady) return undefined
    let active = true
    const query = new URLSearchParams({
      projectId: draft.projectId,
      phaseId: draft.phaseId,
      evaluatorId: draft.evaluatorId,
      evaluationType: draft.evaluationType,
    })
    request('/api/evaluations/sheet/current?' + query.toString())
      .then((response) => {
        if (!active || !response.data) return
        const submission = response.data
        let storedScores = {}
        try { storedScores = JSON.parse(submission.scorePayload || '{}') } catch { storedScores = {} }
        setScoreDrafts((current) => ({ ...current, [sheetId]: storedScores }))
        setSheetStatuses((current) => ({ ...current, [sheetId]: submission.locked ? 'SUBMITTED' : 'DRAFT' }))
        setSubmissionIds((current) => ({ ...current, [sheetId]: submission.id }))
        setDraft((current) => ({ ...current, generalComment: submission.generalComment || '' }))
        setLastSavedAt(submission.draftSavedAt)
        setSaveState('saved')
      })
      .catch(() => { if (active) setSaveState('idle') })
    return () => { active = false }
  }, [contextReady, draft.evaluationType, draft.evaluatorId, draft.phaseId, draft.projectId, request, sheetId])
  useEffect(() => () => window.clearTimeout(autoSaveTimer.current), [sheetId])

  async function loadProjectEvaluations(projectId) {
    if (!projectId) { setProjectEvaluations([]); return }
    try { setProjectEvaluations(unwrapList(await request('/api/evaluations/by-project/' + projectId))) } catch { setProjectEvaluations([]) }
  }

  function cleanScores(values) {
    return Object.fromEntries(Object.entries(values).filter(([, value]) => value !== '' && value !== null && value !== undefined && Number.isFinite(Number(value))).map(([key, value]) => [key, Number(value)]))
  }

  async function persistDraft(scoresToSave = activeScores, silent = false, comment = draft.generalComment) {
    if (!contextReady || editingDisabled) {
      if (!silent) notify(activeRole === 'ADMIN' ? 'Sélectionnez un projet, une phase ouverte et un évaluateur.' : 'Sélectionnez un projet qui vous est attribué et une phase ouverte.', 'danger')
      return null
    }
    setSaveState('saving')
    const submissionId = submissionIds[sheetId]
    try {
      const response = await request(submissionId ? '/api/evaluations/' + submissionId + '/sheet/draft' : '/api/evaluations/sheet/draft', {
        method: submissionId ? 'PUT' : 'POST',
        body: JSON.stringify({
          projectId: draft.projectId,
          phaseId: draft.phaseId,
          evaluatorId: draft.evaluatorId,
          evaluationType: draft.evaluationType,
          generalComment: comment,
          scores: cleanScores(scoresToSave),
          requiredScoreCount: requiredCells,
        }),
      })
      const saved = response.data
      setSubmissionIds((current) => ({ ...current, [sheetId]: saved.id }))
      setSheetStatuses((current) => ({ ...current, [sheetId]: 'DRAFT' }))
      setLastSavedAt(saved.draftSavedAt)
      setSaveState('saved')
      if (!silent) notify('Brouillon enregistré dans PostgreSQL')
      return saved.id
    } catch (error) {
      setSaveState('error')
      if (!silent) notify(error.message, 'danger')
      return null
    }
  }

  function queueAutoSave(scoresToSave, comment = draft.generalComment) {
    window.clearTimeout(autoSaveTimer.current)
    if (!contextReady || editingDisabled) return
    setSaveState('pending')
    autoSaveTimer.current = window.setTimeout(() => persistDraft(scoresToSave, true, comment), 1000)
  }

  function updateScore(sectionId, criterionId, targetId, value) {
    if (editingDisabled) return
    const normalized = value === '' ? '' : normalizeScore(value)
    const key = scoreKey(sectionId, criterionId, targetId)
    const nextScores = { ...activeScores, [key]: normalized }
    setScoreDrafts((current) => ({ ...current, [sheetId]: nextScores }))
    queueAutoSave(nextScores)
  }

  async function saveSheet(submitAfter = false) {
    window.clearTimeout(autoSaveTimer.current)
    if (editingDisabled) { notify('La phase est fermée pour cette évaluation.', 'danger'); return }
    if (submitAfter && completedCells < requiredCells) {
      notify('Toutes les notes doivent être renseignées avant validation.', 'danger')
      return
    }
    const submissionId = await persistDraft(activeScores, submitAfter)
    if (!submissionId || !submitAfter) return
    try {
      const response = await request('/api/evaluations/' + submissionId + '/submit', { method: 'POST' })
      setSheetStatuses((current) => ({ ...current, [sheetId]: 'SUBMITTED' }))
      setLastSavedAt(response.data.submittedAt)
      setSaveState('submitted')
      notify('Fiche validée et verrouillée')
      await loadProjectEvaluations(draft.projectId)
    } catch (error) {
      setSaveState('error')
      notify(error.message, 'danger')
    }
  }

  async function requestExtension(event) {
    event.preventDefault()
    if (!draft.phaseId || !extensionReason.trim()) return
    setExtensionBusy(true)
    try {
      await request('/api/phase-extension-requests', {
        method: 'POST',
        body: JSON.stringify({
          phaseId: draft.phaseId,
          reason: extensionReason.trim(),
          requestedDeadline: requestedDeadline ? new Date(requestedDeadline).toISOString().slice(0, 19) : null,
        }),
      })
      setExtensionReason('')
      setRequestedDeadline('')
      notify('Demande envoyée aux administrateurs')
    } catch (error) { notify(error.message, 'danger') } finally { setExtensionBusy(false) }
  }

  function resetSheet() {
    if (editingDisabled) return
    if (!window.confirm(translateText('Effacer toutes les notes de cette fiche ?'))) return
    setScoreDrafts((current) => ({ ...current, [sheetId]: {} }))
    setDraft((current) => ({ ...current, generalComment: '' }))
    setSheetStatuses((current) => ({ ...current, [sheetId]: 'DRAFT' }))
    queueAutoSave({}, '')
    notify('Fiche réinitialisée')
  }

  function saveStateLabel() {
    if (saveState === 'pending') return 'Modification en attente…'
    if (saveState === 'saving') return 'Enregistrement du brouillon…'
    if (saveState === 'saved') return 'Brouillon enregistré' + (lastSavedAt ? ' · ' + formatDateTime(lastSavedAt) : '')
    if (saveState === 'submitted') return 'Fiche validée définitivement'
    if (saveState === 'error') return 'Échec de l’enregistrement'
    return 'La saisie sera enregistrée comme brouillon'
  }

  return <section className="evaluation-workspace">
    <div className="section-head evaluation-heading"><div><span className="eyebrow">Saisie des évaluations</span><h2>Fiche de notation</h2><p>Chaque modification est enregistrée comme brouillon. Seules les fiches validées sont prises en compte dans la note finale.</p></div><div className="sheet-status-wrap"><div className="sheet-status"><span className={'status-dot ' + status.toLowerCase()} />{locked ? 'Validée' : 'Brouillon'}</div><small className={'save-state ' + saveState}>{saveStateLabel()}</small></div></div>

    <section className="evaluation-context" aria-label="Contexte de l’évaluation">
      <label className="field"><span>Filière du projet</span><select value={draft.trackCode} disabled={activeRole !== 'ADMIN'} onChange={(event) => setDraft({ ...draft, trackCode: event.target.value })}>{trackOptions.map((track) => <option key={track}>{track}</option>)}</select></label>
      <SelectData label="Projet attribué" value={draft.projectId} data={evaluationProjects} onChange={(projectId) => {
        const project = evaluationProjects.find((item) => item.id === projectId)
        const roleTypes = activeRole === 'INDUSTRY_REPRESENTATIVE' ? ['DEMO_DAY_INDUSTRY'] : EVALUATION_TYPES
        const assignedTypes = activeRole === 'ADMIN'
          ? roleTypes
          : roleTypes.filter((type) => personalAssignments.some((assignment) => assignment.projectId === projectId && assignment.evaluationType === type))
        const evaluationType = assignedTypes.includes(draft.evaluationType) ? draft.evaluationType : assignedTypes[0] || ''
        const nextPhaseType = phaseTypeForEvaluation(evaluationType)
        const currentPhase = (datasets.phases || []).find((phase) => phase.id === draft.phaseId)
        setPhaseAccess(null)
        setDraft({
          ...draft,
          projectId,
          phaseId: currentPhase?.type === nextPhaseType ? draft.phaseId : '',
          evaluationType,
          trackCode: project?.track?.code || project?.trackCode || draft.trackCode,
        })
        loadProjectEvaluations(projectId)
      }} />
      {activeRole === 'INDUSTRY_REPRESENTATIVE'
        ? <label className="field"><span>Fiche autorisée</span><input value={draft.projectId ? 'Demo Day · Industry Guest' : 'Sélectionnez un projet attribué'} readOnly /></label>
        : <label className="field"><span>Fiche affectée</span><select value={draft.evaluationType} disabled={!draft.projectId || availableEvaluationTypes.length === 0} onChange={(event) => {
          const evaluationType = event.target.value
          const nextPhaseType = phaseTypeForEvaluation(evaluationType)
          const currentPhase = (datasets.phases || []).find((phase) => phase.id === draft.phaseId)
          setPhaseAccess(null)
          setDraft({ ...draft, evaluationType, phaseId: currentPhase?.type === nextPhaseType ? draft.phaseId : '' })
        }}><option value="">Sélectionnez un projet</option>{availableEvaluationTypes.map((type) => <option key={type} value={type}>{SCORING_TEMPLATES[type]?.label || pretty(type)} · {SCORING_TEMPLATES[type]?.phase || ''}</option>)}</select></label>}
      <SelectData label={activeRole === 'INDUSTRY_REPRESENTATIVE' ? 'Phase Demo Day (FYP II)' : 'Phase'} value={draft.phaseId} data={availablePhases} onChange={(phaseId) => { setPhaseAccess(null); setDraft({ ...draft, phaseId }) }} />
      {activeRole === 'ADMIN' && <SelectData label="Évaluateur" value={draft.evaluatorId} data={evaluatorOptions} onChange={(evaluatorId) => setDraft({ ...draft, evaluatorId })} />}
    </section>

    {activeRole !== 'ADMIN' && !evaluationProjects.length && <EmptyState title="Aucun projet affecté" detail="Contactez l’administrateur FYP pour recevoir une affectation de projet et de fiche." />}

    <LegacyEvaluationHeader
      template={template}
      project={selectedProject}
      team={selectedTeam}
      phase={selectedPhase}
      evaluatorName={evaluatorDisplayName}
      trackCode={draft.trackCode}
      status={status}
    />

    {draft.phaseId && <section className={'deadline-banner ' + (phaseAccess?.allowed ? 'open' : 'closed')}>
      <div><span className="eyebrow">Fenêtre d’évaluation</span><strong>{phaseAccess?.allowed ? 'Évaluation ouverte' : 'Évaluation verrouillée'}</strong><p>{phaseAccess?.message || 'Vérification de l’échéance…'}</p></div>
      <div className="deadline-facts"><span>Échéance générale<strong>{formatDateTime(phaseAccess?.phaseDeadline || selectedPhase?.deadline)}</strong></span><span>Échéance effective<strong>{formatDateTime(effectiveDeadline)}</strong></span></div>
      {deadlineAlert && <div className={'deadline-countdown ' + deadlineAlert}><Bell size={18} /><strong>{deadlineAlert === 'half-day' ? 'Attention: moins de 12 heures restantes' : 'Attention: moins d’un jour restant'}</strong><span>Validez la fiche avant l’échéance pour que les notes soient prises en compte.</span></div>}
      {evaluationBlocked && !locked && submissionIds[sheetId] && <div className="expired-draft-warning"><strong>Brouillon expiré non comptabilisé</strong><span>La fiche n’a pas été validée avant l’échéance. Ses notes sont conservées pour traçabilité, mais elles ne participent pas au calcul final.</span></div>}
      {evaluationBlocked && phaseAccess?.reasonCode === 'PHASE_DEADLINE_EXPIRED' && activeRole !== 'ADMIN' && <form className="extension-inline-form" onSubmit={requestExtension}>
        <label className="field"><span>Motif de la demande</span><textarea required value={extensionReason} onChange={(event) => setExtensionReason(event.target.value)} placeholder="Expliquez la raison du retard" /></label>
        <label className="field"><span>Nouvelle échéance souhaitée</span><input type="datetime-local" value={requestedDeadline} onChange={(event) => setRequestedDeadline(event.target.value)} /></label>
        <button className="primary-action" disabled={extensionBusy}>{extensionBusy ? 'Envoi…' : 'Demander une prolongation'}</button>
      </form>}
    </section>}

    <div className="sheet-toolbar">
      <div className="sheet-title"><strong>{template.label}</strong><span>{template.phase} · note sur 10</span></div>
      <div className="completion-meter"><span>{completedCells}/{requiredCells} cellules</span><div><i style={{ width: (requiredCells ? Math.round((completedCells / requiredCells) * 100) : 0) + '%' }} /></div></div>
      <button className="soft-button" type="button" onClick={() => setShowFormula((value) => !value)}>{showFormula ? 'Masquer le calcul' : 'Afficher le calcul'}</button>
    </div>

    {showFormula && <FormulaPanel template={template} />}

    {requiresStudentTargets && selectedTeam && studentTargets.length === 0 && <EmptyState title="Équipe sans étudiant" detail="Ajoutez les étudiants au projet avant de remplir cette fiche individuelle." />}

    {(!requiresStudentTargets || studentTargets.length > 0) && <div className={'score-sheet ' + (locked ? 'locked' : '')}>
      {template.sections.map((section) => <ExcelScoreSection
        key={section.id}
        section={section}
        targets={section.target === 'student' ? studentTargets : [{ id: 'group', label: selectedTeam?.name || 'Note du groupe', secondary: draft.trackCode }]}
        scores={activeScores}
        onScoreChange={updateScore}
        disabled={editingDisabled}
      />)}
    </div>}

    {(!requiresStudentTargets || studentTargets.length > 0) && <ResultSummary template={template} targets={resultTargets} results={results} />}

    <section className="sheet-footer">
      <label className="field comment-field"><span>Commentaire général</span><textarea value={draft.generalComment} disabled={editingDisabled} onChange={(event) => { const generalComment = event.target.value; setDraft({ ...draft, generalComment }); queueAutoSave(activeScores, generalComment) }} /></label>
      <div className="sheet-actions"><button className="ghost-button" type="button" disabled={editingDisabled} onClick={resetSheet}>Réinitialiser</button><button className="soft-button" type="button" disabled={editingDisabled || saveState === 'saving'} onClick={() => saveSheet(false)}>Enregistrer le brouillon</button><button className="primary-action" type="button" disabled={editingDisabled || saveState === 'saving'} onClick={() => saveSheet(true)}>Valider la fiche</button></div>
    </section>

    <section className="submission-history"><div className="section-head"><div><h3>Évaluations déjà enregistrées</h3><p>Les brouillons sont visibles mais seules les fiches verrouillées sont comptabilisées.</p></div></div><DataTable rows={projectEvaluations} columns={['evaluationType','status','totalScore','completedScoreCount','locked','draftSavedAt','submittedAt','evaluator']} compact /></section>
  </section>
}
function readLocalJson(key, fallback) {
  try { return JSON.parse(localStorage.getItem(key) || '') || fallback } catch { return fallback }
}

function toStudentTarget(student, index) {
  const fullName = student.fullName || [student.firstName, student.lastName].filter(Boolean).join(' ')
  return {
    id: student.id || 'student-' + index,
    label: fullName || 'Étudiant ' + String(index + 1).padStart(2, '0'),
    secondary: student.studentNumber || '',
  }
}

function LegacyEvaluationHeader({ template, project, team, phase, evaluatorName, trackCode, status }) {
  const isIndustry = template.kind === 'demo'
  return <section className="legacy-workbook-header" aria-label="En-tête de la fiche officielle">
    <div className="legacy-workbook-brand">
      <img src={squMark} alt="Sultan Qaboos University" />
      <div><span>Sultan Qaboos University · College of Engineering</span><strong>{isIndustry ? 'FYP Demo Evaluation Sheet' : template.label + ' Evaluation Sheet'}</strong><small>{template.phase} · Official online grading form</small></div>
      <b>{status === 'SUBMITTED' || status === 'LOCKED' ? 'LOCKED' : 'DRAFT'}</b>
    </div>
    <div className="legacy-workbook-meta">
      <div><span>Track</span><strong>{project?.track?.code || project?.trackCode || trackCode || '-'}</strong></div>
      <div><span>Project No.</span><strong>{project?.projectNumber || '-'}</strong></div>
      <div className="wide"><span>Project title</span><strong>{project?.title || 'Select an assigned project'}</strong></div>
      <div><span>Team</span><strong>{team?.name || '-'}</strong></div>
      <div><span>Evaluator</span><strong>{evaluatorName}</strong></div>
      <div><span>Phase / cohort</span><strong>{phase?.name || template.phase} · {project?.academicYear || '-'}</strong></div>
      <div><span>Date</span><strong>{new Intl.DateTimeFormat(currentLocale()).format(new Date())}</strong></div>
    </div>
    <div className="legacy-scale"><strong>Marking scale</strong><span>0 Missing</span><span>2 Poor</span><span>4 Below expectations</span><span>6 Satisfactory</span><span>8 Very good</span><span>10 Excellent</span></div>
  </section>
}
function ExcelScoreSection({ section, targets, scores, onScoreChange, disabled }) {
  return <section className="sheet-section">
    <div className="sheet-section-title"><strong>{section.label}</strong><span>{section.target === 'student' ? 'Par étudiant' : 'Commun au groupe'}</span></div>
    <div className="score-grid-wrap"><table className="score-grid"><thead><tr><th className="criterion-column">Critère d’évaluation</th><th>Résultat</th><th>Coef.</th>{targets.map((target) => <th className="score-column" key={target.id}><span>{target.label}</span><small>{target.secondary}</small></th>)}</tr></thead><tbody>
      {section.criteria.map((criterion, index) => <tr key={criterion.id}><td className="criterion-cell"><span className="row-number">{String(index + 1).padStart(2, '0')}</span><strong>{criterion.label}</strong></td><td><span className="outcome-code">{criterion.outcome}</span></td><td className="weight-cell">× {criterion.weight}</td>{targets.map((target) => { const key = scoreKey(section.id, criterion.id, target.id); return <td className="score-cell" key={target.id}><input aria-label={criterion.label + ' · ' + target.label} type="number" inputMode="decimal" min="0" max="10" step="0.5" disabled={disabled} value={scores[key] ?? ''} onChange={(event) => onScoreChange(section.id, criterion.id, target.id, event.target.value)} /><span>/10</span></td> })}</tr>)}
      <tr className="subtotal-row"><td><strong>Sous-total normalisé</strong></td><td /><td>{section.criteria.reduce((sum, criterion) => sum + criterion.weight, 0)}</td>{targets.map((target) => <td key={target.id}><strong>{formatScore(sectionAverage(section, scores, target.id))}</strong><span>/10</span></td>)}</tr>
    </tbody></table></div>
  </section>
}

function FormulaPanel({ template }) {
  return <section className="formula-panel"><div><span>Formule de la fiche</span><strong>{template.shortFormula}</strong></div>{template.kind === 'presentation' && <div className="formula-breakdown"><span>Partie A: moyenne pondérée sur 5, contribution maximale 3,75</span><span>Partie B: moyenne pondérée sur 9,5, contribution maximale 6,25</span></div>}</section>
}

function ResultSummary({ template, targets, results }) {
  return <section className="result-summary"><div className="result-heading"><div><span className="eyebrow">Résultat calculé</span><h3>Note finale</h3></div><span className="formula-chip">{template.shortFormula}</span></div><div className="result-table-wrap"><table className="result-table"><thead><tr><th>{template.kind === 'demo' ? 'Groupe' : 'Étudiant'}</th>{template.kind === 'presentation' && <><th>Partie A</th><th>Contribution A</th><th>Partie B</th><th>Contribution B</th></>}<th>Note /10</th><th>Niveau</th></tr></thead><tbody>{targets.map((target) => { const result = results[target.id] || results.group; const band = performanceBand(result?.finalScore || 0); return <tr key={target.id}><td><strong>{target.label}</strong><small>{target.secondary}</small></td>{template.kind === 'presentation' && <><td>{formatScore(result.individualScore)}</td><td>{formatScore(result.contributionA)} / 3,75</td><td>{formatScore(result.groupScore)}</td><td>{formatScore(result.contributionB)} / 6,25</td></>}<td className="final-score">{formatScore(result?.finalScore)}</td><td><span className={'performance-pill ' + band.tone}>{band.label}</span></td></tr> })}</tbody></table></div></section>
}

function formatDateTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString(currentLocale())
}

function formatScore(value) {
  return Number(value || 0).toLocaleString(currentLocale(), { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function ExtensionRequestCenter({ datasets, request, notify, activeRole }) {
  const isAdmin = activeRole === 'ADMIN'
  const [rows, setRows] = useState([])
  const [filter, setFilter] = useState('')
  const [busy, setBusy] = useState(false)
  const [form, setForm] = useState({ phaseId: datasets.phases?.[0]?.id || '', reason: '', requestedDeadline: '' })
  const [decisions, setDecisions] = useState({})

  const load = useCallback(async () => {
    const query = isAdmin && filter ? '?status=' + filter : ''
    const response = await request(isAdmin ? '/api/phase-extension-requests' + query : '/api/phase-extension-requests/my')
    setRows(unwrapList(response))
  }, [filter, isAdmin, request])

  useEffect(() => {
    const timer = window.setTimeout(() => load().catch((error) => notify(error.message, 'danger')), 0)
    return () => window.clearTimeout(timer)
  }, [load, notify])

  async function create(event) {
    event.preventDefault(); setBusy(true)
    try {
      await request('/api/phase-extension-requests', {
        method: 'POST',
        body: JSON.stringify({
          phaseId: form.phaseId,
          reason: form.reason.trim(),
          requestedDeadline: form.requestedDeadline ? new Date(form.requestedDeadline).toISOString().slice(0, 19) : null,
        }),
      })
      setForm({ ...form, reason: '', requestedDeadline: '' })
      notify('Demande envoyée aux administrateurs')
      await load()
    } catch (error) { notify(error.message, 'danger') } finally { setBusy(false) }
  }

  function updateDecision(id, key, value) {
    setDecisions((current) => ({ ...current, [id]: { ...(current[id] || {}), [key]: value } }))
  }

  async function decide(row, approved) {
    const decision = decisions[row.id] || {}
    if (approved && !decision.extendedDeadline) { notify('Choisissez la nouvelle échéance.', 'danger'); return }
    setBusy(true)
    try {
      await request('/api/phase-extension-requests/' + row.id + (approved ? '/approve' : '/reject'), {
        method: 'PATCH',
        body: JSON.stringify({
          extendedDeadline: approved ? new Date(decision.extendedDeadline).toISOString().slice(0, 19) : null,
          adminComment: decision.adminComment || '',
        }),
      })
      notify(approved ? 'Prolongation approuvée' : 'Demande rejetée')
      await load()
    } catch (error) { notify(error.message, 'danger') } finally { setBusy(false) }
  }

  const action = (row) => row.status === 'PENDING' && isAdmin ? <div className="extension-review-controls">
    <input type="datetime-local" aria-label="Nouvelle échéance" value={decisions[row.id]?.extendedDeadline || ''} onChange={(event) => updateDecision(row.id, 'extendedDeadline', event.target.value)} />
    <input aria-label="Commentaire administrateur" placeholder="Commentaire" value={decisions[row.id]?.adminComment || ''} onChange={(event) => updateDecision(row.id, 'adminComment', event.target.value)} />
    <button className="mini-button" disabled={busy} onClick={() => decide(row, true)}>Approuver</button>
    <button className="mini-button danger" disabled={busy} onClick={() => decide(row, false)}>Rejeter</button>
  </div> : null

  return <section className="page-grid extension-center">
    <div className="section-head full-span"><div><span className="eyebrow">Gestion des échéances</span><h2>Demandes de prolongation</h2><p>{isAdmin ? 'Examinez les demandes et accordez une échéance personnelle.' : 'Demandez une prolongation lorsqu’une phase d’évaluation est expirée.'}</p></div>{isAdmin && <label className="mini-field"><span>Statut</span><select value={filter} onChange={(event) => setFilter(event.target.value)}><option value="">Tous</option><option value="PENDING">En attente</option><option value="APPROVED">Approuvées</option><option value="REJECTED">Rejetées</option></select></label>}</div>
    {!isAdmin && <Panel title="Nouvelle demande" accent="gold"><form className="stack-form compact" onSubmit={create}><SelectData label="Phase expirée" value={form.phaseId} data={datasets.phases || []} onChange={(phaseId) => setForm({ ...form, phaseId })} /><Field label="Nouvelle échéance souhaitée" type="datetime-local" value={form.requestedDeadline} onChange={(requestedDeadline) => setForm({ ...form, requestedDeadline })} /><Field label="Motif" textarea value={form.reason} onChange={(reason) => setForm({ ...form, reason })} /><button className="primary-action" disabled={busy || !form.phaseId || !form.reason.trim()}>Envoyer la demande</button></form></Panel>}
    <Panel title={isAdmin ? 'Demandes reçues' : 'Mes demandes'} wide><DataTable rows={rows} columns={isAdmin ? ['phase','requester','reason','requestedDeadline','status','extendedDeadline','adminComment','requestedAt'] : ['phase','reason','requestedDeadline','status','extendedDeadline','adminComment','requestedAt']} compact extraAction={isAdmin ? action : null} /></Panel>
  </section>
}
function GradingCenter({ datasets, request, reload, notify, activeRole, token }) {
  const canManageGrades = activeRole === 'ADMIN'
  const canExport = activeRole === 'ADMIN' || activeRole === 'COORDINATOR'
  const projects = useMemo(() => datasets.projects || [], [datasets.projects])
  const [projectId, setProjectId] = useState('')
  const [phaseId, setPhaseId] = useState('')
  const [projectGrades, setProjectGrades] = useState([])
  const [studentGrades, setStudentGrades] = useState([])
  const [busy, setBusy] = useState(false)
  const effectiveProjectId = projects.some((project) => project.id === projectId) ? projectId : projects[0]?.id || ''
  const selectedProject = projects.find((project) => project.id === effectiveProjectId)
  const phaseOptions = useMemo(() => (datasets.phases || []).filter((phase) =>
    !selectedProject?.academicYear || !phase.academicYear || phase.academicYear === selectedProject.academicYear
  ), [datasets.phases, selectedProject])
  const effectivePhaseId = phaseOptions.some((phase) => phase.id === phaseId) ? phaseId : phaseOptions[0]?.id || ''
  const selectedProjectGrade = projectGrades.find((grade) => (grade.phase?.id || grade.phaseId) === effectivePhaseId)

  const loadGrades = useCallback(async (nextProjectId, nextPhaseId) => {
    if (!nextProjectId) {
      setProjectGrades([])
      setStudentGrades([])
      return
    }
    try {
      const projectResponse = await request('/api/grades/project/' + nextProjectId)
      setProjectGrades(unwrapList(projectResponse))
      if (nextPhaseId) {
        setStudentGrades(unwrapList(await request('/api/grades/students/project/' + nextProjectId + '/phase/' + nextPhaseId)))
      } else {
        setStudentGrades(unwrapList(await request('/api/grades/students/project/' + nextProjectId)))
      }
    } catch (error) {
      setProjectGrades([])
      setStudentGrades([])
      notify(error.message, 'danger')
    }
  }, [notify, request])

  useEffect(() => {
    if (!effectiveProjectId) return undefined
    const timer = window.setTimeout(() => loadGrades(effectiveProjectId, effectivePhaseId), 0)
    return () => window.clearTimeout(timer)
  }, [effectivePhaseId, effectiveProjectId, loadGrades])

  async function calculate() {
    if (!effectiveProjectId || !effectivePhaseId) return
    setBusy(true)
    try {
      await request('/api/grades/calculate/project/' + effectiveProjectId + '/phase/' + effectivePhaseId, { method: 'POST' })
      await loadGrades(effectiveProjectId, effectivePhaseId)
      await reload()
      notify('Notes consolidées pour chaque étudiant')
    } catch (error) {
      notify(error.message, 'danger')
    } finally {
      setBusy(false)
    }
  }

  async function publish() {
    if (!selectedProjectGrade) return
    setBusy(true)
    try {
      await request('/api/grades/' + selectedProjectGrade.id + '/publish', { method: 'PATCH' })
      await loadGrades(effectiveProjectId, effectivePhaseId)
      notify('Résultats publiés pour cette phase')
    } catch (error) {
      notify(error.message, 'danger')
    } finally {
      setBusy(false)
    }
  }

  async function exportProject() {
    try {
      const filename = await downloadFile('/api/reports/export/project/' + effectiveProjectId, token, 'FYP_Project_Results.xlsx')
      notify('Export téléchargé: ' + filename)
    } catch (error) {
      notify(error.message, 'danger')
    }
  }

  return <section className="page-grid grading-center">
    <div className="section-head full-span"><div><span className="eyebrow">Consolidation officielle</span><h2>{canManageGrades ? 'Calcul et publication des notes' : 'Résultats consolidés'}</h2><p>La moyenne utilise exclusivement les fiches validées et verrouillées. Les brouillons et fiches expirées restent hors calcul.</p></div></div>
    <Panel title="Périmètre des résultats" accent="green"><div className="form-grid two"><SelectData label="Projet" value={effectiveProjectId} data={projects} onChange={setProjectId} /><SelectData label="Phase" value={effectivePhaseId} data={phaseOptions} onChange={setPhaseId} /></div><div className="action-row">{canManageGrades && <button className="primary-action" disabled={busy || !effectiveProjectId || !effectivePhaseId} onClick={calculate}>{busy ? 'Calcul…' : 'Calculer / recalculer'}</button>}{canManageGrades && selectedProjectGrade && !selectedProjectGrade.published && <button className="soft-button" disabled={busy} onClick={publish}>Publier la phase</button>}{canExport && <button className="soft-button icon-text" disabled={!effectiveProjectId} onClick={exportProject}><Download size={16} />Exporter le projet</button>}</div></Panel>
    <Panel title="Règle de consolidation" accent="gold"><p className="muted">Chaque type est d’abord moyenné entre tous les évaluateurs affectés, puis pondéré selon les règles de la phase. Une valeur 0 validée est une note réelle et reste incluse.</p><div className="tag-list">{EVALUATION_TYPES.map((type) => <span key={type}>{SCORING_TEMPLATES[type]?.label || pretty(type)}</span>)}</div></Panel>
    <Panel title="Résultats individuels" subtitle={selectedProject ? selectedProject.projectNumber + ' · ' + selectedProject.title : ''} wide>{!projects.length
      ? <EmptyState title="Aucun projet accessible" detail="Les projets apparaissent selon les affectations du compte connecté." />
      : studentGrades.length
        ? <StudentGradeTable rows={studentGrades} />
        : <EmptyState title="Aucun résultat calculé" detail={canManageGrades ? 'Validez toutes les fiches requises, puis lancez le calcul.' : 'Les résultats apparaîtront après calcul et publication.'} />}</Panel>
    {projectGrades.length > 0 && <Panel title="Synthèse du projet par phase" wide><DataTable rows={projectGrades} columns={['phaseType','finalScore','published']} compact /></Panel>}
  </section>
}

function StudentGradeTable({ rows }) {
  const score = (value) => value === null || value === undefined ? '-' : formatScore(value)
  return <div className="table-wrap student-grade-table"><table><thead><tr><th>Student ID</th><th>Student name</th><th>Supervisor</th><th>Report</th><th>Presentation</th><th>Demo Day</th><th>Final /10</th><th>Status</th></tr></thead><tbody>{rows.map((row) => <tr key={row.id}><td><strong>{row.student?.studentNumber || '-'}</strong></td><td>{row.student?.fullName || '-'}</td><td>{score(row.supervisorScore)}</td><td>{score(row.reportScore)}</td><td>{score(row.oralScore)}</td><td>{score(row.demoScore)}</td><td className="final-score">{score(row.finalScore)}</td><td><StatusPill value={row.published ? 'PUBLISHED' : 'INTERNAL'} /></td></tr>)}</tbody></table></div>
}

function ReportCenter({ datasets, request, reload, notify, token }) {
  const projects = datasets.projects || []
  const phases = datasets.phases || []
  const [projectId, setProjectId] = useState('')
  const [phaseId, setPhaseId] = useState('')
  const [completenessSnapshot, setCompletenessSnapshot] = useState({ phaseId: '', rows: [] })
  const [busy, setBusy] = useState(false)
  const effectiveProjectId = projects.some((project) => project.id === projectId) ? projectId : projects[0]?.id || ''
  const effectivePhaseId = phases.some((phase) => phase.id === phaseId) ? phaseId : phases[0]?.id || ''
  const reports = datasets.reports || []
  const completeness = completenessSnapshot.phaseId === effectivePhaseId ? completenessSnapshot.rows : []

  useEffect(() => {
    if (!effectivePhaseId) return undefined
    let active = true
    request('/api/reports/completeness/phase/' + effectivePhaseId)
      .then((response) => { if (active) setCompletenessSnapshot({ phaseId: effectivePhaseId, rows: unwrapList(response) }) })
      .catch(() => { if (active) setCompletenessSnapshot({ phaseId: effectivePhaseId, rows: [] }) })
    return () => { active = false }
  }, [effectivePhaseId, request])

  async function generate(finalReport) {
    if (!effectiveProjectId || (!finalReport && !effectivePhaseId)) return
    setBusy(true)
    try {
      const path = finalReport ? '/api/reports/project/' + effectiveProjectId + '/final' : '/api/reports/project/' + effectiveProjectId + '/phase/' + effectivePhaseId
      await request(path, { method: 'POST' })

      await reload()
      notify('Rapport Excel généré et archivé')
    } catch (error) {
      notify(error.message, 'danger')
    } finally {
      setBusy(false)
    }
  }

  async function downloadPhase() {
    try {
      const filename = await downloadFile('/api/reports/export/phase/' + effectivePhaseId, token, 'Final_Evaluation_Summary.xlsx')
      notify('Synthèse téléchargée: ' + filename)
    } catch (error) { notify(error.message, 'danger') }
  }

  async function downloadProject() {
    try {
      const filename = await downloadFile('/api/reports/export/project/' + effectiveProjectId, token, 'FYP_Project_Results.xlsx')
      notify('Rapport projet téléchargé: ' + filename)
    } catch (error) { notify(error.message, 'danger') }
  }

  async function send(id) {
    try {
      await request('/api/reports/' + id + '/send', { method: 'POST' })

      await reload()
      notify('Rapport envoyé par e-mail')
    } catch (error) { notify(error.message, 'danger') }
  }

  return <section className="page-grid report-center">
    <div className="section-head full-span"><div><span className="eyebrow">Remplacement du script MATLAB</span><h2>Consolidation et exports Excel</h2><p>Contrôlez les fiches manquantes, puis exportez la synthèse finale compatible avec l’ancien processus.</p></div></div>
    <Panel title="Génération et téléchargement" accent="green"><div className="form-grid two"><SelectData label="Projet" value={effectiveProjectId} data={projects} onChange={setProjectId} /><SelectData label="Phase" value={effectivePhaseId} data={phases} onChange={setPhaseId} /></div><div className="action-row"><button className="soft-button icon-text" disabled={!effectivePhaseId} onClick={downloadPhase}><FileSpreadsheet size={16} />Final Evaluation Summary</button><button className="soft-button icon-text" disabled={!effectiveProjectId} onClick={downloadProject}><Download size={16} />Export projet</button><button className="primary-action" disabled={busy || !effectiveProjectId} onClick={() => generate(true)}>Archiver le rapport final</button></div></Panel>
    <Panel title="Contenu de l’export" accent="gold"><p className="muted">Le classeur contient la synthèse historique à 11 colonnes, le détail par étudiant, les notes de chaque évaluateur, les fiches manquantes et la piste d’audit.</p><div className="tag-list"><span>LEGACY_SUMMARY</span><span>FINAL_SUMMARY</span><span>EVALUATOR_DETAILS</span><span>MISSING_FORMS</span><span>AUDIT_TRAIL</span></div></Panel>
    <Panel title="Complétude des évaluations" wide>{completeness.length ? <DataTable rows={completeness} columns={['projectNumber','projectTitle','evaluationType','evaluatorName','evaluatorEmail','status']} compact /> : <EmptyState title="Aucune affectation pour cette phase" detail="Créez les affectations ou choisissez une autre phase." />}</Panel>
    <Panel title="Archive des rapports" wide>{reports.length ? <DataTable rows={reports} columns={['project','phase','status','recipientEmail','generatedAt','sentAt']} compact extraAction={(row) => row.status !== 'SENT' && <button className="mini-button" onClick={() => send(row.id)}>Envoyer</button>} /> : <EmptyState title="Aucun rapport archivé" detail="Le téléchargement direct reste disponible sans créer d’archive." />}</Panel>
  </section>
}

function ApiConsole({ request, notify }) {
  const [method, setMethod] = useState('GET'), [path, setPath] = useState('/api/tracks'), [body, setBody] = useState(''), [result, setResult] = useState('')
  async function run() { try { const res = await request(path, { method, ...(method !== 'GET' && body ? { body } : {}) }); setResult(JSON.stringify(res, null, 2)); notify('API request completed') } catch (error) { setResult(error.message); notify(error.message, 'danger') } }
  return <section className="page-grid"><div className="section-head full-span"><div><h2>API console</h2><p>Test Spring Boot endpoints from inside the React app.</p></div></div><Panel title="Request builder" accent="green"><div className="form-grid two"><label className="field"><span>Method</span><select value={method} onChange={(e) => setMethod(e.target.value)}>{['GET','POST','PUT','PATCH','DELETE'].map((m) => <option key={m}>{m}</option>)}</select></label><Field label="Path" value={path} onChange={setPath} /></div><textarea className="csv-box" value={body} onChange={(e) => setBody(e.target.value)} placeholder='{"code":"NEW","name":"New Track"}' /><button className="primary-action" onClick={run}>Run request</button></Panel><Panel title="Response" wide><pre className="response-box">{result || 'Response will appear here.'}</pre></Panel></section>
}
function DynamicField({ field, value, onChange, datasets }) {
  if (field.type === 'select') return <label className="field"><span>{field.label}</span><select value={value ?? ''} onChange={(e) => onChange(e.target.value)}><option value="">Select</option>{field.options.map((o) => <option key={o} value={o}>{pretty(o)}</option>)}</select></label>
  if (field.type === 'selectData') {
    let data = datasets[field.source] || []
    if (field.filterRole) data = data.filter((item) => item.role === field.filterRole || item.user?.role === field.filterRole)
    if (field.filterRoles) data = data.filter((item) => field.filterRoles.includes(item.role || item.user?.role))
    return <SelectData label={field.label} value={value} data={data} onChange={onChange} />
  }
  if (field.type === 'multiData') return <label className="field"><span>{field.label}</span><select multiple value={Array.isArray(value) ? value : []} onChange={(e) => onChange(Array.from(e.target.selectedOptions).map((o) => o.value))}>{(datasets[field.source] || []).map((item) => <option key={item.id} value={item.id}>{itemName(item.user || item)}</option>)}</select></label>
  if (field.type === 'checkbox') return <label className="check-field"><input type="checkbox" checked={Boolean(value)} onChange={(e) => onChange(e.target.checked)} /><span>{field.label}</span></label>
  return <Field label={field.label} type={field.type || 'text'} textarea={field.type === 'textarea'} value={value ?? ''} onChange={onChange} placeholder={field.placeholder} />
}

function Field({ label, value, onChange, type = 'text', textarea = false, placeholder = '' }) {
  return <label className="field"><span>{label}</span>{textarea ? <textarea value={value} onChange={(e) => onChange(e.target.value)} placeholder={placeholder} /> : <input type={type} value={value} onChange={(e) => onChange(e.target.value)} placeholder={placeholder} />}</label>
}

function SelectData({ label, value, data, onChange }) {
  return <label className="field"><span>{label}</span><select value={value || ''} onChange={(e) => onChange(e.target.value)}><option value="">Select</option>{data.map((item) => <option key={item.id} value={item.id}>{itemName(item.user || item)}</option>)}</select></label>
}

function Panel({ title, subtitle, children, accent = 'blue', wide = false, className = '' }) {
  return <section className={'panel accent-' + accent + (wide ? ' wide' : '') + (className ? ' ' + className : '')}><div className="panel-heading"><div><h3>{title}</h3>{subtitle && <p>{subtitle}</p>}</div></div>{children}</section>
}

function DataTable({ rows = [], columns = [], onEdit, onDelete, compact = false, extraAction }) {
  if (!rows.length) return <EmptyState />
  return <div className={'table-wrap ' + (compact ? 'compact' : '')}><table><thead><tr>{columns.map((column) => <th key={column}>{pretty(column)}</th>)}{(onEdit || onDelete || extraAction) && <th>Actions</th>}</tr></thead><tbody>{rows.map((row, index) => <tr key={row.id || index}>{columns.map((column) => <td key={column} data-label={pretty(column)}>{renderCell(row[column], column)}</td>)}{(onEdit || onDelete || extraAction) && <td className="row-actions" data-label="Actions">{extraAction?.(row)}{onEdit && <button type="button" className="mini-button icon-text" onClick={() => onEdit(row)} title="Modifier"><Pencil size={14} />Modifier</button>}{onDelete && <button type="button" className="mini-button danger icon-text" onClick={() => onDelete(row)} title="Supprimer"><Trash2 size={14} />Supprimer</button>}</td>}</tr>)}</tbody></table></div>
}

function renderCell(value, column) {
  if (column.toLowerCase().includes('status') || column === 'role' || column === 'phaseType' || column === 'evaluationType') return <StatusPill value={value} />
  if (Array.isArray(value)) return value.length ? value.map((item) => itemName(item.user || item)).join(', ') : '-'
  if (typeof value === 'boolean') return <StatusPill value={value ? 'YES' : 'NO'} />
  if (value && typeof value === 'object') return itemName(value.user || value)
  if ((String(column).toLowerCase().includes('at') || String(column).toLowerCase().includes('deadline') || String(column).toLowerCase().includes('date')) && value) return formatDateTime(value)
  return value ?? '-'
}

function LogoLockup({ compact = false }) {
  return <div className={'logo-lockup ' + (compact ? 'compact' : '')}><img src={squLogo} alt="Sultan Qaboos University" /><div><strong>Sultan Qaboos University</strong><span>Online FYP Grading Platform</span></div></div>
}

function StatusPill({ value }) {
  return <span className={'status-pill ' + String(value || 'unknown').toLowerCase()}>{pretty(value)}</span>
}

function EmptyState({ title = 'No data yet', detail = 'Create records or run the SQL seed script to populate this view.' }) {
  return <div className="empty-state"><div className="empty-icon"><CircleAlert size={20} /></div><h3>{title}</h3><p>{detail}</p></div>
}


function Toast({ message, type }) {
  return <div className={'toast ' + type} role="status" aria-live="polite">{type === 'danger' ? <CircleAlert size={19} /> : <CheckCircle2 size={19} />}<span>{message}</span></div>
}

export default App
