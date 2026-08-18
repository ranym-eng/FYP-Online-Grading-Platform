export const ROLES = ['ADMIN', 'SUPERVISOR', 'FACULTY_EVALUATOR', 'INDUSTRY_REPRESENTATIVE', 'COORDINATOR']
export const PHASE_TYPES = ['PHASE_I', 'PHASE_II']
export const PHASE_STATUS = ['NOT_STARTED', 'OPEN', 'CLOSED', 'ARCHIVED']
export const EVALUATION_TYPES = ['SUPERVISOR_PHASE_I', 'REPORT_PHASE_I', 'ORAL_PHASE_I', 'SUPERVISOR_PHASE_II', 'REPORT_PHASE_II', 'ORAL_PHASE_II', 'DEMO_DAY_INDUSTRY']

export const actorTemplates = {
  ADMIN: {
    title: 'Espace administrateur',
    summary: 'Gestion des comptes, filières, projets, équipes, phases, modèles, publications et journaux d’audit.',
    actions: ['Créer les comptes', 'Affecter les équipes', 'Ouvrir les phases', 'Publier les notes', 'Envoyer les rapports'],
    panels: ['Vue d’ensemble', 'Matrice de progression', 'Évaluations en attente', 'Journal d’audit'],
  },
  SUPERVISOR: {
    title: 'Espace superviseur',
    summary: 'Évaluation des projets encadrés pour FYP I et FYP II avec brouillon, validation et verrouillage.',
    actions: ['Ouvrir les projets affectés', 'Enregistrer le brouillon', 'Valider la fiche'],
    panels: ['Projets affectés', 'Fiches du superviseur', 'État des validations'],
  },
  FACULTY_EVALUATOR: {
    title: 'Espace évaluateur académique',
    summary: 'Évaluation des rapports et soutenances pour FYP I et FYP II.',
    actions: ['Évaluer le rapport', 'Évaluer la soutenance', 'Consulter les fiches soumises'],
    panels: ['Fiches de rapport', 'Fiches de soutenance', 'Évaluations en attente'],
  },
  INDUSTRY_REPRESENTATIVE: {
    title: 'Espace représentant industriel',
    summary: 'Évaluation Demo Day de la qualité du prototype, de sa pertinence et de sa maturité industrielle.',
    actions: ['Ouvrir les équipes Demo Day', 'Noter le prototype', 'Valider les observations'],
    panels: ['Équipes Demo Day', 'Barème du prototype', 'Fiches soumises'],
  },
  COORDINATOR: {
    title: 'Espace coordinateur FYP',
    summary: 'Consolidation des rapports, notes finales, états d’avancement et traces d’envoi.',
    actions: ['Contrôler les rapports', 'Recevoir les notes finales', 'Consulter les journaux'],
    panels: ['Rapports reçus', 'Progression par phase', 'Exports des notes finales'],
  },
}

export const views = [
  { id: 'dashboard', label: 'Tableau de bord', roles: ROLES },
  { id: 'calendar', label: 'Calendrier FYP', roles: ROLES },
  { id: 'notifications', label: 'Notifications', roles: ROLES },
  { id: 'imports', label: 'Imports Excel', roles: ['ADMIN'] },
  { id: 'crud', label: 'Gestion des données', roles: ['ADMIN'] },
  { id: 'evaluations', label: 'Évaluations', roles: ['SUPERVISOR', 'FACULTY_EVALUATOR', 'INDUSTRY_REPRESENTATIVE'] },
  { id: 'extensions', label: 'Prolongations', roles: ['ADMIN', 'SUPERVISOR', 'FACULTY_EVALUATOR', 'INDUSTRY_REPRESENTATIVE'] },
  { id: 'grading', label: 'Notes consolidees', roles: ['ADMIN', 'INDUSTRY_REPRESENTATIVE', 'COORDINATOR'] },
  { id: 'reports', label: 'Rapports', roles: ['ADMIN', 'COORDINATOR'] },
  { id: 'api', label: 'Console API', roles: ['ADMIN'] },
]

export const resourceConfigs = {
  users: {
    title: 'Users', endpoint: '/api/users', subtitle: 'All platform accounts and RBAC roles.',
    columns: ['universityId', 'fullName', 'email', 'role', 'status'],
    fields: [
      { name: 'universityId', label: 'University ID', required: true },
      { name: 'fullName', label: 'Full name', required: true },
      { name: 'email', label: 'Email', type: 'email', required: true },
      { name: 'phone', label: 'Phone' },
      { name: 'password', label: 'Password', type: 'password', placeholder: 'Leave blank on update' },
      { name: 'role', label: 'Role', type: 'select', options: ROLES, required: true },
    ],
  },
  students: {
    title: 'Étudiants', endpoint: '/api/students', subtitle: 'Référentiel académique officiel importé depuis la base SQU.',
    columns: ['studentNumber', 'cohort', 'fullName', 'email', 'trackCode'],
    fields: [
      { name: 'studentNumber', label: 'stdID', required: true, placeholder: '142430' },
      { name: 'cohort', label: 'Cohorte', required: true, placeholder: '2022' },
      { name: 'fullName', label: 'Nom complet officiel', required: true },
      { name: 'email', label: 'E-mail SQU', type: 'email', required: true, placeholder: 's142430@student.squ.edu.om' },
      { name: 'academicYear', label: 'Année académique FYP', placeholder: '2026-2027' },
      { name: 'trackCode', label: 'Filière FYP', type: 'select', options: ['EIC', 'CSN', 'CSP', 'PSE'] },
      { name: 'level', label: 'Niveau', defaultValue: 'Final Year' },
    ],
  },
  evaluators: {
    title: 'Evaluators', endpoint: '/api/evaluators', subtitle: 'Supervisor, faculty and industry profiles.',
    columns: ['user', 'department', 'specialization', 'externalOrganization', 'external'],
    fields: [
      { name: 'userId', label: 'Evaluator user', type: 'selectData', source: 'users', filterRoles: ['SUPERVISOR', 'FACULTY_EVALUATOR', 'INDUSTRY_REPRESENTATIVE'], required: true },
      { name: 'department', label: 'Department', defaultValue: 'Electrical Engineering' },
      { name: 'specialization', label: 'Specialization' },
      { name: 'externalOrganization', label: 'External organization' },
      { name: 'external', label: 'External', type: 'checkbox' },
    ],
  },
  tracks: {
    title: 'Tracks', endpoint: '/api/tracks', subtitle: 'Academic tracks: EIC, CSN, CSP and PSE.',
    columns: ['code', 'name', 'description', 'active'],
    fields: [{ name: 'code', label: 'Code', required: true }, { name: 'name', label: 'Name', required: true }, { name: 'description', label: 'Description', type: 'textarea' }],
  },
  projects: {
    title: 'Projets', endpoint: '/api/projects', subtitle: 'Projets FYP identifiés par le numéro utilisé sur les fiches d’évaluation.',
    columns: ['projectNumber', 'title', 'academicYear', 'status', 'track'],
    fields: [
      { name: 'projectNumber', label: 'Numéro de projet', required: true, placeholder: 'PSE-01' },
      { name: 'title', label: 'Titre du projet', required: true },
      { name: 'abstractText', label: 'Résumé', type: 'textarea' },
      { name: 'academicYear', label: 'Année académique', defaultValue: '2026-2027', required: true },
      { name: 'trackId', label: 'Filière', type: 'selectData', source: 'tracks', required: true },
      { name: 'status', label: 'État', defaultValue: 'ACTIVE' },
    ],
  },
  teams: {
    title: 'Teams', endpoint: '/api/teams', subtitle: 'Teams and student membership.',
    columns: ['name', 'section', 'academicYear', 'project', 'students'],
    fields: [
      { name: 'name', label: 'Team name', required: true },
      { name: 'section', label: 'Section' },
      { name: 'academicYear', label: 'Academic year', defaultValue: '2025-2026', required: true },
      { name: 'projectId', label: 'Project', type: 'selectData', source: 'projects', required: true },
      { name: 'studentIds', label: 'Student IDs', type: 'multiData', source: 'students' },
    ],
  },
  phases: {
    title: 'Phases', endpoint: '/api/phases', subtitle: 'FYP I and FYP II windows and deadlines.',
    columns: ['name', 'type', 'academicYear', 'status', 'startDate', 'deadline', 'durationDays'],
    fields: [
      { name: 'type', label: 'Phase type', type: 'select', options: PHASE_TYPES, required: true },
      { name: 'name', label: 'Name', required: true },
      { name: 'academicYear', label: 'Academic year', defaultValue: '2025-2026', required: true },
      { name: 'startDate', label: 'Start date', type: 'datetime-local', required: true },
      { name: 'deadline', label: 'Deadline', type: 'datetime-local', required: true },
      { name: 'status', label: 'Status', type: 'select', options: PHASE_STATUS, defaultValue: 'OPEN' },
    ],
  },
  forms: {
    title: 'Evaluation forms', endpoint: '/api/evaluation-forms', subtitle: 'Dynamic templates per role and phase.',
    columns: ['name', 'evaluationType', 'phaseType', 'totalWeight', 'active'],
    fields: [
      { name: 'name', label: 'Form name', required: true },
      { name: 'evaluationType', label: 'Evaluation type', type: 'select', options: EVALUATION_TYPES, required: true },
      { name: 'phaseType', label: 'Phase type', type: 'select', options: PHASE_TYPES, required: true },
      { name: 'description', label: 'Description', type: 'textarea' },
      { name: 'totalWeight', label: 'Total weight', type: 'number', defaultValue: 100 },
    ],
  },
  reports: { title: 'Reports', endpoint: '/api/reports', subtitle: 'Generated phase and final reports.', columns: ['project', 'phase', 'status', 'recipientEmail', 'generatedAt', 'sentAt'], readOnly: true },
  notifications: {
    title: 'Notifications', endpoint: '/api/notifications', customCreateEndpoint: '/api/notifications/email', subtitle: 'Email messages, retries and reminders.', columns: ['recipient', 'subject', 'status', 'sentAt'],
    fields: [{ name: 'recipient', label: 'Recipient', type: 'email', required: true }, { name: 'subject', label: 'Subject', required: true }, { name: 'body', label: 'Body', type: 'textarea' }, { name: 'attachmentPath', label: 'Attachment path' }],
  },
  audit: { title: 'Audit logs', endpoint: '/api/audit', subtitle: 'Trace of sensitive actions.', columns: ['action', 'entityType', 'entityId', 'userId', 'createdAt'], readOnly: true },
}
