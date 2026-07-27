import { useCallback, useEffect, useRef, useState } from 'react'
import { Bell, CheckCheck, MailOpen } from 'lucide-react'
import squLogo from './assets/Sultan_Qaboos_University_Logo.png'
import squMark from './assets/sultan-qaboos-university-logo-png_seeklogo-271991.png'
import { apiRequest, itemName, pretty, unwrapList } from './api.js'
import { EVALUATION_TYPES, ROLES, actorTemplates, resourceConfigs, views } from './config.js'
import { SCORING_TEMPLATES, calculateTemplate, normalizeScore, performanceBand, scoreKey, sectionAverage } from './gradingTemplates.js'
import { readImportFile } from './importWorkbook.js'
import { currentLocale, getInitialLanguage, setLanguagePreference, translateText, useAutoTranslate } from './i18n.js'
import './App.css'

const seedProjectId = '50000000-0000-0000-0000-000000000001'
const homeViewByRole = {
  ADMIN: 'dashboard',
  STUDENT: 'dashboard',
  SUPERVISOR: 'dashboard',
  FACULTY_EVALUATOR: 'dashboard',
  INDUSTRY_REPRESENTATIVE: 'dashboard',
  COORDINATOR: 'dashboard',
}

function normalizeRole(role) {
  return ROLES.includes(role) ? role : 'STUDENT'
}

function homeViewForRole(role) {
  return homeViewByRole[normalizeRole(role)] || 'dashboard'
}

function normalizeSession(raw, fallbackRole = 'STUDENT') {
  const session = raw?.data || raw || {}
  const role = normalizeRole(session.role || fallbackRole)
  return { ...session, role }
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
  const [session, setSession] = useState(() => JSON.parse(localStorage.getItem('fyp-session') || 'null'))
  const [activeView, setActiveView] = useState(() => homeViewForRole(session?.role || 'STUDENT'))
  const [language, setLanguage] = useState(getInitialLanguage)
  const [toast, setToast] = useState(null)
  useAutoTranslate(language)

  useEffect(() => {
    if (session) localStorage.setItem('fyp-session', JSON.stringify(session))
    else localStorage.removeItem('fyp-session')
  }, [session])

  const notify = useCallback((message, type = 'success') => {
    setToast({ message, type })
    window.clearTimeout(window.__toast)
    window.__toast = window.setTimeout(() => setToast(null), 4200)
  }, [])

  function openWorkspace(rawSession, fallbackRole) {
    const next = normalizeSession(rawSession, fallbackRole)
    setSession(next)
    setActiveView(homeViewForRole(next.role))
    notify('Bienvenue dans votre espace ' + pretty(next.role))
  }

  if (!session) return <AuthScreen onSession={openWorkspace} notify={notify} toast={toast} language={language} setLanguage={setLanguage} />
  return <Shell session={session} activeView={activeView} setActiveView={setActiveView} onLogout={() => setSession(null)} notify={notify} toast={toast} language={language} setLanguage={setLanguage} />
}

function LanguageSwitcher({ language, setLanguage }) {
  return <div className="language-switcher" data-no-translate aria-label="Language">
    {['fr', 'en'].map((code) => <button key={code} type="button" className={language === code ? 'active' : ''} onClick={() => { setLanguagePreference(code); setLanguage(code) }} aria-pressed={language === code}>{code.toUpperCase()}</button>)}
  </div>
}

function AuthScreen({ onSession, notify, toast, language, setLanguage }) {
  const [busy, setBusy] = useState(false)
  const [loginForm, setLoginForm] = useState({ email: 'admin@squ.edu.om', password: 'Admin@123' })

  async function login(event) {
    event.preventDefault(); setBusy(true)
    try {
      const result = await apiRequest('/api/auth/login', { method: 'POST', body: JSON.stringify(loginForm) })
      onSession(result.data)
    } catch (error) { notify(error.message, 'danger') } finally { setBusy(false) }
  }

  return <main className="auth-screen">
    <section className="auth-visual">
      <LogoLockup />
      <div className="auth-copy">
        <span className="eyebrow">Département de génie électrique</span>
        <h1>Évaluation numérique des projets de fin d’études</h1>
        <p>Un espace commun pour l’administration, les superviseurs, les évaluateurs académiques, les représentants industriels et la coordination.</p>
      </div>
      <div className="auth-metrics"><div><strong>2</strong><span>phases FYP</span></div><div><strong>7</strong><span>fiches d’évaluation</span></div><div><strong>5</strong><span>espaces RBAC</span></div></div>
    </section>
    <section className="auth-panel">
      <div className="auth-panel-tools"><LanguageSwitcher language={language} setLanguage={setLanguage} /></div>
      <div className="brand-badge"><img src={squMark} alt="SQU" /><span>Processus académique sécurisé</span></div>
      <form className="stack-form" onSubmit={login}>
        <h2>Connexion</h2><p className="muted">Accédez à votre espace selon le rôle attribué par l’administration.</p>
        <Field label="Adresse e-mail" type="email" value={loginForm.email} onChange={(email) => setLoginForm({ ...loginForm, email })} />
        <Field label="Mot de passe" type="password" value={loginForm.password} onChange={(password) => setLoginForm({ ...loginForm, password })} />
        <button className="primary-action" disabled={busy}>{busy ? 'Connexion…' : 'Se connecter'}</button>
        <div className="hint-strip">Compte de démonstration: admin@squ.edu.om / Admin@123</div>
      </form>
    </section>
    {toast && <Toast {...toast} />}
  </main>
}

function Shell({ session, activeView, setActiveView, onLogout, notify, toast, language, setLanguage }) {
  const [datasets, setDatasets] = useState({})
  const [personalNotifications, setPersonalNotifications] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const activeRole = normalizeRole(session.role)
  const allowedViews = views.filter((view) => view.roles.includes(activeRole))
  const request = useCallback((path, options = {}) => apiRequest(path, options, session.token), [session.token])
  const loadPersonalNotifications = useCallback(async () => {
    try {
      setPersonalNotifications(unwrapList(await request('/api/notifications/me')))
    } catch {
      setPersonalNotifications([])
    }
  }, [request])

  const loadCore = useCallback(async () => {
    setLoading(true); setError('')
    const endpoints = [['users','/api/users'],['students','/api/students'],['evaluators','/api/evaluators'],['tracks','/api/tracks'],['projects','/api/projects'],['teams','/api/teams'],['phases','/api/phases'],['forms','/api/evaluation-forms'],['reports','/api/reports'],['notifications','/api/notifications'],['audit','/api/audit'],['grades','/api/grades/project/' + seedProjectId]]
    try {
      const pairs = await Promise.all(endpoints.map(async ([key, path]) => {
        try { return [key, unwrapList(await request(path))] } catch { return [key, []] }
      }))
      setDatasets(Object.fromEntries(pairs))
    } catch (err) { setError(err.message) } finally { setLoading(false) }
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

  return <div className="app-shell">
    <aside className="sidebar">
      <LogoLockup compact />
      <nav className="nav-list">{allowedViews.map((view) => <button key={view.id} className={activeView === view.id ? 'active' : ''} onClick={() => setActiveView(view.id)}>{view.label}</button>)}</nav>
      <div className="sidebar-footer"><button className="ghost-button" onClick={onLogout}>Deconnexion</button></div>
    </aside>
    <main className="workspace">
      <header className="topbar"><div><span className="eyebrow">Session active</span><h1>{actorTemplates[activeRole]?.title || 'Espace utilisateur'}</h1></div><div className="topbar-actions"><button type="button" className={'notification-button ' + (activeView === 'notifications' ? 'active' : '')} onClick={() => setActiveView('notifications')} title="Notifications" aria-label="Notifications"><Bell size={20} />{personalNotifications.some((item) => !item.readAt) && <span className="notification-badge">{Math.min(99, personalNotifications.filter((item) => !item.readAt).length)}</span>}</button><LanguageSwitcher language={language} setLanguage={setLanguage} /><button className="soft-button" onClick={() => { loadCore(); loadPersonalNotifications() }}>Actualiser les donnees</button><div className="user-chip"><strong>{session.fullName}</strong><span>{session.email}</span></div></div></header>
      {loading && <div className="loading-bar"><span /></div>}{error && <div className="alert danger">{error}</div>}
      {activeView === 'dashboard' && <Dashboard datasets={datasets} request={request} activeRole={activeRole} notify={notify} setActiveView={setActiveView} allowedViews={allowedViews} />}
      {activeView === 'notifications' && <NotificationCenter notifications={personalNotifications} request={request} reload={loadPersonalNotifications} notify={notify} setActiveView={setActiveView} allowedViews={allowedViews} />}
      {activeView === 'imports' && <ImportCenter request={request} notify={notify} />}
      {activeView === 'crud' && <CrudStudio datasets={datasets} request={request} reload={loadCore} notify={notify} />}
      {activeView === 'evaluations' && <EvaluationStudio datasets={datasets} request={request} notify={notify} activeRole={activeRole} session={session} />}
      {activeView === 'extensions' && <ExtensionRequestCenter datasets={datasets} request={request} notify={notify} activeRole={activeRole} />}
      {activeView === 'grading' && <GradingCenter datasets={datasets} request={request} reload={loadCore} notify={notify} activeRole={activeRole} />}
      {activeView === 'reports' && <ReportCenter datasets={datasets} request={request} reload={loadCore} notify={notify} />}
      {activeView === 'api' && <ApiConsole request={request} notify={notify} />}
    </main>{toast && <Toast {...toast} />}
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

  const publishedGrades = (datasets.grades || []).filter((grade) => grade.published)
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
    STUDENT: {
      title: 'Tableau de bord etudiant',
      description: 'Consultation de votre projet, de votre equipe, de la progression FYP et des notes publiees.',
      metrics: [['Mes projets', datasets.projects?.length ?? 0], ['Mon equipe', datasets.teams?.length ?? 0], ['Progression', Math.min(100, (publishedGrades.length || 0) * 50) + '%'], ['Notes publiees', publishedGrades.length]],
      actions: [['Voir mes notes publiees', 'grading'], ['Actualiser mes donnees', 'dashboard']],
      primaryTitle: 'Projet et equipe', primaryRows: datasets.projects || [], primaryColumns: ['title','academicYear','status','track'],
      secondaryTitle: 'Notes publiees', secondaryRows: publishedGrades, secondaryColumns: ['phaseType','weightedScore','finalScore','published'],
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
      actions: [['Evaluer Demo Day', 'evaluations'], ['Demander une prolongation', 'extensions'], ['Actualiser les equipes', 'dashboard']],
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

  return <section className="page-grid">
    <div className="section-head full-span"><div><h2>{dashboard.title}</h2><p>{dashboard.description}</p></div></div>
    <div className="metric-grid full-span">{(dashboard.metrics || []).map(([label, value]) => <div className="metric" key={label}><span>{label}</span><strong>{value}</strong></div>)}</div>
    <Panel title="Actions de votre espace" accent="green"><div className="action-row">{actions.map(([label, view]) => <button className="soft-button" key={label} onClick={() => setActiveView(view)}>{label}</button>)}</div><div className="tag-list">{roleActions.map((action) => <span key={action}>{action}</span>)}</div></Panel>
    <Panel title="Modules autorises" accent="gold"><div className="tag-list">{allowedViews.map((view) => <span key={view.id}>{view.label}</span>)}</div><Checklist items={rolePanels} done={rolePanels} /></Panel>
    <Panel title={dashboard.primaryTitle} wide><DataTable rows={dashboard.primaryRows || []} columns={dashboard.primaryColumns || []} compact /></Panel>
    <Panel title={dashboard.secondaryTitle} wide><DataTable rows={dashboard.secondaryRows || []} columns={dashboard.secondaryColumns || []} compact /></Panel>
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
  const [csvText, setCsvText] = useState('')
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

  async function importCsv() {
    const [head, ...lines] = csvText.trim().split(/\r?\n/); if (!head) return
    const headers = head.split(',').map((x) => x.trim()); setBusy(true)
    try {
      for (const line of lines) {
        const values = line.split(',').map((x) => x.trim())
        await request(config.customCreateEndpoint || config.endpoint, { method: 'POST', body: JSON.stringify(Object.fromEntries(headers.map((h, i) => [h, values[i] || '']))) })
      }
      notify('CSV import completed'); setCsvText(''); await reload()
    } catch (error) { notify(error.message, 'danger') } finally { setBusy(false) }
  }

  return <div className="manager-grid">
    <Panel title={config.title} subtitle={config.subtitle} wide><div className="table-toolbar"><input value={filter} onChange={(e) => setFilter(e.target.value)} placeholder="Search records" /><button className="soft-button" onClick={async () => setRows(unwrapList(await request(config.endpoint)))}>Reload</button></div><DataTable rows={filtered} columns={config.columns} onEdit={!config.readOnly ? edit : null} onDelete={!config.readOnly ? remove : null} /></Panel>
    {!config.readOnly && <Panel title={editing ? 'Edit record' : 'Create record'} accent="green"><form className="stack-form compact" onSubmit={submit}>{(config.fields || []).map((field) => <DynamicField key={field.name} field={field} value={form[field.name]} datasets={datasets} onChange={(value) => setForm({ ...form, [field.name]: value })} />)}<button className="primary-action" disabled={busy}>{busy ? 'Saving...' : editing ? 'Update' : 'Create'}</button>{editing && <button type="button" className="ghost-button" onClick={() => { setEditing(null); setForm(initialForm(config.fields || [])) }}>Cancel edit</button>}</form></Panel>}
    {!config.readOnly && <Panel title="Import CSV" accent="gold"><p className="muted">Paste CSV using field names as headers. Useful for bulk users, tracks and projects.</p><textarea className="csv-box" value={csvText} onChange={(e) => setCsvText(e.target.value)} placeholder="title,academicYear,status,trackId" /><button className="soft-button" onClick={importCsv} disabled={busy}>Importer le CSV</button></Panel>}
  </div>
}
function ImportCenter({ request, notify }) {
  const [kind, setKind] = useState('students')
  const [file, setFile] = useState(null)
  const [preview, setPreview] = useState(null)
  const [busy, setBusy] = useState(false)

  async function analyze() {
    setBusy(true)
    try {
      const parsed = await readImportFile(file, kind)
      const normalized = parsed.rows.map((row, index) => normalizeImportRow(row, kind, index))
      const errors = normalized.flatMap((row) => row.errors)
      setPreview({ ...parsed, normalized, errors })
      notify(errors.length ? 'Fichier analysé avec des lignes à corriger' : 'Fichier prêt pour l’import', errors.length ? 'danger' : 'success')
    } catch (error) {
      setPreview(null); notify(error.message, 'danger')
    } finally { setBusy(false) }
  }

  async function importToServer() {
    if (!file || !preview || preview.errors.length) return
    const body = new FormData()
    body.append('file', file)
    body.append('type', kind)
    setBusy(true)
    try {
      await request('/api/import/' + kind, { method: 'POST', body })
      notify(preview.normalized.length + ' lignes importées')
    } catch (error) { notify(error.message, 'danger') } finally { setBusy(false) }
  }

  const columns = kind === 'students'
    ? ['Identifiant', 'Nom', 'Prénom', 'Filière', 'État']
    : ['Identifiant', 'Nom complet', 'E-mail', 'Rôle', 'État']

  return <section className="import-workspace">
    <div className="section-head"><div><span className="eyebrow">Administration des données</span><h2>Imports Excel</h2><p>Listes académiques sans création de compte étudiant.</p></div><a className="soft-button download-template" href="/modele_import_fyp_etudiants_professeurs.xlsx" download>Télécharger le modèle vierge</a></div>
    <div className="import-mode" role="tablist"><button className={kind === 'students' ? 'active' : ''} onClick={() => { setKind('students'); setPreview(null) }}>Étudiants</button><button className={kind === 'professors' ? 'active' : ''} onClick={() => { setKind('professors'); setPreview(null) }}>Professeurs</button></div>
    <section className="import-dropzone">
      <div><strong>{file?.name || 'Sélectionner un fichier Excel ou CSV'}</strong><span>{file ? formatFileSize(file.size) : '.xlsx ou .csv'}</span></div>
      <label className="file-picker"><input type="file" accept=".xlsx,.csv" onChange={(event) => { setFile(event.target.files?.[0] || null); setPreview(null) }} /><span>Choisir le fichier</span></label>
      <button className="primary-action" disabled={!file || busy} onClick={analyze}>{busy ? 'Analyse…' : 'Analyser'}</button>
    </section>
    {preview && <>
      <section className="import-summary"><div><span>Feuille</span><strong>{preview.sheetName}</strong></div><div><span>Lignes détectées</span><strong>{preview.normalized.length}</strong></div><div><span>Valides</span><strong>{preview.normalized.length - preview.errors.length}</strong></div><div><span>À corriger</span><strong className={preview.errors.length ? 'danger-text' : ''}>{preview.errors.length}</strong></div></section>
      {preview.errors.length > 0 && <div className="import-errors">{preview.errors.slice(0, 8).map((error) => <span key={error}>{error}</span>)}</div>}
      <section className="import-preview"><div className="section-head"><div><h3>Aperçu avant import</h3><p>{preview.normalized.length} lignes · {preview.headers.length} colonnes source</p></div></div><div className="table-wrap"><table><thead><tr>{columns.map((column) => <th key={column}>{column}</th>)}</tr></thead><tbody>{preview.normalized.slice(0, 12).map((row) => <tr key={row.rowNumber}>{kind === 'students' ? <><td>{row.studentNumber}</td><td>{row.lastName}</td><td>{row.firstName}</td><td>{row.trackCode || '-'}</td></> : <><td>{row.universityId}</td><td>{row.fullName}</td><td>{row.email}</td><td>{pretty(row.role)}</td></>}<td><span className={'validation-state ' + (row.errors.length ? 'invalid' : 'valid')}>{row.errors.length ? 'À corriger' : 'Valide'}</span></td></tr>)}</tbody></table></div></section>
      <div className="import-actions"><button className="primary-action" disabled={busy || preview.errors.length > 0 || preview.normalized.length === 0} onClick={importToServer}>Importer dans la plateforme</button></div>
    </>}
  </section>
}

function normalizeImportLabel(value) {
  return String(value).normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[^a-z0-9]+/g, ' ').trim()
}

function importValue(row, aliases) {
  const entry = Object.entries(row).find(([key]) => aliases.includes(normalizeImportLabel(key)))
  return entry?.[1]?.trim() || ''
}

function normalizeImportRow(row, kind, index) {
  const rowNumber = index + 1
  const errors = []
  if (kind === 'students') {
    const normalized = {
      rowNumber,
      studentNumber: importValue(row, ['student id', 'student number', 'identifiant etudiant', 'id etudiant', 'id']),
      lastName: importValue(row, ['nom', 'last name']),
      firstName: importValue(row, ['prenom', 'first name']),
      trackCode: importValue(row, ['filiere', 'track code', 'track']),
      errors,
    }
    if (!normalized.studentNumber) errors.push('Ligne ' + rowNumber + ': identifiant étudiant manquant')
    if (!normalized.lastName) errors.push('Ligne ' + rowNumber + ': nom manquant')
    if (!normalized.firstName) errors.push('Ligne ' + rowNumber + ': prénom manquant')
    return normalized
  }
  const normalized = {
    rowNumber,
    universityId: importValue(row, ['professeur id', 'professor id', 'university id', 'identifiant', 'id']),
    fullName: importValue(row, ['nom complet', 'full name', 'nom']),
    email: importValue(row, ['email', 'e mail', 'adresse e mail']),
    role: importValue(row, ['role plateforme', 'role']) || 'FACULTY_EVALUATOR',
    errors,
  }
  if (!normalized.universityId) errors.push('Ligne ' + rowNumber + ': identifiant professeur manquant')
  if (!normalized.fullName) errors.push('Ligne ' + rowNumber + ': nom complet manquant')
  if (!normalized.email || !normalized.email.includes('@')) errors.push('Ligne ' + rowNumber + ': e-mail invalide')
  return normalized
}

function formatFileSize(bytes) {
  if (bytes < 1024) return bytes + ' octets'
  return (bytes / 1024).toLocaleString(currentLocale(), { maximumFractionDigits: 1 }) + ' Ko'
}

function EvaluationStudio({ datasets, request, notify, activeRole, session }) {
  const [draft, setDraft] = useState({ projectId: '', phaseId: '', evaluatorId: '', evaluationType: 'ORAL_PHASE_I', trackCode: 'CSN', generalComment: '' })
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
  const template = SCORING_TEMPLATES[draft.evaluationType] || SCORING_TEMPLATES.ORAL_PHASE_I
  const sheetId = [draft.trackCode, draft.projectId || 'apercu', draft.phaseId || 'phase', draft.evaluatorId || 'evaluator', draft.evaluationType].join(':')
  const activeScores = scoreDrafts[sheetId] || {}
  const status = sheetStatuses[sheetId] || 'DRAFT'
  const locked = status === 'SUBMITTED' || status === 'LOCKED'
  const selectedPhase = (datasets.phases || []).find((phase) => phase.id === draft.phaseId)
  const evaluationBlocked = Boolean(draft.phaseId && phaseAccess && !phaseAccess.allowed)
  const editingDisabled = locked || !draft.phaseId || phaseAccess?.allowed !== true
  const contextReady = Boolean(draft.projectId && draft.phaseId && draft.evaluatorId && draft.evaluationType)
  const allEvaluators = datasets.evaluators || []
  const evaluatorOptions = activeRole === 'ADMIN' ? allEvaluators : allEvaluators.filter((evaluator) => {
    const user = evaluator.user || {}
    return user.id === session.userId || user.email === session.email
  })

  const selectedTeam = (datasets.teams || []).find((team) => {
    const linkedProjectId = team.project?.id || team.projectId
    return linkedProjectId && linkedProjectId === draft.projectId
  })
  const importedStudents = (selectedTeam?.students || []).map(toStudentTarget)
  const studentTargets = importedStudents.length ? importedStudents : [
    { id: 'student-01', label: 'Étudiant 01', secondary: 'ID-01' },
    { id: 'student-02', label: 'Étudiant 02', secondary: 'ID-02' },
    { id: 'student-03', label: 'Étudiant 03', secondary: 'ID-03' },
  ]
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
      if (!silent) notify('Sélectionnez un projet, une phase ouverte et votre profil évaluateur.', 'danger')
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
      <label className="field"><span>Filière</span><select value={draft.trackCode} onChange={(event) => setDraft({ ...draft, trackCode: event.target.value })}>{['CSN', 'CSP', 'EIC', 'PSE'].map((track) => <option key={track}>{track}</option>)}</select></label>
      <SelectData label="Projet" value={draft.projectId} data={datasets.projects || []} onChange={(projectId) => { setDraft({ ...draft, projectId }); loadProjectEvaluations(projectId) }} />
      <label className="field"><span>Fiche</span><select value={draft.evaluationType} onChange={(event) => setDraft({ ...draft, evaluationType: event.target.value })}>{EVALUATION_TYPES.map((type) => <option key={type} value={type}>{SCORING_TEMPLATES[type]?.label || pretty(type)} · {SCORING_TEMPLATES[type]?.phase || ''}</option>)}</select></label>
      <SelectData label="Phase" value={draft.phaseId} data={datasets.phases || []} onChange={(phaseId) => { setPhaseAccess(null); setDraft({ ...draft, phaseId }) }} />
      <SelectData label="Évaluateur" value={draft.evaluatorId} data={evaluatorOptions} onChange={(evaluatorId) => setDraft({ ...draft, evaluatorId })} />
    </section>

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
      <div className="completion-meter"><span>{completedCells}/{requiredCells} cellules</span><div><i style={{ width: Math.round((completedCells / requiredCells) * 100) + '%' }} /></div></div>
      <button className="soft-button" type="button" onClick={() => setShowFormula((value) => !value)}>{showFormula ? 'Masquer le calcul' : 'Afficher le calcul'}</button>
    </div>

    {showFormula && <FormulaPanel template={template} />}

    <div className={'score-sheet ' + (locked ? 'locked' : '')}>
      {template.sections.map((section) => <ExcelScoreSection
        key={section.id}
        section={section}
        targets={section.target === 'student' ? studentTargets : [{ id: 'group', label: selectedTeam?.name || 'Note du groupe', secondary: draft.trackCode }]}
        scores={activeScores}
        onScoreChange={updateScore}
        disabled={editingDisabled}
      />)}
    </div>

    <ResultSummary template={template} targets={resultTargets} results={results} />

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
  const profile = student.user || student
  const fullName = profile.fullName || [student.firstName, student.lastName].filter(Boolean).join(' ')
  return {
    id: student.id || profile.id || 'student-' + index,
    label: fullName || 'Étudiant ' + String(index + 1).padStart(2, '0'),
    secondary: student.studentNumber || profile.universityId || '',
  }
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
function GradingCenter({ datasets, request, reload, notify, activeRole }) {
  const canManageGrades = activeRole === 'ADMIN'
  const [projectId, setProjectId] = useState(datasets.projects?.[0]?.id || '')
  const [phaseId, setPhaseId] = useState(datasets.phases?.[0]?.id || '')
  const [grades, setGrades] = useState(datasets.grades || [])
  async function loadGrades(id = projectId) { if (id) setGrades(unwrapList(await request('/api/grades/project/' + id))) }
  async function calculate() { try { const res = await request('/api/grades/calculate/project/' + projectId + '/phase/' + phaseId, { method: 'POST' }); notify('Grade calculated'); setGrades([res.data, ...grades.filter((g) => g.id !== res.data.id)]); await reload() } catch (error) { notify(error.message, 'danger') } }
  async function publish(id) { try { await request('/api/grades/' + id + '/publish', { method: 'PATCH' }); notify('Grade published'); await loadGrades() } catch (error) { notify(error.message, 'danger') } }
  const visibleGrades = activeRole === 'STUDENT' ? grades.filter((grade) => grade.published) : grades
  return <section className="page-grid"><div className="section-head full-span"><div><h2>{canManageGrades ? 'Grade center' : 'Notes consolidees'}</h2><p>{canManageGrades ? 'Calculate and publish grades from locked submissions.' : 'Consultation des notes autorisees pour votre role.'}</p></div></div>{canManageGrades && <Panel title="Calculation controls" accent="green"><div className="form-grid two"><SelectData label="Project" value={projectId} data={datasets.projects || []} onChange={(v) => { setProjectId(v); loadGrades(v) }} /><SelectData label="Phase" value={phaseId} data={datasets.phases || []} onChange={setPhaseId} /></div><button className="primary-action" onClick={calculate}>Calculate grade</button></Panel>}<Panel title="Grade rules" accent="gold"><p className="muted">Chaque fiche produit une note sur 10 selon le bareme Excel correspondant. La consolidation s effectue apr?s validation des fiches.</p><div className="tag-list">{EVALUATION_TYPES.map((type) => <span key={type}>{pretty(type)}</span>)}</div></Panel><Panel title={activeRole === 'STUDENT' ? 'Mes notes publiees' : 'Grades'} wide><DataTable rows={visibleGrades} columns={['phaseType','rawScore','weightedScore','finalScore','published']} compact extraAction={(row) => canManageGrades && !row.published && <button className="mini-button" onClick={() => publish(row.id)}>Publish</button>} /></Panel></section>
}

function ReportCenter({ datasets, request, reload, notify }) {
  const [projectId, setProjectId] = useState(datasets.projects?.[0]?.id || '')
  const [phaseId, setPhaseId] = useState(datasets.phases?.[0]?.id || '')
  const [reports, setReports] = useState(datasets.reports || [])
  async function generate(finalReport) { try { const path = finalReport ? '/api/reports/project/' + projectId + '/final' : '/api/reports/project/' + projectId + '/phase/' + phaseId; const res = await request(path, { method: 'POST' }); notify('Report generated'); setReports([res.data, ...reports]); await reload() } catch (error) { notify(error.message, 'danger') } }
  async function send(id) { try { await request('/api/reports/' + id + '/send', { method: 'POST' }); notify('Report sent'); setReports(unwrapList(await request('/api/reports'))); await reload() } catch (error) { notify(error.message, 'danger') } }
  return <section className="page-grid"><div className="section-head full-span"><div><h2>Reporting and collection</h2><p>Generate reports and trace emails sent to the FYP coordinator.</p></div></div><Panel title="Report generator" accent="green"><div className="form-grid two"><SelectData label="Project" value={projectId} data={datasets.projects || []} onChange={setProjectId} /><SelectData label="Phase" value={phaseId} data={datasets.phases || []} onChange={setPhaseId} /></div><div className="action-row"><button className="soft-button" onClick={() => generate(false)}>Generate phase report</button><button className="primary-action" onClick={() => generate(true)}>Generate final report</button></div></Panel><Panel title="Notifications" accent="gold"><DataTable rows={datasets.notifications || []} columns={['recipient','subject','status','sentAt']} compact /></Panel><Panel title="Report archive" wide><DataTable rows={reports} columns={['project','phase','status','recipientEmail','generatedAt','sentAt']} compact extraAction={(row) => row.status !== 'SENT' && <button className="mini-button" onClick={() => send(row.id)}>Send</button>} /></Panel></section>
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

function Panel({ title, subtitle, children, accent = 'blue', wide = false }) {
  return <section className={'panel accent-' + accent + (wide ? ' wide' : '')}><div className="panel-heading"><div><h3>{title}</h3>{subtitle && <p>{subtitle}</p>}</div></div>{children}</section>
}

function DataTable({ rows = [], columns = [], onEdit, onDelete, compact = false, extraAction }) {
  if (!rows.length) return <EmptyState />
  return <div className={'table-wrap ' + (compact ? 'compact' : '')}><table><thead><tr>{columns.map((column) => <th key={column}>{pretty(column)}</th>)}{(onEdit || onDelete || extraAction) && <th>Actions</th>}</tr></thead><tbody>{rows.map((row, index) => <tr key={row.id || index}>{columns.map((column) => <td key={column}>{renderCell(row[column], column)}</td>)}{(onEdit || onDelete || extraAction) && <td className="row-actions">{extraAction?.(row)}{onEdit && <button className="mini-button" onClick={() => onEdit(row)}>Edit</button>}{onDelete && <button className="mini-button danger" onClick={() => onDelete(row)}>Delete</button>}</td>}</tr>)}</tbody></table></div>
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
  return <div className="empty-state"><div className="empty-icon">i</div><h3>{title}</h3><p>{detail}</p></div>
}

function Checklist({ items, done }) {
  return <ul className="checklist">{items.map((item) => <li key={item} className={done.includes(item) ? 'done' : ''}><span>{done.includes(item) ? 'OK' : '--'}</span>{item}</li>)}</ul>
}

function Toast({ message, type }) {
  return <div className={'toast ' + type}>{message}</div>
}

export default App
