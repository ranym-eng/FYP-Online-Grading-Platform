export const ROLES = ['ADMIN', 'SUPERVISOR', 'REPORT_EVALUATOR', 'FACULTY_EVALUATOR', 'INDUSTRY_REPRESENTATIVE', 'COORDINATOR']
export const PHASE_TYPES = ['PHASE_I', 'PHASE_II']
export const PHASE_STATUS = ['NOT_STARTED', 'OPEN', 'CLOSED', 'ARCHIVED']
export const EVALUATION_TYPES = ['SUPERVISOR_PHASE_I', 'REPORT_PHASE_I', 'ORAL_PHASE_I', 'SUPERVISOR_PHASE_II', 'REPORT_PHASE_II', 'ORAL_PHASE_II', 'DEMO_DAY_INDUSTRY']

export const actorTemplates = {
  ADMIN: {
    title: 'Administrator workspace',
    summary: 'Manage accounts, tracks, projects, teams, phases, templates, publications and audit logs.',
    actions: ['Create accounts', 'Assign teams', 'Open phases', 'Publish grades', 'Send reports'],
    panels: ['Overview', 'Progress matrix', 'Pending evaluations', 'Audit log'],
  },
  SUPERVISOR: {
    title: 'Supervisor workspace',
    summary: 'Evaluate supervised FYP I and FYP II projects with drafts, submission and locking.',
    actions: ['Open assigned projects', 'Save draft', 'Submit form'],
    panels: ['Assigned projects', 'Supervisor forms', 'Submission status'],
  },
  REPORT_EVALUATOR: {
    title: 'Report evaluator workspace',
    summary: 'Evaluate Report I and Report II paper reports for assigned projects.',
    actions: ['Open assigned reports', 'Save draft', 'Submit form'],
    panels: ['Assigned reports', 'Report I and Report II', 'Pending evaluations'],
  },
  FACULTY_EVALUATOR: {
    title: 'Faculty evaluator workspace',
    summary: 'Evaluate FYP I and FYP II oral presentations.',
    actions: ['Evaluate presentation', 'Save draft', 'Submit form'],
    panels: ['Assigned presentations', 'FYP I and FYP II', 'Pending evaluations'],
  },
  INDUSTRY_REPRESENTATIVE: {
    title: 'Industry representative workspace',
    summary: 'Evaluate prototype quality, relevance and industry readiness during Demo Day.',
    actions: ['Open Demo Day teams', 'Grade prototype', 'Submit observations'],
    panels: ['Demo Day teams', 'Prototype rubric', 'Submitted forms'],
  },
  COORDINATOR: {
    title: 'FYP coordinator workspace',
    summary: 'Consolidate reports, final grades, progress states and delivery records.',
    actions: ['Review reports', 'Receive final grades', 'View logs'],
    panels: ['Received reports', 'Progress by phase', 'Final grade exports'],
  },
}

export const views = [
  { id: 'dashboard', label: 'Dashboard', roles: ROLES },
  { id: 'calendar', label: 'FYP calendar', roles: ROLES },
  { id: 'notifications', label: 'Notifications', roles: ROLES },
  { id: 'imports', label: 'Excel imports', roles: ['ADMIN'] },
  { id: 'crud', label: 'Data management', roles: ['ADMIN'] },
  { id: 'evaluations', label: 'Evaluations', roles: ['SUPERVISOR', 'REPORT_EVALUATOR', 'FACULTY_EVALUATOR', 'INDUSTRY_REPRESENTATIVE'] },
  { id: 'extensions', label: 'Extensions', roles: ['ADMIN', 'SUPERVISOR', 'REPORT_EVALUATOR', 'FACULTY_EVALUATOR', 'INDUSTRY_REPRESENTATIVE'] },
  { id: 'grading', label: 'Consolidated grades', roles: ['ADMIN', 'INDUSTRY_REPRESENTATIVE', 'COORDINATOR'] },
  { id: 'reports', label: 'Reports', roles: ['ADMIN', 'COORDINATOR'] },
]

export const resourceConfigs = {
  users: {
    title: 'Accounts and access', endpoint: '/api/users', subtitle: 'Pre-register an account or activate it immediately with a temporary password sent by email.',
    columns: ['universityId', 'fullName', 'email', 'role', 'status', 'passwordChangeRequired', 'accessExpiresAt'],
    fields: [
      { name: 'universityId', label: 'University ID', required: true },
      { name: 'fullName', label: 'Full name', required: true },
      { name: 'email', label: 'Email', type: 'email', required: true },
      { name: 'phone', label: 'Phone' },
      { name: 'role', label: 'Role', type: 'select', options: ROLES, required: true },
      {
        name: 'accessExpiresAt',
        label: 'Industry Guest expiration',
        type: 'datetime-local',
        visibleWhen: { field: 'role', equals: 'INDUSTRY_REPRESENTATIVE' },
      },
      {
        name: 'activateImmediately',
        label: 'Create active account and email a temporary password',
        type: 'checkbox',
        defaultValue: false,
        createOnly: true,
      },
    ],
  },
  students: {
    title: 'Students', endpoint: '/api/students', subtitle: 'Official academic records imported from the SQU database.',
    columns: ['studentNumber', 'cohort', 'fullName', 'email', 'trackCode'],
    fields: [
      { name: 'studentNumber', label: 'stdID', required: true, placeholder: '142430' },
      { name: 'cohort', label: 'Cohort', required: true, placeholder: '2022' },
      { name: 'fullName', label: 'Official full name', required: true },
      { name: 'email', label: 'SQU email', type: 'email', required: true, placeholder: 's142430@student.squ.edu.om' },
      { name: 'academicYear', label: 'FYP academic year', placeholder: '2026-2027' },
      { name: 'trackCode', label: 'FYP track', type: 'select', options: ['EIC', 'CSN', 'CSP', 'PSE'] },
      { name: 'level', label: 'Level', defaultValue: 'Final Year' },
    ],
  },
  evaluators: {
    title: 'Evaluators', endpoint: '/api/evaluators', subtitle: 'Supervisor, report, oral and industry profiles.',
    columns: ['user', 'department', 'specialization', 'externalOrganization', 'external'],
    fields: [
      { name: 'userId', label: 'Evaluator user', type: 'selectData', source: 'users', filterRoles: ['SUPERVISOR', 'REPORT_EVALUATOR', 'FACULTY_EVALUATOR', 'INDUSTRY_REPRESENTATIVE'], required: true },
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
    title: 'Projects', endpoint: '/api/projects', subtitle: 'FYP projects identified by the number used on evaluation forms.',
    columns: ['projectNumber', 'title', 'academicYear', 'status', 'track'],
    fields: [
      { name: 'projectNumber', label: 'Project number', required: true, placeholder: 'PSE-01' },
      { name: 'title', label: 'Project title', required: true },
      { name: 'abstractText', label: 'Abstract', type: 'textarea' },
      { name: 'academicYear', label: 'Academic year', defaultValue: '2026-2027', required: true },
      { name: 'trackId', label: 'Track', type: 'selectData', source: 'tracks', required: true },
      { name: 'status', label: 'Status', defaultValue: 'ACTIVE' },
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
