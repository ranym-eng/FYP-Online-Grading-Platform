import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { ArrowRight, Bell, CheckCheck, CheckCircle2, ChevronLeft, ChevronRight, CircleAlert, Download, Eye, EyeOff, FileSpreadsheet, KeyRound, LogOut, Mail, MailOpen, Menu, PanelLeftClose, PanelLeftOpen, Pencil, Plus, RefreshCw, Search, Send, ShieldCheck, Trash2, X } from 'lucide-react'
import squLogo from './assets/Sultan_Qaboos_University_Logo.png'
import squMark from './assets/sultan-qaboos-university-logo-png_seeklogo-271991.png'
import { apiRequest, downloadFile, itemName, pretty, unwrapList } from './api.js'
import { EVALUATION_TYPES, ROLES, actorTemplates, resourceConfigs, views } from './config.js'
import { SCORING_TEMPLATES, calculateTemplate, normalizeScore, performanceBand, scoreKey, sectionAverage } from './gradingTemplates.js'
import { currentLocale, getInitialLanguage, setLanguagePreference, translateText, useAutoTranslate } from './i18n.js'
import { AppSkeleton, CalendarView, ErrorState, GlobalSearch, MetricIcon, ProfileDrawer, ThemeToggle, ViewIcon } from './workspaceUi.jsx'
import './App.css'
import './design-system.css'

const homeViewByRole = {
  ADMIN: 'dashboard',
  SUPERVISOR: 'dashboard',
  REPORT_EVALUATOR: 'dashboard',
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

function evaluationTypesForRole(role) {
  if (role === 'SUPERVISOR') return ['SUPERVISOR_PHASE_I', 'SUPERVISOR_PHASE_II']
  if (role === 'REPORT_EVALUATOR') return ['REPORT_PHASE_I', 'REPORT_PHASE_II']
  if (role === 'FACULTY_EVALUATOR') return ['ORAL_PHASE_I', 'ORAL_PHASE_II']
  if (role === 'INDUSTRY_REPRESENTATIVE') return ['DEMO_DAY_INDUSTRY']
  return EVALUATION_TYPES
}

function initialTheme() {
  const saved = localStorage.getItem('fyp-theme')
  if (saved === 'light' || saved === 'dark') return saved
  return 'light'
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

  if (!session) return <AuthScreen onSession={openWorkspace} notify={notify} toast={toast} language={language} setLanguage={setLanguage} />
  return <Shell session={session} activeView={activeView} setActiveView={setActiveView} onLogout={() => setSession(null)} notify={notify} toast={toast} language={language} setLanguage={setLanguage} theme={theme} setTheme={setTheme} />
}

function LanguageSwitcher({ language, setLanguage }) {
  return <div className="language-switcher" data-no-translate aria-label="Language">
    {['fr', 'en'].map((code) => <button key={code} type="button" className={language === code ? 'active' : ''} onClick={() => { setLanguagePreference(code); setLanguage(code) }} aria-pressed={language === code}>{code.toUpperCase()}</button>)}
  </div>
}

function AuthScreen({ onSession, notify, toast, language, setLanguage }) {
  const initialUrl = new URL(window.location.href)
  const initialInvitationToken = initialUrl.searchParams.get('industryInvitation') || ''
  const initialResetToken = initialUrl.searchParams.get('resetToken') || ''
  const [busy, setBusy] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  const [mode, setMode] = useState(() => initialInvitationToken ? 'activate' : initialResetToken ? 'reset' : 'login')
  const [loginForm, setLoginForm] = useState({ email: '', password: '' })
  const [recovery, setRecovery] = useState({ email: '', token: initialResetToken, newPassword: '', confirmPassword: '' })
  const [recoverySent, setRecoverySent] = useState(false)
  const [activation, setActivation] = useState(() => ({ token: initialInvitationToken, newPassword: '', confirmPassword: '' }))
  const [ssoConfig, setSsoConfig] = useState({ enabled: false, loginUrl: null, localInternalLoginEnabled: false })
  const authLinkHandled = useRef(false)

  useEffect(() => {
    let active = true
    apiRequest('/api/auth/sso/config').then((response) => {
      if (!active) return
      const next = response.data || { enabled: false, loginUrl: null, localInternalLoginEnabled: false }
      setSsoConfig(next)
    }).catch(() => {
      if (active) setSsoConfig({ enabled: false, loginUrl: null, localInternalLoginEnabled: false })
    })
    return () => { active = false }
  }, [])

  useEffect(() => {
    if (authLinkHandled.current) return
    const url = new URL(window.location.href)
    const invitationToken = url.searchParams.get('industryInvitation')
    const resetToken = url.searchParams.get('resetToken')
    const ssoCode = url.searchParams.get('ssoCode')
    const ssoError = url.searchParams.get('ssoError')
    if (!invitationToken && !resetToken && !ssoCode && !ssoError) return
    authLinkHandled.current = true
    ;['industryInvitation', 'resetToken', 'ssoCode', 'ssoError'].forEach((name) => url.searchParams.delete(name))
    window.history.replaceState({}, document.title, url.pathname + url.search + url.hash)
    if (invitationToken || resetToken) {
      return
    }
    if (ssoError) {
      notify('Connexion SQU refusée : ' + pretty(ssoError), 'danger')
      return
    }
    apiRequest('/api/auth/sso/exchange', { method: 'POST', body: JSON.stringify({ code: ssoCode }) })
      .then((response) => onSession(response.data))
      .catch((error) => notify(error.message, 'danger'))
      .finally(() => setBusy(false))
  }, [notify, onSession])

  async function login(event) {
    event.preventDefault()
    if (ssoConfig.enabled && !ssoConfig.localInternalLoginEnabled && loginForm.email.toLowerCase().endsWith('@squ.edu.om')) {
      startSso()
      return
    }
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
      setRecoverySent(true)
      notify('Si ce compte est éligible, un lien de réinitialisation vient d’être envoyé.')
    } catch (error) {
      notify(error.message, 'danger')
    } finally {
      setBusy(false)
    }
  }

  async function resetPassword(event) {
    event.preventDefault()
    if (recovery.newPassword !== recovery.confirmPassword) {
      notify('Les deux mots de passe ne correspondent pas.', 'danger')
      return
    }
    setBusy(true)
    try {
      await apiRequest('/api/auth/reset-password', { method: 'POST', body: JSON.stringify({ token: recovery.token, newPassword: recovery.newPassword }) })
      setLoginForm({ email: recovery.email, password: '' })
      setRecovery({ email: '', token: '', newPassword: '', confirmPassword: '' })
      setRecoverySent(false)
      setMode('login')
      notify('Mot de passe réinitialisé. Vous pouvez vous connecter.')
    } catch (error) {
      notify(error.message, 'danger')
    } finally {
      setBusy(false)
    }
  }

  async function activateIndustryGuest(event) {
    event.preventDefault()
    if (activation.newPassword !== activation.confirmPassword) {
      notify('Les deux mots de passe ne correspondent pas.', 'danger')
      return
    }
    setBusy(true)
    try {
      const result = await apiRequest('/api/auth/industry/activate', {
        method: 'POST',
        body: JSON.stringify({ token: activation.token, newPassword: activation.newPassword }),
      })
      onSession(result.data)
    } catch (error) {
      notify(error.message, 'danger')
    } finally {
      setBusy(false)
    }
  }

  function startSso() {
    if (!ssoConfig.enabled || !ssoConfig.loginUrl) {
      notify('Le SSO SQU doit être configuré par le service informatique.', 'danger')
      return
    }
    window.location.assign(ssoConfig.loginUrl)
  }

  const heading = mode === 'login' ? 'Connexion' : mode === 'forgot' ? 'Récupérer le compte' : mode === 'activate' ? 'Activer votre invitation' : 'Créer un nouveau mot de passe'
  const description = mode === 'login'
    ? 'Accédez à votre espace de travail FYP.'
    : mode === 'forgot'
      ? 'Saisissez l’adresse du compte importé par l’administration.'
      : mode === 'activate'
        ? 'Choisissez le mot de passe associé à votre invitation temporaire.'
        : 'Le lien est valide une seule fois. Choisissez maintenant votre nouveau mot de passe.'

  return <main className="auth-screen">
    <section className="auth-visual">
      <div className="auth-brand-line"><LogoLockup /><LanguageSwitcher language={language} setLanguage={setLanguage} /></div>
      <div className="auth-copy page-enter">
        <div className="auth-kicker"><span>College of Engineering</span><i /></div>
        <h1>Final Year<br /><em>Grading</em></h1>
        <p>Une évaluation claire, du premier rapport au résultat final.</p>
      </div>
      <div className="auth-stage-line"><span>FYP I</span><i /><span>FYP II</span><i /><span>Demo Day</span></div>
    </section>
    <div className="auth-panel-shell">
      <section className="auth-panel page-enter">
        <div className="brand-badge"><img src={squMark} alt="SQU" /><div><strong>Sultan Qaboos University</strong><small>Final Year Grading Platform</small></div></div>
        <form className="stack-form auth-form" onSubmit={mode === 'login' ? login : mode === 'forgot' ? requestReset : mode === 'activate' ? activateIndustryGuest : resetPassword}>
          <div className="auth-form-heading"><h2>{heading}</h2>{description && <p>{description}</p>}</div>
          {mode === 'login' && <>
            <AuthField icon={Mail} label="Adresse e-mail" type="email" required value={loginForm.email} onChange={(email) => setLoginForm({ ...loginForm, email })} autoComplete="username" />
            <AuthField icon={ShieldCheck} label="Mot de passe" type={showPassword ? 'text' : 'password'} required={!ssoConfig.enabled || ssoConfig.localInternalLoginEnabled || !loginForm.email.toLowerCase().endsWith('@squ.edu.om')} value={loginForm.password} onChange={(password) => setLoginForm({ ...loginForm, password })} autoComplete="current-password" action={<button type="button" onClick={() => setShowPassword((value) => !value)} title={showPassword ? 'Masquer le mot de passe' : 'Afficher le mot de passe'} aria-label={showPassword ? 'Masquer le mot de passe' : 'Afficher le mot de passe'}>{showPassword ? <EyeOff size={18} /> : <Eye size={18} />}</button>} />
          </>}
          {mode === 'forgot' && !recoverySent && <AuthField icon={Mail} label="Adresse e-mail" type="email" required value={recovery.email} onChange={(email) => setRecovery({ ...recovery, email })} autoComplete="email" />}
          {mode === 'forgot' && recoverySent && <div className="auth-success-state"><span><CheckCircle2 size={22} /></span><div><strong>Consultez votre boîte e-mail</strong><p>Un lien sera envoyé si le compte est éligible.</p></div></div>}
          {mode === 'reset' && <><AuthField icon={KeyRound} label="Nouveau mot de passe" type="password" minLength="8" value={recovery.newPassword} onChange={(newPassword) => setRecovery({ ...recovery, newPassword })} autoComplete="new-password" /><AuthField icon={ShieldCheck} label="Confirmer le mot de passe" type="password" minLength="8" value={recovery.confirmPassword} onChange={(confirmPassword) => setRecovery({ ...recovery, confirmPassword })} autoComplete="new-password" /></>}
          {mode === 'activate' && <><AuthField icon={KeyRound} label="Nouveau mot de passe" type="password" minLength="8" value={activation.newPassword} onChange={(newPassword) => setActivation({ ...activation, newPassword })} autoComplete="new-password" /><AuthField icon={ShieldCheck} label="Confirmer le mot de passe" type="password" minLength="8" value={activation.confirmPassword} onChange={(confirmPassword) => setActivation({ ...activation, confirmPassword })} autoComplete="new-password" /></>}
          {!(mode === 'forgot' && recoverySent) && <button className="primary-action auth-submit" disabled={busy}>{busy ? <><span className="button-spinner" />Traitement…</> : <>{mode === 'login' ? 'Continuer' : mode === 'forgot' ? 'Envoyer le lien' : mode === 'activate' ? 'Activer et ouvrir mon espace' : 'Enregistrer le mot de passe'}<ArrowRight size={18} /></>}</button>}
          {mode === 'forgot' && recoverySent && <button className="soft-button auth-submit" type="button" onClick={() => setRecoverySent(false)}>Utiliser une autre adresse</button>}
          {mode === 'login' ? <button className="auth-link" type="button" onClick={() => { setRecovery((current) => ({ ...current, email: loginForm.email })); setRecoverySent(false); setMode('forgot') }}>Mot de passe oublié ?</button> : <button className="auth-link" type="button" onClick={() => { setRecoverySent(false); setMode('login') }}>Retour à la connexion</button>}
        </form>
        <div className="auth-trust"><ShieldCheck size={16} /><span>Accès sécurisé et réservé aux utilisateurs autorisés</span></div>
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
  const [notificationOpen, setNotificationOpen] = useState(false)
  const [searchTarget, setSearchTarget] = useState(null)
  const notificationMenuRef = useRef(null)
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
    const commonEndpoints = [
      ['tracks', '/api/tracks'],
      ['projects', '/api/projects'],
      ['projectAssignments', '/api/projects/my-evaluation-assignments'],
      ['teams', '/api/teams'],
      ['phases', '/api/phases'],
      ['forms', '/api/evaluation-forms'],
    ]
    const evaluatorEndpoints = ['SUPERVISOR', 'REPORT_EVALUATOR', 'FACULTY_EVALUATOR', 'INDUSTRY_REPRESENTATIVE'].includes(activeRole)
      ? [['evaluators', '/api/evaluators/me']]
      : []
    const managementEndpoints = activeRole === 'ADMIN'
      ? [
          ['users', '/api/users'],
          ['students', '/api/students'],
          ['evaluators', '/api/evaluators'],
          ['reports', '/api/reports'],
          ['notifications', '/api/notifications'],
          ['audit', '/api/audit'],
        ]
      : activeRole === 'COORDINATOR'
        ? [['reports', '/api/reports'], ['audit', '/api/audit']]
        : []
    const endpoints = [...commonEndpoints, ...evaluatorEndpoints, ...managementEndpoints]
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
  }, [activeRole, request])

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
  useEffect(() => {
    if (!notificationOpen) return undefined
    const closeOutside = (event) => {
      if (!notificationMenuRef.current?.contains(event.target)) setNotificationOpen(false)
    }
    const closeOnEscape = (event) => {
      if (event.key === 'Escape') setNotificationOpen(false)
    }
    document.addEventListener('pointerdown', closeOutside)
    window.addEventListener('keydown', closeOnEscape)
    return () => {
      document.removeEventListener('pointerdown', closeOutside)
      window.removeEventListener('keydown', closeOnEscape)
    }
  }, [notificationOpen])

  function navigate(view, target = null) {
    setSearchTarget(target ? { ...target, nonce: Date.now() } : null)
    setActiveView(view)
    setSidebarOpen(false)
    setNotificationOpen(false)
  }

  let activeContent = <ErrorState notFound message="Le module demandé n’est pas disponible pour cette session." onRetry={() => navigate('dashboard')} />
  if (activeView === 'dashboard') activeContent = <Dashboard datasets={datasets} request={request} activeRole={activeRole} session={session} notify={notify} setActiveView={navigate} allowedViews={allowedViews} />
  if (activeView === 'calendar') activeContent = <CalendarView phases={datasets.phases || []} />
  if (activeView === 'notifications') activeContent = <NotificationCenter notifications={personalNotifications} request={request} reload={loadPersonalNotifications} notify={notify} setActiveView={navigate} allowedViews={allowedViews} />
  if (activeView === 'imports') activeContent = <ImportCenter request={request} notify={notify} reload={loadCore} />
  if (activeView === 'crud') activeContent = <CrudStudio datasets={datasets} request={request} reload={loadCore} notify={notify} searchTarget={searchTarget} />
  if (activeView === 'evaluations') activeContent = <EvaluationStudio datasets={datasets} request={request} notify={notify} activeRole={activeRole} session={session} initialProjectId={searchTarget?.projectId} />
  if (activeView === 'extensions') activeContent = <ExtensionRequestCenter datasets={datasets} request={request} notify={notify} activeRole={activeRole} />
  if (activeView === 'grading') activeContent = <GradingCenter datasets={datasets} request={request} reload={loadCore} notify={notify} activeRole={activeRole} token={session.token} initialProjectId={searchTarget?.projectId} />
  if (activeView === 'reports') activeContent = <ReportCenter datasets={datasets} request={request} reload={loadCore} notify={notify} token={session.token} initialProjectId={searchTarget?.projectId} />

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
        <div><span className="sidebar-kicker"><i />Espace actif</span><strong>{actorTemplates[activeRole]?.title}</strong></div>
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
          <div className="notification-menu" ref={notificationMenuRef}>
            <button type="button" className={'notification-button ' + (notificationOpen ? 'active' : '')} onClick={() => setNotificationOpen((current) => !current)} title="Notifications" aria-label="Notifications" aria-expanded={notificationOpen} aria-haspopup="dialog"><Bell size={19} />{unreadCount > 0 && <span className="notification-badge">{Math.min(99, unreadCount)}</span>}</button>
            {notificationOpen && <NotificationPopover notifications={personalNotifications} request={request} reload={loadPersonalNotifications} notify={notify} onNavigate={navigate} onClose={() => setNotificationOpen(false)} allowedViews={allowedViews} />}
          </div>
          <ThemeToggle theme={theme} setTheme={setTheme} compact />
          <LanguageSwitcher language={language} setLanguage={setLanguage} />
          <button type="button" className="user-chip" onClick={() => setProfileOpen(true)}><span>{String(session.fullName || session.email || 'S').slice(0, 1).toUpperCase()}</span><div><strong>{session.fullName || 'Utilisateur SQU'}</strong><small>{pretty(activeRole)}</small></div></button>
        </div>
      </header>
      {loading && !initialLoading && <div className="loading-bar"><span /></div>}
      <section className="workspace-content" key={activeView + '-' + (searchTarget?.nonce || 'default')}>
        {initialLoading ? <AppSkeleton /> : error ? <ErrorState message={error} onRetry={loadCore} /> : activeContent}
      </section>
    </main>
    <ProfileDrawer open={profileOpen} onClose={() => setProfileOpen(false)} session={session} request={request} notify={notify} theme={theme} setTheme={setTheme} language={language} setLanguage={setLanguage} onLogout={onLogout} roleLabel={actorTemplates[activeRole]?.title || pretty(activeRole)} />
    {toast && <Toast {...toast} />}
  </div>
}
function NotificationPopover({ notifications, request, reload, notify, onNavigate, onClose, allowedViews }) {
  const allowedViewIds = new Set(allowedViews.map((view) => view.id))
  const recent = notifications.slice(0, 6)
  const unreadCount = notifications.filter((item) => !item.readAt).length

  async function markAllRead() {
    try {
      await request('/api/notifications/me/read-all', { method: 'PATCH' })
      await reload()
    } catch (error) {
      notify(error.message, 'danger')
    }
  }

  async function openItem(notification) {
    try {
      if (!notification.readAt) {
        await request('/api/notifications/' + notification.id + '/read', { method: 'PATCH' })
        await reload()
      }
      const destination = notification.actionView && allowedViewIds.has(notification.actionView)
        ? notification.actionView
        : 'notifications'
      onClose()
      onNavigate(destination)
    } catch (error) {
      notify(error.message, 'danger')
    }
  }

  return <section className="notification-popover" role="dialog" aria-label="Notifications récentes">
    <header><div><h2>Notifications</h2><span>{unreadCount ? unreadCount + ' non lue' + (unreadCount === 1 ? '' : 's') : 'Vous êtes à jour'}</span></div>{unreadCount > 0 && <button type="button" onClick={markAllRead} title="Tout marquer comme lu" aria-label="Tout marquer comme lu"><CheckCheck size={18} /></button>}</header>
    <div className="notification-preview-list">
      {!recent.length && <div className="notification-popover-empty"><Bell size={24} /><strong>Aucune notification</strong><span>Les alertes importantes apparaîtront ici.</span></div>}
      {recent.map((notification) => <button type="button" key={notification.id} className={'notification-preview ' + (notification.readAt ? 'read' : 'unread')} onClick={() => openItem(notification)}>
        <span className={'notification-preview-icon ' + String(notification.severity || 'INFO').toLowerCase()}><Bell size={17} /></span>
        <span className="notification-preview-copy"><strong>{notification.subject}</strong><small>{notificationDisplayBody(notification)}</small><time>{formatDateTime(notification.createdAt || notification.sentAt)}</time></span>
        {!notification.readAt && <i aria-label="Non lue" />}
      </button>)}
    </div>
    <footer><button type="button" onClick={() => onNavigate('notifications')}>Voir toutes les notifications<ArrowRight size={16} /></button></footer>
  </section>
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
      <div><h2>Notifications</h2></div>
      <div className="notification-summary"><strong>{unreadCount}</strong><span>non lue{unreadCount === 1 ? '' : 's'}</span><button type="button" className="soft-button" onClick={markAllRead} disabled={!unreadCount}><CheckCheck size={17} />Tout marquer comme lu</button></div>
    </div>
    {!notifications.length && <EmptyState title="Aucune notification" detail="Vos prochaines alertes apparaîtront ici." />}
    <div className="notification-list">
      {notifications.map((notification) => <article key={notification.id} className={'notification-item ' + (notification.readAt ? 'read ' : 'unread ') + (notification.severity || 'INFO').toLowerCase()}>
        <div className="notification-symbol"><Bell size={19} /></div>
        <div className="notification-copy"><div className="notification-meta"><span>{pretty(notification.category || 'NOTIFICATION')}</span><time>{formatDateTime(notification.createdAt || notification.sentAt)}</time></div><h3>{notification.subject}</h3><p>{notificationDisplayBody(notification)}</p></div>
        <div className="notification-actions">{!notification.readAt && <button type="button" className="icon-button" title="Marquer comme lue" aria-label="Marquer comme lue" onClick={() => markRead(notification)}><MailOpen size={18} /></button>}{notification.actionView && allowedViewIds.has(notification.actionView) && <button type="button" className="mini-button" onClick={() => openNotification(notification)}>Ouvrir</button>}</div>
      </article>)}
    </div>
  </section>
}

function notificationDisplayBody(notification) {
  const body = String(notification?.body || '')
  if (/industryInvitation=/i.test(body)) {
    return 'Votre invitation Industry Guest est prête. Consultez votre e-mail pour activer votre accès temporaire.'
  }
  if (/resetToken=/i.test(body)) {
    return 'Une réinitialisation de mot de passe a été demandée. Consultez votre e-mail pour continuer.'
  }
  return body
}

function Dashboard({ datasets, request, activeRole, session, notify, setActiveView, allowedViews }) {
  const [summary, setSummary] = useState(null)
  const [pending, setPending] = useState([])
  const [detailModal, setDetailModal] = useState(null)
  const [dashboardNow] = useState(() => Date.now())
  const allowedViewIds = new Set(allowedViews.map((view) => view.id))

  useEffect(() => {
    Promise.allSettled([request('/api/dashboard/me/summary'), request('/api/dashboard/me/pending-evaluations')]).then(([a, b]) => {
      if (a.status === 'fulfilled') setSummary(a.value.data)
      if (b.status === 'fulfilled') setPending(unwrapList(b.value))
    }).catch((error) => notify(error.message, 'danger'))
  }, [notify, request])

  const activeForms = (datasets.forms || []).filter((form) => form.active !== false)
  const demoForms = activeForms.filter((form) => form.evaluationType === 'DEMO_DAY_INDUSTRY')
  const reportForms = activeForms.filter((form) => ['REPORT_PHASE_I', 'REPORT_PHASE_II'].includes(form.evaluationType))
  const oralForms = activeForms.filter((form) => ['ORAL_PHASE_I', 'ORAL_PHASE_II'].includes(form.evaluationType))
  const dashboard = {
    ADMIN: {
      title: 'Tableau de bord administrateur',
      description: 'Pilotage global de la plateforme: comptes, projets, equipes, phases, evaluations, notes et rapports.',
      metrics: [['Utilisateurs', summary?.users ?? datasets.users?.length ?? 0], ['Projets', summary?.projects ?? datasets.projects?.length ?? 0], ['Évaluations', summary?.evaluations ?? pending.length], ['Rapports', summary?.reports ?? datasets.reports?.length ?? 0]],
      actions: [['Importer les données', 'imports'], ['Gérer les données', 'crud'], ['Demandes de prolongation', 'extensions'], ['Notes', 'grading'], ['Rapports', 'reports']],
      primaryTitle: 'evaluations en attente', primaryRows: pending.slice(0, 8), primaryColumns: ['evaluationType','status','project','evaluator','updatedAt'],
      secondaryTitle: 'Projets actifs', secondaryRows: datasets.projects || [], secondaryColumns: ['title','academicYear','status','track'],
    },
    SUPERVISOR: {
      title: 'Tableau de bord superviseur',
      description: 'Suivi des projets encadres et saisie des fiches superviseur Phase I et Phase II.',
      metrics: [['Projets assignés', summary?.projects ?? datasets.projects?.length ?? 0], ['Fiches à remplir', summary?.pendingEvaluations ?? pending.length], ['Fiches validées', summary?.submittedEvaluations ?? 0], ['Phases ouvertes', summary?.openPhases ?? 0]],
      actions: [['Ouvrir les évaluations', 'evaluations'], ['Demander une prolongation', 'extensions'], ['Voir les échéances', 'calendar']],
      primaryTitle: 'Projets assignes', primaryRows: datasets.projects || [], primaryColumns: ['title','academicYear','status','track'],
      secondaryTitle: 'evaluations a traiter', secondaryRows: pending.slice(0, 8), secondaryColumns: ['evaluationType','status','project','evaluator','updatedAt'],
    },
    FACULTY_EVALUATOR: {
      title: 'Tableau de bord evaluateur academique',
      description: 'Evaluation des soutenances orales avec brouillon, validation et verrouillage.',
      metrics: [['Projets assignés', summary?.projects ?? datasets.projects?.length ?? 0], ['Soutenances à traiter', summary?.pendingEvaluations ?? pending.length], ['Fiches validées', summary?.submittedEvaluations ?? 0], ['Phases ouvertes', summary?.openPhases ?? 0]],
      actions: [['Évaluer une soutenance', 'evaluations'], ['Demander une prolongation', 'extensions'], ['Voir les échéances', 'calendar']],
      primaryTitle: 'Formulaires de soutenance', primaryRows: oralForms, primaryColumns: ['name','evaluationType','phaseType','active'],
      secondaryTitle: 'evaluations en attente', secondaryRows: pending.slice(0, 8), secondaryColumns: ['evaluationType','status','project','evaluator','updatedAt'],
    },
    REPORT_EVALUATOR: {
      title: 'Tableau de bord évaluateur de rapports',
      description: 'Notation des rapports papier Report 1 et Report 2 pour les projets attribués.',
      metrics: [['Projets assignés', summary?.projects ?? datasets.projects?.length ?? 0], ['Rapports à traiter', summary?.pendingEvaluations ?? pending.length], ['Fiches validées', summary?.submittedEvaluations ?? 0], ['Phases ouvertes', summary?.openPhases ?? 0]],
      actions: [['Évaluer un rapport', 'evaluations'], ['Demander une prolongation', 'extensions'], ['Voir les échéances', 'calendar']],
      primaryTitle: 'Formulaires de rapport', primaryRows: reportForms, primaryColumns: ['name','evaluationType','phaseType','active'],
      secondaryTitle: 'Rapports en attente', secondaryRows: pending.slice(0, 8), secondaryColumns: ['evaluationType','status','project','evaluator','updatedAt'],
    },
    INDUSTRY_REPRESENTATIVE: {
      title: 'Tableau de bord representant industriel',
      description: 'evaluation Demo Day: prototype, impact industriel et feedback final.',
      metrics: [['Équipes Demo Day', datasets.teams?.length ?? 0], ['Projets à évaluer', summary?.projects ?? datasets.projects?.length ?? 0], ['Fiches à remplir', summary?.pendingEvaluations ?? pending.length], ['Fiches validées', summary?.submittedEvaluations ?? 0]],
      actions: [['Évaluer Demo Day', 'evaluations'], ['Consulter les notes publiées', 'grading'], ['Demander une prolongation', 'extensions']],
      primaryTitle: 'equipes Demo Day', primaryRows: datasets.teams || [], primaryColumns: ['name','section','academicYear','project'],
      secondaryTitle: 'Formulaires Demo Day', secondaryRows: demoForms, secondaryColumns: ['name','evaluationType','phaseType','active'],
    },
    COORDINATOR: {
      title: 'Tableau de bord coordinateur FYP',
      description: 'Consolidation des rapports, notes finales, notifications et suivi de completion.',
      metrics: [['Rapports', summary?.reports ?? datasets.reports?.length ?? 0], ['Notes', summary?.grades ?? datasets.grades?.length ?? 0], ['Évaluations en attente', summary?.pendingEvaluations ?? pending.length], ['Projets', summary?.projects ?? datasets.projects?.length ?? 0]],
      actions: [['Consulter les rapports', 'reports'], ['Voir les notes', 'grading'], ['Voir les échéances', 'calendar']],
      primaryTitle: 'Rapports recents', primaryRows: datasets.reports || [], primaryColumns: ['project','phase','status','recipientEmail','generatedAt'],
      secondaryTitle: 'Notes consolidees', secondaryRows: datasets.grades || [], secondaryColumns: ['phaseType','weightedScore','finalScore','published'],
    },
  }[activeRole] || {}

  const actions = (dashboard.actions || []).filter(([, view]) => allowedViewIds.has(view)).slice(0, 5)
  const phases = [...(datasets.phases || [])].sort((left, right) => new Date(left.deadline || 0) - new Date(right.deadline || 0))
  const currentPhase = phases.find((phase) => phase.status === 'OPEN') || phases.find((phase) => new Date(phase.deadline || 0) >= new Date())
  const todayLabel = new Intl.DateTimeFormat(currentLocale(), { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date())
  const firstName = String(session?.fullName || '').trim().split(/\s+/)[0] || 'Bienvenue'
  const phaseStart = currentPhase?.startDate ? new Date(currentPhase.startDate).getTime() : 0
  const phaseEnd = currentPhase?.deadline ? new Date(currentPhase.deadline).getTime() : 0
  const phaseProgress = phaseEnd > phaseStart ? Math.max(0, Math.min(100, ((dashboardNow - phaseStart) / (phaseEnd - phaseStart)) * 100)) : 0
  const openDetails = (kind) => {
    const primary = kind === 'primary'
    setDetailModal({
      title: primary ? dashboard.primaryTitle : dashboard.secondaryTitle,
      rows: primary ? dashboard.primaryRows || [] : dashboard.secondaryRows || [],
      columns: primary ? dashboard.primaryColumns || [] : dashboard.secondaryColumns || [],
    })
  }

  return <section className="dashboard-page page-enter">
    <header className="dashboard-compact-header dashboard-command-header">
      <div className="dashboard-welcome"><span>{todayLabel}</span><h2>Bonjour, {firstName}</h2><p>{dashboard.title}</p></div>
      <button type="button" className="active-phase-button" onClick={() => setActiveView('calendar')}>
        <span className={'phase-dot ' + String(currentPhase?.status || '').toLowerCase()} />
        <div><small>Phase active</small><strong>{currentPhase?.name || 'Aucune phase'}</strong></div>
        <ChevronRight size={17} />
        <span className="phase-progress" aria-hidden="true"><i style={{ width: `${phaseProgress}%` }} /></span>
      </button>
    </header>

    <div className="metric-grid">{(dashboard.metrics || []).map(([label, value], index) => <article className={'metric tone-' + (index + 1)} key={label}><div className="metric-top"><span>{label}</span><i><MetricIcon label={label} /></i></div><strong>{value}</strong></article>)}</div>

    <section className="dashboard-actions" aria-label="Actions rapides">
      {actions.map(([label, view], index) => <button type="button" className={index === 0 ? 'primary' : ''} key={label} onClick={() => setActiveView(view)}><ViewIcon view={view} /><span>{label}</span><ChevronRight size={16} /></button>)}
    </section>

    <div className="dashboard-summary-grid">
      <Panel title={dashboard.primaryTitle} className="dashboard-priority-panel"><div className="dashboard-panel-toolbar"><span>{(dashboard.primaryRows || []).length} élément{(dashboard.primaryRows || []).length === 1 ? '' : 's'}</span><button type="button" onClick={() => openDetails('primary')}>Voir tout<ArrowRight size={15} /></button></div><DataTable rows={(dashboard.primaryRows || []).slice(0, 4)} columns={(dashboard.primaryColumns || []).slice(0, 3)} compact searchable={false} /></Panel>
      <Panel title="Échéances" accent="gold" className="deadline-panel"><div className="deadline-list">{phases.slice(0, 3).map((phase) => <button type="button" key={phase.id} onClick={() => setActiveView('calendar')}><span className={'phase-dot ' + String(phase.status || '').toLowerCase()} /><div><strong>{phase.name}</strong><small>{formatDateTime(phase.deadline)}</small></div><ChevronRight size={15} /></button>)}{!phases.length && <EmptyState title="Aucune échéance" />}</div><button type="button" className="panel-link" onClick={() => setActiveView('calendar')}>Ouvrir le calendrier<ArrowRight size={15} /></button></Panel>
    </div>

    {(dashboard.secondaryRows || []).length > 0 && <button type="button" className="dashboard-more-button" onClick={() => openDetails('secondary')}><Eye size={17} />{dashboard.secondaryTitle}<span>{dashboard.secondaryRows.length}</span></button>}

    <DataDialog open={Boolean(detailModal)} title={detailModal?.title} rows={detailModal?.rows || []} columns={detailModal?.columns || []} onClose={() => setDetailModal(null)} />
  </section>
}

function CrudStudio({ datasets, request, reload, notify, searchTarget }) {
  const [resourceKey, setResourceKey] = useState(() => searchTarget?.resourceKey && resourceConfigs[searchTarget.resourceKey] ? searchTarget.resourceKey : 'users')
  const config = resourceConfigs[resourceKey]
  return <section className="crud-studio"><div className="section-head"><div><h2>Gestion des données</h2></div></div><div className="resource-tabs">{Object.entries(resourceConfigs).map(([key, cfg]) => <button key={key} className={resourceKey === key ? 'active' : ''} onClick={() => setResourceKey(key)}>{cfg.title}</button>)}</div><ResourceManager key={resourceKey} resourceKey={resourceKey} config={config} datasets={datasets} request={request} reload={reload} notify={notify} initialQuery={searchTarget?.resourceKey === resourceKey ? searchTarget.query : ''} /></section>
}

function ResourceManager({ resourceKey, config, datasets, request, reload, notify, initialQuery }) {
  const [rows, setRows] = useState(datasets[resourceKey] || [])
  const [form, setForm] = useState(() => initialForm(config.fields || []))
  const [editing, setEditing] = useState(null)
  const [formOpen, setFormOpen] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [busy, setBusy] = useState(false)
  useEffect(() => {
    const timer = window.setTimeout(() => setRows(datasets[resourceKey] || []), 0)
    return () => window.clearTimeout(timer)
  }, [datasets, resourceKey])

  function edit(row) {
    const next = initialForm(config.fields || [])
    ;(config.fields || []).forEach((field) => {
      if (field.name.endsWith('Id')) next[field.name] = row[field.name.replace(/Id$/, '')]?.id || row[field.name] || ''
      else if (field.name === 'studentIds') next[field.name] = (row.students || []).map((student) => student.id)
      else if (field.type === 'datetime-local' && row[field.name]) next[field.name] = String(row[field.name]).slice(0, 16)
      else next[field.name] = row[field.name] ?? next[field.name]
    })
    setEditing(row); setForm(next); setFormOpen(true)
  }

  function createNew() {
    setEditing(null)
    setForm(initialForm(config.fields || []))
    setFormOpen(true)
  }

  async function submit(event) {
    event.preventDefault(); if (config.readOnly) return; setBusy(true)
    try {
      const payload = serialize(form, config.fields)
      await request(editing ? config.endpoint + '/' + editing.id : (config.customCreateEndpoint || config.endpoint), { method: editing ? 'PUT' : 'POST', body: JSON.stringify(payload) })
      notify(editing ? 'Modification enregistrée' : 'Élément ajouté'); setEditing(null); setFormOpen(false); setForm(initialForm(config.fields || [])); await reload(); setRows(unwrapList(await request(config.endpoint)))
    } catch (error) { notify(error.message, 'danger') } finally { setBusy(false) }
  }

  async function remove(row) {
    setBusy(true)
    try { await request(config.endpoint + '/' + row.id, { method: 'DELETE' }); notify('Élément supprimé'); setDeleteTarget(null); await reload(); setRows(unwrapList(await request(config.endpoint))) } catch (error) { notify(error.message, 'danger') } finally { setBusy(false) }
  }

  async function resendIndustryInvitation(row) {
    setBusy(true)
    try {
      await request('/api/users/' + row.id + '/invite', { method: 'POST' })
      notify('Invitation Industry Guest envoyée')
      await reload()
      setRows(unwrapList(await request(config.endpoint)))
    } catch (error) {
      notify(error.message, 'danger')
    } finally {
      setBusy(false)
    }
  }

  const extraAction = resourceKey === 'users'
    ? (row) => row.role === 'INDUSTRY_REPRESENTATIVE'
      ? <button type="button" className="mini-button icon-text" disabled={busy} onClick={() => resendIndustryInvitation(row)} title="Renvoyer l’invitation"><Send size={14} />Inviter</button>
      : null
    : null

  return <div className="resource-manager">
    <div className="resource-manager-toolbar"><button className="icon-button" title="Actualiser" aria-label="Actualiser" onClick={async () => setRows(unwrapList(await request(config.endpoint)))}><RefreshCw size={17} /></button>{!config.readOnly && <button type="button" className="primary-action" onClick={createNew}><Plus size={17} />Ajouter</button>}</div>
    <Panel title={config.title} wide><DataTable rows={rows} columns={config.columns} onEdit={!config.readOnly ? edit : null} onDelete={!config.readOnly ? setDeleteTarget : null} extraAction={extraAction} initialQuery={initialQuery} /></Panel>
    <DialogShell open={formOpen} title={editing ? 'Modifier' : 'Ajouter'} onClose={() => { if (!busy) setFormOpen(false) }}>
      <form className="stack-form compact dialog-form" onSubmit={submit}>{(config.fields || []).map((field) => <DynamicField key={field.name} field={field} value={form[field.name]} datasets={datasets} onChange={(value) => setForm({ ...form, [field.name]: value })} />)}<div className="dialog-actions"><button type="button" className="ghost-button" onClick={() => setFormOpen(false)} disabled={busy}>Annuler</button><button className="primary-action" disabled={busy}>{busy ? 'Enregistrement…' : editing ? 'Enregistrer' : 'Ajouter'}</button></div></form>
    </DialogShell>
    <ConfirmDialog open={Boolean(deleteTarget)} title="Supprimer cet élément ?" message={deleteTarget ? itemName(deleteTarget) : ''} confirmLabel="Supprimer" danger onCancel={() => setDeleteTarget(null)} onConfirm={() => remove(deleteTarget)} />
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
        notify(`Initialisation terminée : ${created} créations, ${updated} mises à jour ; vérifiez les invitations Industry dans Mailpit`)
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
    ? '/FYP_FULL_DEMO_DATA.xlsx'
    : '/modele_import_etudiants_squ.xlsx'

  return <section className="import-workspace">
    <div className="section-head">
      <div>
        <h2>Imports Excel</h2>
      </div>
      <a className="soft-button download-template" href={templateHref} download>Télécharger le modèle Excel</a>
    </div>

    <div className="import-mode" role="tablist" aria-label="Type d’import">
      <button className={initializationMode ? 'active' : ''} onClick={() => resetMode('initialization')}>Initialisation annuelle</button>
      <button className={!initializationMode ? 'active' : ''} onClick={() => resetMode('students')}>Mise à jour étudiants</button>
    </div>

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
            <div className="section-head"><div><h3>Contrôle des feuilles</h3></div></div>
            <div className="table-wrap"><table className="initialization-table"><thead><tr><th>Feuille</th><th>Lignes</th><th>Valides</th><th>Créations</th><th>Mises à jour</th><th>Inchangées</th><th>État</th></tr></thead><tbody>
              {(preview.sheets || []).map((sheet) => <tr key={sheet.sheet}>
                <td><strong>{sheet.sheet}</strong></td><td>{sheet.totalRows}</td><td>{sheet.validRows}</td><td>{sheet.created || 0}</td><td>{sheet.updated || 0}</td><td>{sheet.unchanged || 0}</td>
                <td><span className={'validation-state ' + (sheet.totalRows === sheet.validRows ? 'valid' : 'invalid')}>{sheet.totalRows === sheet.validRows ? 'Valide' : 'À corriger'}</span></td>
              </tr>)}
            </tbody></table></div>
          </section>
        : <section className="import-preview">
            <div className="section-head"><div><h3>Aperçu des étudiants</h3></div></div>
            <div className="table-wrap"><table><thead><tr>{['stdID', 'Cohorte', 'Nom complet', 'E-mail SQU', 'Action', 'État'].map((column) => <th key={column}>{column}</th>)}</tr></thead><tbody>
              {(preview.normalized || []).slice(0, 15).map((row) => <tr key={row.rowNumber}><td>{row.studentNumber}</td><td>{row.cohort}</td><td>{row.fullName}</td><td>{row.email}</td><td>{row.existing ? 'Mise à jour' : 'Création'}</td><td><span className={'validation-state ' + (row.errors?.length ? 'invalid' : 'valid')}>{row.errors?.length ? 'À corriger' : 'Valide'}</span></td></tr>)}
            </tbody></table></div>
          </section>}

      <div className="import-actions">
        <span>{preview.errors.length ? 'Corrigez le classeur puis relancez l’analyse.' : 'Prêt à importer.'}</span>
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

function EvaluationStudio({ datasets, request, notify, activeRole, session, initialProjectId }) {
  const defaultEvaluationType = activeRole === 'ADMIN'
    ? 'ORAL_PHASE_I'
    : evaluationTypesForRole(activeRole)[0] || 'ORAL_PHASE_I'
  const [draft, setDraft] = useState({ projectId: '', phaseId: '', evaluatorId: '', evaluationType: defaultEvaluationType, trackCode: 'CSN', generalComment: '' })
  const [scoreDrafts, setScoreDrafts] = useState(() => readLocalJson('fyp-score-sheets', {}))
  const [sheetStatuses, setSheetStatuses] = useState(() => readLocalJson('fyp-score-statuses', {}))
  const [submissionIds, setSubmissionIds] = useState({})
  const [projectEvaluations, setProjectEvaluations] = useState([])
  const [showFormula, setShowFormula] = useState(false)
  const [historyOpen, setHistoryOpen] = useState(false)
  const [extensionOpen, setExtensionOpen] = useState(false)
  const [submitConfirmOpen, setSubmitConfirmOpen] = useState(false)
  const [phaseAccess, setPhaseAccess] = useState(null)
  const [saveState, setSaveState] = useState('idle')
  const [lastSavedAt, setLastSavedAt] = useState(null)
  const [currentTime, setCurrentTime] = useState(() => new Date().getTime())
  const [extensionReason, setExtensionReason] = useState('')
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
    const roleTypes = evaluationTypesForRole(activeRole)
    return activeRole === 'ADMIN'
      ? roleTypes
      : draft.projectId
        ? roleTypes.filter((type) => personalAssignments.some((assignment) => assignment.projectId === draft.projectId && assignment.evaluationType === type))
        : roleTypes
  }, [activeRole, draft.projectId, personalAssignments])
  const selectedProject = evaluationProjects.find((project) => project.id === draft.projectId)
  const requiredPhaseType = phaseTypeForEvaluation(draft.evaluationType)
  const availablePhases = useMemo(() => (datasets.phases || []).filter((phase) => {
    if (requiredPhaseType && phase.type !== requiredPhaseType) return false
    return !selectedProject?.academicYear || !phase.academicYear || phase.academicYear === selectedProject.academicYear
  }), [datasets.phases, requiredPhaseType, selectedProject])
  const template = SCORING_TEMPLATES[draft.evaluationType] || SCORING_TEMPLATES[defaultEvaluationType]
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
    session.fullName || session.email || (activeRole === 'INDUSTRY_REPRESENTATIVE'
      ? 'Membre du jury'
      : activeRole === 'REPORT_EVALUATOR' ? 'Évaluateur de rapports' : 'Évaluateur'),
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
    if (!initialProjectId || draft.projectId === initialProjectId) return undefined
    const project = evaluationProjects.find((item) => item.id === initialProjectId)
    if (!project) return undefined
    const timer = window.setTimeout(() => {
      setPhaseAccess(null)
      setDraft((current) => ({ ...current, projectId: project.id, phaseId: '', evaluationType: '', trackCode: project.track?.code || project.trackCode || current.trackCode }))
      request('/api/evaluations/by-project/' + project.id).then((response) => setProjectEvaluations(unwrapList(response))).catch(() => setProjectEvaluations([]))
    }, 0)
    return () => window.clearTimeout(timer)
  }, [draft.projectId, evaluationProjects, initialProjectId, request])
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
        }),
      })
      setExtensionReason('')
      setExtensionOpen(false)
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
    <div className="section-head evaluation-heading"><div><h2>Évaluation</h2></div><div className="sheet-status-wrap"><div className="sheet-status"><span className={'status-dot ' + status.toLowerCase()} />{locked ? 'Validée' : 'Brouillon'}</div><small className={'save-state ' + saveState}>{saveStateLabel()}</small></div></div>

    <section className="evaluation-context" aria-label="Contexte de l’évaluation">
      <label className="field"><span>Filière du projet</span><select value={draft.trackCode} disabled={activeRole !== 'ADMIN'} onChange={(event) => setDraft({ ...draft, trackCode: event.target.value })}>{trackOptions.map((track) => <option key={track}>{track}</option>)}</select></label>
      <SelectData label="Projet attribué" value={draft.projectId} data={evaluationProjects} onChange={(projectId) => {
        const project = evaluationProjects.find((item) => item.id === projectId)
        const roleTypes = evaluationTypesForRole(activeRole)
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

    {draft.phaseId && <section className={'deadline-banner compact ' + (phaseAccess?.allowed ? 'open' : 'closed')}>
      <div><strong>{phaseAccess?.allowed ? 'Évaluation ouverte' : 'Évaluation verrouillée'}</strong><small>{formatDateTime(effectiveDeadline)}</small></div>
      {deadlineAlert && <div className={'deadline-countdown ' + deadlineAlert}><Bell size={18} /><strong>{deadlineAlert === 'half-day' ? 'Moins de 12 heures' : 'Moins de 24 heures'}</strong></div>}
      {evaluationBlocked && !locked && submissionIds[sheetId] && <span className="expired-draft-warning compact">Brouillon non comptabilisé</span>}
      {evaluationBlocked && phaseAccess?.reasonCode === 'PHASE_DEADLINE_EXPIRED' && activeRole !== 'ADMIN' && <button type="button" className="soft-button" onClick={() => setExtensionOpen(true)}>Demander une prolongation</button>}
    </section>}

    <div className="sheet-toolbar">
      <div className="sheet-title"><strong>{template.label}</strong><span>{template.phase} · note sur 10</span></div>
      <div className="completion-meter"><span>{completedCells}/{requiredCells} cellules</span><div><i style={{ width: (requiredCells ? Math.round((completedCells / requiredCells) * 100) : 0) + '%' }} /></div></div>
      <button className="soft-button" type="button" onClick={() => setShowFormula(true)}>Voir le calcul</button>
    </div>

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
      <div className="sheet-actions"><button className="ghost-button" type="button" disabled={editingDisabled} onClick={resetSheet}>Réinitialiser</button><button className="soft-button" type="button" disabled={editingDisabled || saveState === 'saving'} onClick={() => saveSheet(false)}>Enregistrer le brouillon</button><button className="primary-action" type="button" disabled={editingDisabled || saveState === 'saving'} onClick={() => {
        if (completedCells < requiredCells) {
          notify('Toutes les notes doivent être renseignées avant validation.', 'danger')
          return
        }
        setSubmitConfirmOpen(true)
      }}>Valider la fiche</button></div>
    </section>

    <button type="button" className="dashboard-more-button" onClick={() => setHistoryOpen(true)}><Eye size={17} />Historique des évaluations<span>{projectEvaluations.length}</span></button>

    <DialogShell open={showFormula} title="Calcul de la note" onClose={() => setShowFormula(false)}><FormulaPanel template={template} /></DialogShell>
    <DialogShell open={historyOpen} title="Historique des évaluations" onClose={() => setHistoryOpen(false)} wide><DataTable rows={projectEvaluations} columns={['evaluationType','status','totalScore','completedScoreCount','locked','draftSavedAt','submittedAt','evaluator']} compact /></DialogShell>
    <DialogShell open={extensionOpen} title="Demander une prolongation" onClose={() => setExtensionOpen(false)}><form className="stack-form compact dialog-form" onSubmit={requestExtension}><label className="field"><span>Motif</span><textarea required value={extensionReason} onChange={(event) => setExtensionReason(event.target.value)} /></label><div className="extension-policy-note"><ShieldCheck size={18} /><span>L’administrateur fixera la nouvelle date.</span></div><div className="dialog-actions"><button type="button" className="ghost-button" onClick={() => setExtensionOpen(false)}>Annuler</button><button className="primary-action" disabled={extensionBusy}>{extensionBusy ? 'Envoi…' : 'Envoyer'}</button></div></form></DialogShell>
    <ConfirmDialog open={submitConfirmOpen} title="Valider définitivement cette fiche ?" message="Après validation, les notes seront verrouillées et prises en compte dans les calculs. Cette action ne pourra pas être annulée par l’évaluateur." confirmLabel="Valider et verrouiller" onCancel={() => setSubmitConfirmOpen(false)} onConfirm={async () => {
      setSubmitConfirmOpen(false)
      await saveSheet(true)
    }} />
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
  const targetLabel = template.kind === 'demo' ? 'Groupe' : template.kind === 'report' ? 'Projet' : 'Étudiant'
  return <section className="result-summary"><div className="result-heading"><div><span className="eyebrow">Résultat calculé</span><h3>Note finale</h3></div><span className="formula-chip">{template.shortFormula}</span></div><div className="result-table-wrap"><table className="result-table"><thead><tr><th>{targetLabel}</th>{template.kind === 'presentation' && <><th>Partie A</th><th>Contribution A</th><th>Partie B</th><th>Contribution B</th></>}<th>Note /10</th><th>Niveau</th></tr></thead><tbody>{targets.map((target) => { const result = results[target.id] || results.group; const band = performanceBand(result?.finalScore || 0); return <tr key={target.id}><td><strong>{target.label}</strong><small>{target.secondary}</small></td>{template.kind === 'presentation' && <><td>{formatScore(result.individualScore)}</td><td>{formatScore(result.contributionA)} / 3,75</td><td>{formatScore(result.groupScore)}</td><td>{formatScore(result.contributionB)} / 6,25</td></>}<td className="final-score">{formatScore(result?.finalScore)}</td><td><span className={'performance-pill ' + band.tone}>{band.label}</span></td></tr> })}</tbody></table></div></section>
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
  const [requestOpen, setRequestOpen] = useState(false)
  const [reviewing, setReviewing] = useState(null)
  const [form, setForm] = useState({ phaseId: datasets.phases?.[0]?.id || '', reason: '' })
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
        }),
      })
      setForm({ ...form, reason: '' })
      setRequestOpen(false)
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
      setReviewing(null)
      notify(approved ? 'Prolongation approuvée' : 'Demande rejetée')
      await load()
    } catch (error) { notify(error.message, 'danger') } finally { setBusy(false) }
  }

  const action = (row) => row.status === 'PENDING' && isAdmin ? <button type="button" className="mini-button icon-text" onClick={() => setReviewing(row)}><Eye size={14} />Examiner</button> : null

  return <section className="extension-center">
    <div className="section-head"><div><h2>Prolongations</h2></div><div className="section-actions">{isAdmin ? <label className="mini-field"><span>Statut</span><select value={filter} onChange={(event) => setFilter(event.target.value)}><option value="">Tous</option><option value="PENDING">En attente</option><option value="APPROVED">Approuvées</option><option value="REJECTED">Rejetées</option></select></label> : <button type="button" className="primary-action" onClick={() => setRequestOpen(true)}><Plus size={17} />Nouvelle demande</button>}</div></div>
    <Panel title={isAdmin ? 'Demandes reçues' : 'Mes demandes'} wide><DataTable rows={rows} columns={isAdmin ? ['phase','requester','reason','status','extendedDeadline','requestedAt'] : ['phase','reason','status','extendedDeadline','requestedAt']} compact extraAction={isAdmin ? action : null} /></Panel>
    <DialogShell open={requestOpen} title="Demander une prolongation" onClose={() => setRequestOpen(false)}>
      <form className="stack-form compact dialog-form" onSubmit={create}><SelectData label="Phase expirée" value={form.phaseId} data={datasets.phases || []} onChange={(phaseId) => setForm({ ...form, phaseId })} /><Field label="Motif" textarea value={form.reason} onChange={(reason) => setForm({ ...form, reason })} /><div className="extension-policy-note"><ShieldCheck size={18} /><span>L’administrateur fixera la nouvelle date.</span></div><div className="dialog-actions"><button type="button" className="ghost-button" onClick={() => setRequestOpen(false)}>Annuler</button><button className="primary-action" disabled={busy || !form.phaseId || !form.reason.trim()}>Envoyer</button></div></form>
    </DialogShell>
    <DialogShell open={Boolean(reviewing)} title="Examiner la demande" onClose={() => setReviewing(null)}>
      {reviewing && <div className="extension-review-dialog"><div className="review-summary"><span>Demandeur<strong>{itemName(reviewing.requester)}</strong></span><span>Phase<strong>{itemName(reviewing.phase)}</strong></span><span className="wide">Motif<strong>{reviewing.reason}</strong></span></div><label className="field"><span>Nouvelle échéance</span><input type="datetime-local" value={decisions[reviewing.id]?.extendedDeadline || ''} onChange={(event) => updateDecision(reviewing.id, 'extendedDeadline', event.target.value)} /></label><Field label="Commentaire" textarea value={decisions[reviewing.id]?.adminComment || ''} onChange={(value) => updateDecision(reviewing.id, 'adminComment', value)} /><div className="dialog-actions three"><button type="button" className="ghost-button" onClick={() => setReviewing(null)}>Annuler</button><button type="button" className="danger-action compact" disabled={busy} onClick={() => decide(reviewing, false)}>Rejeter</button><button type="button" className="primary-action" disabled={busy || !decisions[reviewing.id]?.extendedDeadline} onClick={() => decide(reviewing, true)}>Approuver</button></div></div>}
    </DialogShell>
  </section>
}
function GradingCenter({ datasets, request, reload, notify, activeRole, token, initialProjectId }) {
  const canManageGrades = activeRole === 'ADMIN'
  const canExport = activeRole === 'ADMIN' || activeRole === 'COORDINATOR'
  const projects = useMemo(() => datasets.projects || [], [datasets.projects])
  const [projectId, setProjectId] = useState('')
  const [phaseId, setPhaseId] = useState('')
  const [projectGrades, setProjectGrades] = useState([])
  const [studentGrades, setStudentGrades] = useState([])
  const [busy, setBusy] = useState(false)
  const [rulesOpen, setRulesOpen] = useState(false)
  const [publishConfirmOpen, setPublishConfirmOpen] = useState(false)
  const effectiveProjectId = projects.some((project) => project.id === projectId)
    ? projectId
    : projects.some((project) => project.id === initialProjectId) ? initialProjectId : projects[0]?.id || ''
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
    <div className="section-head full-span"><div><h2>{canManageGrades ? 'Notes et publication' : 'Résultats'}</h2></div><button type="button" className="soft-button" onClick={() => setRulesOpen(true)}>Règles de calcul</button></div>
    <Panel title="Projet et phase" accent="green" wide><div className="form-grid two"><SelectData label="Projet" value={effectiveProjectId} data={projects} onChange={setProjectId} /><SelectData label="Phase" value={effectivePhaseId} data={phaseOptions} onChange={setPhaseId} /></div><div className="action-row">{canManageGrades && <button className="primary-action" disabled={busy || !effectiveProjectId || !effectivePhaseId} onClick={calculate}>{busy ? 'Calcul…' : 'Calculer'}</button>}{canManageGrades && selectedProjectGrade && !selectedProjectGrade.published && <button className="soft-button" disabled={busy} onClick={() => setPublishConfirmOpen(true)}>Publier</button>}{canExport && <button className="soft-button icon-text" disabled={!effectiveProjectId} onClick={exportProject}><Download size={16} />Exporter</button>}</div></Panel>
    <Panel title="Résultats individuels" subtitle={selectedProject ? selectedProject.projectNumber + ' · ' + selectedProject.title : ''} wide>{!projects.length
      ? <EmptyState title="Aucun projet accessible" detail="Les projets apparaissent selon les affectations du compte connecté." />
      : studentGrades.length
        ? <StudentGradeTable rows={studentGrades} />
        : <EmptyState title="Aucun résultat calculé" detail={canManageGrades ? 'Validez toutes les fiches requises, puis lancez le calcul.' : 'Les résultats apparaîtront après calcul et publication.'} />}</Panel>
    {projectGrades.length > 0 && <Panel title="Synthèse du projet par phase" wide><DataTable rows={projectGrades} columns={['phaseType','finalScore','published']} compact /></Panel>}
    <DialogShell open={rulesOpen} title="Règles de calcul" onClose={() => setRulesOpen(false)}><p className="muted">Seules les fiches validées sont calculées. Les notes des évaluateurs sont moyennées puis pondérées par phase.</p><div className="tag-list">{EVALUATION_TYPES.map((type) => <span key={type}>{SCORING_TEMPLATES[type]?.label || pretty(type)}</span>)}</div></DialogShell>
    <ConfirmDialog open={publishConfirmOpen} title="Publier les résultats ?" message="Les notes de cette phase deviendront visibles dans les espaces autorisés. Vérifiez les résultats avant de continuer." confirmLabel="Publier les notes" onCancel={() => setPublishConfirmOpen(false)} onConfirm={async () => {
      setPublishConfirmOpen(false)
      await publish()
    }} />
  </section>
}

function StudentGradeTable({ rows }) {
  const score = (value) => value === null || value === undefined ? '-' : formatScore(value)
  return <div className="table-wrap student-grade-table"><table><thead><tr><th>Student ID</th><th>Student name</th><th>Supervisor</th><th>Report</th><th>Presentation</th><th>Demo Day</th><th>Final /10</th><th>Status</th></tr></thead><tbody>{rows.map((row) => <tr key={row.id}><td><strong>{row.student?.studentNumber || '-'}</strong></td><td>{row.student?.fullName || '-'}</td><td>{score(row.supervisorScore)}</td><td>{score(row.reportScore)}</td><td>{score(row.oralScore)}</td><td>{score(row.demoScore)}</td><td className="final-score">{score(row.finalScore)}</td><td><StatusPill value={row.published ? 'PUBLISHED' : 'INTERNAL'} /></td></tr>)}</tbody></table></div>
}

function ReportCenter({ datasets, request, reload, notify, token, initialProjectId }) {
  const projects = useMemo(() => datasets.projects || [], [datasets.projects])
  const phases = datasets.phases || []
  const [projectId, setProjectId] = useState('')
  const [phaseId, setPhaseId] = useState('')
  const [completenessSnapshot, setCompletenessSnapshot] = useState({ phaseId: '', rows: [] })
  const [busy, setBusy] = useState(false)
  const [dialog, setDialog] = useState(null)
  const [infoOpen, setInfoOpen] = useState(false)
  const [archiveOpen, setArchiveOpen] = useState(false)
  const effectiveProjectId = projects.some((project) => project.id === projectId)
    ? projectId
    : projects.some((project) => project.id === initialProjectId) ? initialProjectId : projects[0]?.id || ''
  const effectivePhaseId = phases.some((phase) => phase.id === phaseId) ? phaseId : phases[0]?.id || ''
  const reports = datasets.reports || []
  const completeness = completenessSnapshot.phaseId === effectivePhaseId ? completenessSnapshot.rows : []
  const lockedForms = completeness.filter((row) => row.status === 'LOCKED').length
  const pendingForms = completeness.filter((row) => row.status !== 'LOCKED').length

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

  async function regenerate(id) {
    setBusy(true)
    try {
      await request('/api/reports/' + id + '/regenerate', { method: 'POST' })
      await reload()
      notify('Une nouvelle version du rapport a été générée')
    } catch (error) { notify(error.message, 'danger') } finally { setBusy(false) }
  }

  async function removeReport(id) {
    setBusy(true)
    try {
      await request('/api/reports/' + id, { method: 'DELETE' })
      await reload()
      notify('Rapport supprimé de l’archive')
    } catch (error) { notify(error.message, 'danger') } finally { setBusy(false) }
  }

  function confirmAction(title, message, label, action, danger = false) {
    setDialog({ title, message, label, action, danger })
  }

  return <section className="page-grid report-center">
    <div className="section-head full-span"><div><h2>Rapports Excel</h2></div><button type="button" className="soft-button" onClick={() => setInfoOpen(true)}>Contenu du fichier</button></div>
    <Panel title="Génération et téléchargement" accent="green" wide><div className="form-grid two"><SelectData label="Projet" value={effectiveProjectId} data={projects} onChange={setProjectId} /><SelectData label="Phase" value={effectivePhaseId} data={phases} onChange={setPhaseId} /></div><div className="report-readiness"><span><strong>{lockedForms}</strong> fiches validées</span><span className={pendingForms ? 'warning' : 'ready'}><strong>{pendingForms}</strong> à compléter</span></div><div className="action-row"><button className="soft-button icon-text" disabled={!effectivePhaseId} onClick={downloadPhase}><FileSpreadsheet size={16} />Synthèse de la phase</button><button className="soft-button icon-text" disabled={!effectiveProjectId} onClick={downloadProject}><Download size={16} />Export du projet</button><button className="soft-button icon-text" disabled={busy || !effectiveProjectId || !effectivePhaseId} onClick={() => confirmAction('Archiver le rapport de phase', 'Une version Excel de la phase sélectionnée sera conservée dans l’archive.', 'Générer la phase', () => generate(false))}><FileSpreadsheet size={16} />Archiver la phase</button><button className="primary-action" disabled={busy || !effectiveProjectId} onClick={() => confirmAction('Archiver le rapport final', 'Le rapport regroupera toutes les phases disponibles de ce projet.', 'Générer le rapport final', () => generate(true))}>Rapport final</button></div></Panel>
    <Panel title="Complétude des évaluations" wide>{completeness.length ? <DataTable rows={completeness} columns={['projectNumber','projectTitle','evaluationType','evaluatorName','evaluatorEmail','status']} compact /> : <EmptyState title="Aucune affectation pour cette phase" detail="Créez les affectations ou choisissez une autre phase." />}</Panel>
    <button type="button" className="dashboard-more-button full-span" onClick={() => setArchiveOpen(true)}><FileSpreadsheet size={17} />Archive des rapports<span>{reports.length}</span></button>
    <DialogShell open={infoOpen} title="Contenu du fichier Excel" onClose={() => setInfoOpen(false)}><div className="tag-list"><span>LEGACY_SUMMARY</span><span>FINAL_SUMMARY</span><span>EVALUATOR_DETAILS</span><span>MISSING_FORMS</span><span>AUDIT_TRAIL</span></div></DialogShell>
    <DialogShell open={archiveOpen} title="Archive des rapports" onClose={() => setArchiveOpen(false)} wide>{reports.length ? <DataTable rows={reports} columns={['project','phase','status','recipientEmail','generatedAt','sentAt']} compact extraAction={(row) => <div className="row-actions"><button className="mini-button" disabled={busy} onClick={() => confirmAction('Envoyer le rapport', 'Le fichier Excel sera joint au courriel destiné à ' + (row.recipientEmail || 'la coordination') + '.', 'Envoyer', () => send(row.id))}><Send size={14} />Envoyer</button><button className="mini-button" disabled={busy} onClick={() => confirmAction('Créer une nouvelle version', 'Les données actuelles seront recalculées dans un nouveau fichier archivé.', 'Régénérer', () => regenerate(row.id))}><RefreshCw size={14} />Régénérer</button><button className="mini-button danger" disabled={busy} title="Supprimer le rapport" aria-label="Supprimer le rapport" onClick={() => confirmAction('Supprimer ce rapport', 'Cette version archivée sera supprimée de la plateforme.', 'Supprimer', () => removeReport(row.id), true)}><Trash2 size={14} /></button></div>} /> : <EmptyState title="Aucun rapport" />}</DialogShell>
    <ConfirmDialog open={Boolean(dialog)} title={dialog?.title} message={dialog?.message} confirmLabel={dialog?.label} danger={dialog?.danger} onCancel={() => setDialog(null)} onConfirm={() => { const action = dialog?.action; setDialog(null); action?.() }} />
  </section>
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

function DialogShell({ open, title, onClose, children, wide = false }) {
  useEffect(() => {
    if (!open) return undefined
    const previousOverflow = document.body.style.overflow
    const close = (event) => { if (event.key === 'Escape') onClose() }
    document.body.style.overflow = 'hidden'
    window.addEventListener('keydown', close)
    return () => {
      document.body.style.overflow = previousOverflow
      window.removeEventListener('keydown', close)
    }
  }, [onClose, open])
  if (!open) return null
  return createPortal(<div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}><section className={'app-dialog ' + (wide ? 'wide' : '')} role="dialog" aria-modal="true" aria-label={title}><header><h2>{title}</h2><button type="button" className="modal-close" onClick={onClose} aria-label="Fermer"><X size={18} /></button></header><div className="app-dialog-body">{children}</div></section></div>, document.body)
}

function DataDialog({ open, title, rows, columns, onClose }) {
  return <DialogShell open={open} title={title || 'Détails'} onClose={onClose} wide><DataTable rows={rows} columns={columns} compact /></DialogShell>
}

function ConfirmDialog({ open, title, message, confirmLabel, danger, onCancel, onConfirm }) {
  useEffect(() => {
    if (!open) return undefined
    const close = (event) => {
      if (event.key === 'Escape') onCancel()
    }
    window.addEventListener('keydown', close)
    return () => window.removeEventListener('keydown', close)
  }, [onCancel, open])
  if (!open) return null
  return createPortal(<div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onCancel() }}><section className="confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="confirm-title" aria-describedby="confirm-message"><button type="button" className="modal-close" onClick={onCancel} aria-label="Fermer"><X size={18} /></button><span className={'confirm-dialog-icon ' + (danger ? 'danger' : '')}>{danger ? <Trash2 size={22} /> : <ShieldCheck size={22} />}</span><h2 id="confirm-title">{title}</h2><p id="confirm-message">{message}</p><div className="confirm-dialog-actions"><button type="button" className="ghost-button" onClick={onCancel}>Annuler</button><button type="button" className={danger ? 'danger-action compact' : 'primary-action'} onClick={onConfirm}>{confirmLabel || 'Confirmer'}</button></div></section></div>, document.body)
}

function DataTable({ rows = [], columns = [], onEdit, onDelete, compact = false, extraAction, searchable = true, initialQuery = '', pageSize }) {
  const [query, setQuery] = useState(initialQuery || '')
  const [page, setPage] = useState(0)
  const effectivePageSize = pageSize || 8
  const normalizedQuery = normalizeTableSearch(query)
  const filteredRows = useMemo(() => normalizedQuery
    ? rows.filter((row) => normalizeTableSearch(JSON.stringify(row)).includes(normalizedQuery))
    : rows, [normalizedQuery, rows])
  const pageCount = Math.max(1, Math.ceil(filteredRows.length / effectivePageSize))
  const currentPage = Math.min(page, pageCount - 1)
  const visibleRows = filteredRows.slice(currentPage * effectivePageSize, (currentPage + 1) * effectivePageSize)

  if (!rows.length) return <EmptyState />
  return <div className="data-table-shell">
    {searchable && <div className="data-table-toolbar"><label><Search size={16} /><input value={query} onChange={(event) => { setQuery(event.target.value); setPage(0) }} placeholder="Rechercher dans le tableau…" aria-label="Rechercher dans le tableau" />{query && <button type="button" onClick={() => { setQuery(''); setPage(0) }} title="Effacer" aria-label="Effacer la recherche"><X size={15} /></button>}</label><span>{filteredRows.length} / {rows.length}</span></div>}
    {!filteredRows.length ? <EmptyState title="Aucun résultat" detail="Modifiez votre recherche." /> : <div className={'table-wrap ' + (compact ? 'compact' : '')}><table className="data-table"><thead><tr>{columns.map((column) => <th key={column}>{pretty(column)}</th>)}{(onEdit || onDelete || extraAction) && <th className="actions-column">Actions</th>}</tr></thead><tbody>{visibleRows.map((row, index) => <tr key={row.id || currentPage * effectivePageSize + index}>{columns.map((column) => <td key={column} data-label={pretty(column)} title={cellTitle(row[column])}><div className="cell-content">{renderCell(row[column], column)}</div></td>)}{(onEdit || onDelete || extraAction) && <td className="actions-cell" data-label="Actions"><div className="row-actions">{extraAction?.(row)}{onEdit && <button type="button" className="table-action-button" onClick={() => onEdit(row)} title="Modifier" aria-label={'Modifier ' + itemName(row)}><Pencil size={15} /></button>}{onDelete && <button type="button" className="table-action-button danger" onClick={() => onDelete(row)} title="Supprimer" aria-label={'Supprimer ' + itemName(row)}><Trash2 size={15} /></button>}</div></td>}</tr>)}</tbody></table></div>}
    {filteredRows.length > effectivePageSize && <footer className="data-table-pagination"><span>{currentPage + 1} / {pageCount}</span><div><button type="button" className="icon-button" disabled={currentPage === 0} onClick={() => setPage(Math.max(0, currentPage - 1))} title="Page précédente" aria-label="Page précédente"><ChevronLeft size={17} /></button><button type="button" className="icon-button" disabled={currentPage >= pageCount - 1} onClick={() => setPage(Math.min(pageCount - 1, currentPage + 1))} title="Page suivante" aria-label="Page suivante"><ChevronRight size={17} /></button></div></footer>}
  </div>
}

function normalizeTableSearch(value) {
  return String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('fr').trim()
}

function cellTitle(value) {
  if (Array.isArray(value)) return value.map((item) => itemName(item?.user || item)).join(', ')
  if (value && typeof value === 'object') return itemName(value.user || value)
  return value === null || value === undefined ? '' : String(value)
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
