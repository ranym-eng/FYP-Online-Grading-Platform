\set ON_ERROR_STOP on

BEGIN;

-- ============================================================
-- FYP Online Grading Platform - idempotent demonstration data
-- Existing business data is preserved. Demo rows use DEMO-* keys.
-- Demo actor password: Test@123
-- Default administrator: admin@squ.edu.om / Admin@123
-- ============================================================

-- 1. Official tracks (normally created by DataInitializer).
INSERT INTO tracks (id, created_at, updated_at, active, code, description, name)
VALUES
('10000000-0000-0000-0000-000000000001', now(), now(), true, 'EIC', 'Electronics, Instrumentation and Control', 'Electronics, Instrumentation and Control'),
('10000000-0000-0000-0000-000000000002', now(), now(), true, 'CSN', 'Communication and Signal Networks', 'Communication and Signal Networks'),
('10000000-0000-0000-0000-000000000003', now(), now(), true, 'CSP', 'Computer Systems and Programming', 'Computer Systems and Programming'),
('10000000-0000-0000-0000-000000000004', now(), now(), true, 'PSE', 'Power Systems Engineering', 'Power Systems Engineering')
ON CONFLICT (code) DO UPDATE SET
  name = excluded.name,
  description = excluded.description,
  active = true,
  updated_at = now();

-- 2. One account for every authenticated actor.
-- BCrypt value below matches Test@123.
INSERT INTO app_users
  (id, created_at, updated_at, email, full_name, password_hash, phone, role, status, university_id)
VALUES
('20000000-0000-0000-0000-000000000001', now(), now(), 'demo.supervisor@squ.edu.om', 'Dr Ahmed Al Balushi', '$2a$10$IvbPJqjMvSVP0TPRgwJ4C.Cy0lxtUwaIYbOvdo9a3kFpzfrosPj9W', '+968 9000 0001', 'SUPERVISOR', 'ACTIVE', 'DEMO-SUP-001'),
('20000000-0000-0000-0000-000000000002', now(), now(), 'demo.faculty@squ.edu.om', 'Dr Fatma Al Hinai', '$2a$10$IvbPJqjMvSVP0TPRgwJ4C.Cy0lxtUwaIYbOvdo9a3kFpzfrosPj9W', '+968 9000 0002', 'FACULTY_EVALUATOR', 'ACTIVE', 'DEMO-FAC-001'),
('20000000-0000-0000-0000-000000000003', now(), now(), 'demo.industry@squ.edu.om', 'Eng Khalid Al Rawahi', '$2a$10$IvbPJqjMvSVP0TPRgwJ4C.Cy0lxtUwaIYbOvdo9a3kFpzfrosPj9W', '+968 9000 0003', 'INDUSTRY_REPRESENTATIVE', 'ACTIVE', 'DEMO-IND-001'),
('20000000-0000-0000-0000-000000000004', now(), now(), 'demo.coordinator@squ.edu.om', 'Dr Maryam Al Harthy', '$2a$10$IvbPJqjMvSVP0TPRgwJ4C.Cy0lxtUwaIYbOvdo9a3kFpzfrosPj9W', '+968 9000 0004', 'COORDINATOR', 'ACTIVE', 'DEMO-COO-001')
ON CONFLICT (email) DO UPDATE SET
  full_name = excluded.full_name,
  password_hash = excluded.password_hash,
  phone = excluded.phone,
  role = excluded.role,
  status = 'ACTIVE',
  updated_at = now();

-- 3. Academic student records. Students have no platform account.
INSERT INTO student_profiles
  (id, created_at, updated_at, academic_year, cohort, email, full_name, level, student_number, track_code)
VALUES
('30000000-0000-0000-0000-000000000001', now(), now(), '2026-2027', '2023', 's20270001@student.squ.edu.om', 'Ali Al Harthy', 'Final Year', '20270001', 'PSE'),
('30000000-0000-0000-0000-000000000002', now(), now(), '2026-2027', '2023', 's20270002@student.squ.edu.om', 'Maha Al Balushi', 'Final Year', '20270002', 'PSE'),
('30000000-0000-0000-0000-000000000003', now(), now(), '2026-2027', '2023', 's20270003@student.squ.edu.om', 'Noor Al Hinai', 'Final Year', '20270003', 'CSP'),
('30000000-0000-0000-0000-000000000004', now(), now(), '2026-2027', '2023', 's20270004@student.squ.edu.om', 'Salim Al Habsi', 'Final Year', '20270004', 'CSP'),
('30000000-0000-0000-0000-000000000005', now(), now(), '2026-2027', '2023', 's20270005@student.squ.edu.om', 'Aisha Al Siyabi', 'Final Year', '20270005', 'EIC'),
('30000000-0000-0000-0000-000000000006', now(), now(), '2026-2027', '2023', 's20270006@student.squ.edu.om', 'Omar Al Farsi', 'Final Year', '20270006', 'EIC')
ON CONFLICT (student_number) DO UPDATE SET
  academic_year = excluded.academic_year,
  cohort = excluded.cohort,
  email = excluded.email,
  full_name = excluded.full_name,
  level = excluded.level,
  track_code = excluded.track_code,
  updated_at = now();

-- 4. Evaluator profiles linked to actor accounts.
INSERT INTO evaluator_profiles
  (id, created_at, updated_at, department, external, external_organization, specialization, user_id)
SELECT '40000000-0000-0000-0000-000000000001', now(), now(), 'Electrical Engineering', false, null, 'Power Systems and project supervision', id
FROM app_users WHERE email = 'demo.supervisor@squ.edu.om'
ON CONFLICT (user_id) DO UPDATE SET department = excluded.department, specialization = excluded.specialization, external = false, updated_at = now();

INSERT INTO evaluator_profiles
  (id, created_at, updated_at, department, external, external_organization, specialization, user_id)
SELECT '40000000-0000-0000-0000-000000000002', now(), now(), 'Electrical Engineering', false, null, 'Technical reports and oral defenses', id
FROM app_users WHERE email = 'demo.faculty@squ.edu.om'
ON CONFLICT (user_id) DO UPDATE SET department = excluded.department, specialization = excluded.specialization, external = false, updated_at = now();

INSERT INTO evaluator_profiles
  (id, created_at, updated_at, department, external, external_organization, specialization, user_id)
SELECT '40000000-0000-0000-0000-000000000003', now(), now(), 'Industry', true, 'Oman Digital Solutions', 'Prototype and industry Demo Day evaluation', id
FROM app_users WHERE email = 'demo.industry@squ.edu.om'
ON CONFLICT (user_id) DO UPDATE SET department = excluded.department, specialization = excluded.specialization, external_organization = excluded.external_organization, external = true, updated_at = now();

-- 5. Three projects, one main evaluator demonstration per project.
INSERT INTO projects
  (id, created_at, updated_at, abstract_text, academic_year, project_number, status, title, track_id)
VALUES
('50000000-0000-0000-0000-000000000001', now(), now(), 'Monitoring and forecasting for smart electrical grids using sensors and analytics.', '2026-2027', 'DEMO-PSE-01', 'ACTIVE', 'Smart Grid Monitoring System', (SELECT id FROM tracks WHERE code = 'PSE')),
('50000000-0000-0000-0000-000000000002', now(), now(), 'A secure platform for automating university Final Year Project assessment workflows.', '2026-2027', 'DEMO-CSP-02', 'ACTIVE', 'AI Assisted FYP Grading Platform', (SELECT id FROM tracks WHERE code = 'CSP')),
('50000000-0000-0000-0000-000000000003', now(), now(), 'An intelligent prototype for real-time laboratory energy optimisation.', '2026-2027', 'DEMO-EIC-03', 'ACTIVE', 'Intelligent Laboratory Energy Controller', (SELECT id FROM tracks WHERE code = 'EIC'))
ON CONFLICT (project_number) DO UPDATE SET
  title = excluded.title,
  abstract_text = excluded.abstract_text,
  academic_year = excluded.academic_year,
  status = 'ACTIVE',
  track_id = excluded.track_id,
  updated_at = now();

-- 6. Teams and membership.
INSERT INTO teams (id, created_at, updated_at, academic_year, name, section, project_id)
VALUES
('60000000-0000-0000-0000-000000000001', now(), now(), '2026-2027', 'Team Smart Grid', 'PSE-A', (SELECT id FROM projects WHERE project_number = 'DEMO-PSE-01')),
('60000000-0000-0000-0000-000000000002', now(), now(), '2026-2027', 'Team Digital Assessment', 'CSP-A', (SELECT id FROM projects WHERE project_number = 'DEMO-CSP-02')),
('60000000-0000-0000-0000-000000000003', now(), now(), '2026-2027', 'Team Energy Lab', 'EIC-A', (SELECT id FROM projects WHERE project_number = 'DEMO-EIC-03'))
ON CONFLICT (project_id) DO UPDATE SET name = excluded.name, section = excluded.section, academic_year = excluded.academic_year, updated_at = now();

INSERT INTO team_members (team_id, students_id)
VALUES
((SELECT id FROM teams WHERE project_id = (SELECT id FROM projects WHERE project_number = 'DEMO-PSE-01')), (SELECT id FROM student_profiles WHERE student_number = '20270001')),
((SELECT id FROM teams WHERE project_id = (SELECT id FROM projects WHERE project_number = 'DEMO-PSE-01')), (SELECT id FROM student_profiles WHERE student_number = '20270002')),
((SELECT id FROM teams WHERE project_id = (SELECT id FROM projects WHERE project_number = 'DEMO-CSP-02')), (SELECT id FROM student_profiles WHERE student_number = '20270003')),
((SELECT id FROM teams WHERE project_id = (SELECT id FROM projects WHERE project_number = 'DEMO-CSP-02')), (SELECT id FROM student_profiles WHERE student_number = '20270004')),
((SELECT id FROM teams WHERE project_id = (SELECT id FROM projects WHERE project_number = 'DEMO-EIC-03')), (SELECT id FROM student_profiles WHERE student_number = '20270005')),
((SELECT id FROM teams WHERE project_id = (SELECT id FROM projects WHERE project_number = 'DEMO-EIC-03')), (SELECT id FROM student_profiles WHERE student_number = '20270006'))
ON CONFLICT DO NOTHING;

-- 7. Dynamic deadlines make reminders and extension scenarios reproducible.
INSERT INTO phases (id, created_at, updated_at, academic_year, deadline, name, start_date, status, type)
VALUES
('70000000-0000-0000-0000-000000000001', now(), now(), '2026-2027', now() + interval '23 hours', 'FYP I - Demonstration Window', now() - interval '7 days', 'OPEN', 'PHASE_I'),
('70000000-0000-0000-0000-000000000002', now(), now(), '2026-2027', now() + interval '11 hours', 'FYP II - Demo Day Window', now() - interval '7 days', 'OPEN', 'PHASE_II'),
('70000000-0000-0000-0000-000000000003', now(), now(), '2026-2027', now() - interval '1 day', 'FYP I - Expired Extension Example', now() - interval '30 days', 'OPEN', 'PHASE_I')
ON CONFLICT (id) DO UPDATE SET
  academic_year = excluded.academic_year,
  deadline = excluded.deadline,
  name = excluded.name,
  start_date = excluded.start_date,
  status = excluded.status,
  type = excluded.type,
  updated_at = now();

-- 8. Supervisor links for all demo projects.
INSERT INTO project_supervisor_assignments (id, created_at, updated_at, active, project_id, supervisor_id)
SELECT ids.assignment_id, now(), now(), true, p.id, ep.id
FROM (VALUES
  ('80000000-0000-0000-0000-000000000001'::uuid, 'DEMO-PSE-01'),
  ('80000000-0000-0000-0000-000000000002'::uuid, 'DEMO-CSP-02'),
  ('80000000-0000-0000-0000-000000000003'::uuid, 'DEMO-EIC-03')
) AS ids(assignment_id, project_number)
JOIN projects p ON p.project_number = ids.project_number
JOIN app_users u ON u.email = 'demo.supervisor@squ.edu.om'
JOIN evaluator_profiles ep ON ep.user_id = u.id
ON CONFLICT (id) DO UPDATE SET active = true, project_id = excluded.project_id, supervisor_id = excluded.supervisor_id, updated_at = now();

-- 9. Evaluator assignments used by backend authorization.
WITH assignment_data(id, project_number, evaluator_email, evaluation_type) AS (
  VALUES
  ('81000000-0000-0000-0000-000000000001'::uuid, 'DEMO-PSE-01', 'demo.supervisor@squ.edu.om', 'SUPERVISOR_PHASE_I'),
  ('81000000-0000-0000-0000-000000000002'::uuid, 'DEMO-PSE-01', 'demo.faculty@squ.edu.om', 'REPORT_PHASE_I'),
  ('81000000-0000-0000-0000-000000000003'::uuid, 'DEMO-PSE-01', 'demo.faculty@squ.edu.om', 'ORAL_PHASE_I'),
  ('81000000-0000-0000-0000-000000000004'::uuid, 'DEMO-CSP-02', 'demo.supervisor@squ.edu.om', 'SUPERVISOR_PHASE_II'),
  ('81000000-0000-0000-0000-000000000005'::uuid, 'DEMO-CSP-02', 'demo.faculty@squ.edu.om', 'REPORT_PHASE_II'),
  ('81000000-0000-0000-0000-000000000006'::uuid, 'DEMO-CSP-02', 'demo.faculty@squ.edu.om', 'ORAL_PHASE_II'),
  ('81000000-0000-0000-0000-000000000007'::uuid, 'DEMO-CSP-02', 'demo.industry@squ.edu.om', 'DEMO_DAY_INDUSTRY'),
  ('81000000-0000-0000-0000-000000000008'::uuid, 'DEMO-EIC-03', 'demo.supervisor@squ.edu.om', 'SUPERVISOR_PHASE_II'),
  ('81000000-0000-0000-0000-000000000009'::uuid, 'DEMO-EIC-03', 'demo.faculty@squ.edu.om', 'REPORT_PHASE_II'),
  ('81000000-0000-0000-0000-000000000010'::uuid, 'DEMO-EIC-03', 'demo.faculty@squ.edu.om', 'ORAL_PHASE_II'),
  ('81000000-0000-0000-0000-000000000011'::uuid, 'DEMO-EIC-03', 'demo.industry@squ.edu.om', 'DEMO_DAY_INDUSTRY')
)
INSERT INTO project_evaluator_assignments
  (id, created_at, updated_at, active, evaluation_type, evaluator_id, project_id)
SELECT d.id, now(), now(), true, d.evaluation_type, ep.id, p.id
FROM assignment_data d
JOIN projects p ON p.project_number = d.project_number
JOIN app_users u ON u.email = d.evaluator_email
JOIN evaluator_profiles ep ON ep.user_id = u.id
ON CONFLICT (id) DO UPDATE SET active = true, evaluation_type = excluded.evaluation_type, evaluator_id = excluded.evaluator_id, project_id = excluded.project_id, updated_at = now();

-- 10. Locked prerequisite evaluations. Drafts are inserted separately below.
WITH locked_data(id, project_number, phase_id, evaluator_email, evaluation_type, total_score, comment, required_count) AS (
  VALUES
  ('a0000000-0000-0000-0000-000000000002'::uuid, 'DEMO-PSE-01', '70000000-0000-0000-0000-000000000001'::uuid, 'demo.faculty@squ.edu.om', 'REPORT_PHASE_I', 8.20, 'Validated report evaluation for the demo.', 20),
  ('a0000000-0000-0000-0000-000000000003'::uuid, 'DEMO-PSE-01', '70000000-0000-0000-0000-000000000001'::uuid, 'demo.faculty@squ.edu.om', 'ORAL_PHASE_I', 8.50, 'Validated oral defense evaluation for the demo.', 11),
  ('a0000000-0000-0000-0000-000000000004'::uuid, 'DEMO-CSP-02', '70000000-0000-0000-0000-000000000002'::uuid, 'demo.supervisor@squ.edu.om', 'SUPERVISOR_PHASE_II', 8.80, 'Validated supervisor evaluation.', 22),
  ('a0000000-0000-0000-0000-000000000006'::uuid, 'DEMO-CSP-02', '70000000-0000-0000-0000-000000000002'::uuid, 'demo.faculty@squ.edu.om', 'ORAL_PHASE_II', 8.40, 'Validated oral defense evaluation.', 11),
  ('a0000000-0000-0000-0000-000000000007'::uuid, 'DEMO-CSP-02', '70000000-0000-0000-0000-000000000002'::uuid, 'demo.industry@squ.edu.om', 'DEMO_DAY_INDUSTRY', 9.10, 'Validated industry Demo Day evaluation.', 5),
  ('a0000000-0000-0000-0000-000000000008'::uuid, 'DEMO-EIC-03', '70000000-0000-0000-0000-000000000002'::uuid, 'demo.supervisor@squ.edu.om', 'SUPERVISOR_PHASE_II', 8.30, 'Validated supervisor evaluation.', 22),
  ('a0000000-0000-0000-0000-000000000009'::uuid, 'DEMO-EIC-03', '70000000-0000-0000-0000-000000000002'::uuid, 'demo.faculty@squ.edu.om', 'REPORT_PHASE_II', 8.10, 'Validated final report evaluation.', 20),
  ('a0000000-0000-0000-0000-000000000010'::uuid, 'DEMO-EIC-03', '70000000-0000-0000-0000-000000000002'::uuid, 'demo.faculty@squ.edu.om', 'ORAL_PHASE_II', 8.60, 'Validated final oral evaluation.', 11)
)
INSERT INTO evaluation_submissions
  (id, created_at, updated_at, completed_score_count, draft_saved_at, evaluation_type, general_comment, locked, locked_at, required_score_count, score_payload, status, submitted_at, total_score, evaluator_id, form_template_id, phase_id, project_id)
SELECT d.id, now(), now(), d.required_count, now(), d.evaluation_type, d.comment, true, now(), d.required_count, null, 'LOCKED', now(), d.total_score,
       ep.id,
       (SELECT id FROM evaluation_form_templates WHERE active = true AND evaluation_type = d.evaluation_type ORDER BY created_at LIMIT 1),
       d.phase_id,
       p.id
FROM locked_data d
JOIN projects p ON p.project_number = d.project_number
JOIN app_users u ON u.email = d.evaluator_email
JOIN evaluator_profiles ep ON ep.user_id = u.id
ON CONFLICT (id) DO UPDATE SET
  created_at = excluded.created_at,
  updated_at = now(),
  completed_score_count = excluded.completed_score_count,
  draft_saved_at = excluded.draft_saved_at,
  general_comment = excluded.general_comment,
  locked = true,
  locked_at = excluded.locked_at,
  required_score_count = excluded.required_score_count,
  score_payload = null,
  status = 'LOCKED',
  submitted_at = excluded.submitted_at,
  total_score = excluded.total_score,
  evaluator_id = excluded.evaluator_id,
  form_template_id = excluded.form_template_id,
  phase_id = excluded.phase_id,
  project_id = excluded.project_id;

-- 11. Almost complete drafts: fill one remaining cell during the live demo.
INSERT INTO evaluation_submissions
  (id, created_at, updated_at, completed_score_count, draft_saved_at, evaluation_type, general_comment, locked, required_score_count, score_payload, status, total_score, evaluator_id, form_template_id, phase_id, project_id)
SELECT
  'a0000000-0000-0000-0000-000000000001', now(), now(), 21, now(), 'SUPERVISOR_PHASE_I',
  'Draft: complete the final proposal deadline score for Maha, then validate.', false, 22,
  jsonb_build_object(
    'individual:analyze-solutions:30000000-0000-0000-0000-000000000001',8,
    'individual:build-test:30000000-0000-0000-0000-000000000001',8,
    'individual:professional-responsibility:30000000-0000-0000-0000-000000000001',9,
    'individual:plan-objectives:30000000-0000-0000-0000-000000000001',8,
    'individual:assigned-tasks:30000000-0000-0000-0000-000000000001',8,
    'individual:team-leadership:30000000-0000-0000-0000-000000000001',7,
    'individual:acquire-information:30000000-0000-0000-0000-000000000001',8,
    'individual:learning-strategies:30000000-0000-0000-0000-000000000001',8,
    'individual:apply-knowledge:30000000-0000-0000-0000-000000000001',8,
    'individual:technical-questions:30000000-0000-0000-0000-000000000001',8,
    'individual:proposal-deadline:30000000-0000-0000-0000-000000000001',9,
    'individual:analyze-solutions:30000000-0000-0000-0000-000000000002',7,
    'individual:build-test:30000000-0000-0000-0000-000000000002',8,
    'individual:professional-responsibility:30000000-0000-0000-0000-000000000002',8,
    'individual:plan-objectives:30000000-0000-0000-0000-000000000002',7,
    'individual:assigned-tasks:30000000-0000-0000-0000-000000000002',8,
    'individual:team-leadership:30000000-0000-0000-0000-000000000002',8,
    'individual:acquire-information:30000000-0000-0000-0000-000000000002',7,
    'individual:learning-strategies:30000000-0000-0000-0000-000000000002',8,
    'individual:apply-knowledge:30000000-0000-0000-0000-000000000002',8,
    'individual:technical-questions:30000000-0000-0000-0000-000000000002',7
  )::text,
  'DRAFT', 7.91,
  (SELECT ep.id FROM evaluator_profiles ep JOIN app_users u ON u.id = ep.user_id WHERE u.email = 'demo.supervisor@squ.edu.om'),
  (SELECT id FROM evaluation_form_templates WHERE active = true AND evaluation_type = 'SUPERVISOR_PHASE_I' ORDER BY created_at LIMIT 1),
  '70000000-0000-0000-0000-000000000001',
  (SELECT id FROM projects WHERE project_number = 'DEMO-PSE-01')
ON CONFLICT (id) DO UPDATE SET
  created_at = excluded.created_at, updated_at = now(), completed_score_count = 21, draft_saved_at = now(),
  general_comment = excluded.general_comment, locked = false, locked_at = null, required_score_count = 22,
  score_payload = excluded.score_payload, status = 'DRAFT', submitted_at = null, total_score = excluded.total_score,
  evaluator_id = excluded.evaluator_id, form_template_id = excluded.form_template_id, phase_id = excluded.phase_id, project_id = excluded.project_id;

INSERT INTO evaluation_submissions
  (id, created_at, updated_at, completed_score_count, draft_saved_at, evaluation_type, general_comment, locked, required_score_count, score_payload, status, total_score, evaluator_id, form_template_id, phase_id, project_id)
SELECT
  'a0000000-0000-0000-0000-000000000005', now(), now(), 19, now(), 'REPORT_PHASE_II',
  'Draft: complete the final work score for Salim, then validate.', false, 20,
  jsonb_build_object(
    'individual:identify-problem:30000000-0000-0000-0000-000000000003',8,
    'individual:formulate-problem:30000000-0000-0000-0000-000000000003',8,
    'individual:design-requirements:30000000-0000-0000-0000-000000000003',9,
    'individual:analyze-solutions:30000000-0000-0000-0000-000000000003',8,
    'individual:develop-solutions:30000000-0000-0000-0000-000000000003',8,
    'individual:build-test:30000000-0000-0000-0000-000000000003',8,
    'individual:technical-report:30000000-0000-0000-0000-000000000003',9,
    'individual:professional-ethics:30000000-0000-0000-0000-000000000003',9,
    'individual:evaluate-impact:30000000-0000-0000-0000-000000000003',8,
    'individual:complete-work:30000000-0000-0000-0000-000000000003',8,
    'individual:identify-problem:30000000-0000-0000-0000-000000000004',7,
    'individual:formulate-problem:30000000-0000-0000-0000-000000000004',8,
    'individual:design-requirements:30000000-0000-0000-0000-000000000004',8,
    'individual:analyze-solutions:30000000-0000-0000-0000-000000000004',8,
    'individual:develop-solutions:30000000-0000-0000-0000-000000000004',7,
    'individual:build-test:30000000-0000-0000-0000-000000000004',8,
    'individual:technical-report:30000000-0000-0000-0000-000000000004',8,
    'individual:professional-ethics:30000000-0000-0000-0000-000000000004',9,
    'individual:evaluate-impact:30000000-0000-0000-0000-000000000004',8
  )::text,
  'DRAFT', 8.09,
  (SELECT ep.id FROM evaluator_profiles ep JOIN app_users u ON u.id = ep.user_id WHERE u.email = 'demo.faculty@squ.edu.om'),
  (SELECT id FROM evaluation_form_templates WHERE active = true AND evaluation_type = 'REPORT_PHASE_II' ORDER BY created_at LIMIT 1),
  '70000000-0000-0000-0000-000000000002',
  (SELECT id FROM projects WHERE project_number = 'DEMO-CSP-02')
ON CONFLICT (id) DO UPDATE SET
  created_at = excluded.created_at, updated_at = now(), completed_score_count = 19, draft_saved_at = now(),
  general_comment = excluded.general_comment, locked = false, locked_at = null, required_score_count = 20,
  score_payload = excluded.score_payload, status = 'DRAFT', submitted_at = null, total_score = excluded.total_score,
  evaluator_id = excluded.evaluator_id, form_template_id = excluded.form_template_id, phase_id = excluded.phase_id, project_id = excluded.project_id;

INSERT INTO evaluation_submissions
  (id, created_at, updated_at, completed_score_count, draft_saved_at, evaluation_type, general_comment, locked, required_score_count, score_payload, status, total_score, evaluator_id, form_template_id, phase_id, project_id)
SELECT
  'a0000000-0000-0000-0000-000000000011', now(), now(), 4, now(), 'DEMO_DAY_INDUSTRY',
  'Draft: complete the poster score, then validate the Industry Guest sheet.', false, 5,
  jsonb_build_object(
    'group:prototype:group',9,
    'group:present-prototype:group',8,
    'group:answer-questions:group',9,
    'group:complete-work:group',8
  )::text,
  'DRAFT', 8.60,
  (SELECT ep.id FROM evaluator_profiles ep JOIN app_users u ON u.id = ep.user_id WHERE u.email = 'demo.industry@squ.edu.om'),
  (SELECT id FROM evaluation_form_templates WHERE active = true AND evaluation_type = 'DEMO_DAY_INDUSTRY' ORDER BY created_at LIMIT 1),
  '70000000-0000-0000-0000-000000000002',
  (SELECT id FROM projects WHERE project_number = 'DEMO-EIC-03')
ON CONFLICT (id) DO UPDATE SET
  created_at = excluded.created_at, updated_at = now(), completed_score_count = 4, draft_saved_at = now(),
  general_comment = excluded.general_comment, locked = false, locked_at = null, required_score_count = 5,
  score_payload = excluded.score_payload, status = 'DRAFT', submitted_at = null, total_score = excluded.total_score,
  evaluator_id = excluded.evaluator_id, form_template_id = excluded.form_template_id, phase_id = excluded.phase_id, project_id = excluded.project_id;

-- 12. Personal welcome notifications for the bell demonstration.
INSERT INTO email_notifications
  (id, created_at, updated_at, action_view, body, category, deduplication_key, recipient, recipient_user_id, sent_at, severity, status, subject)
SELECT
  ('f0000000-0000-0000-0000-' || right(replace(u.id::text, '-', ''), 12))::uuid,
  now(), now(), 'dashboard',
  'Your demonstration workspace is ready. Open your assigned workflow from the dashboard.',
  'SYSTEM', 'DEMO_WELCOME:' || u.id, u.email, u.id, now(), 'INFO', 'IN_APP', 'FYP demonstration workspace ready'
FROM app_users u
WHERE u.email IN ('demo.supervisor@squ.edu.om', 'demo.faculty@squ.edu.om', 'demo.industry@squ.edu.om', 'demo.coordinator@squ.edu.om')
ON CONFLICT (deduplication_key) DO NOTHING;

-- 13. Initial audit marker.
INSERT INTO audit_logs
  (id, created_at, updated_at, action, entity_id, entity_type, ip_address, new_value, old_value, user_agent, user_id)
SELECT
  'ab000000-0000-0000-0000-000000000001', now(), now(), 'DEMO_DATA_PREPARED',
  (SELECT id FROM projects WHERE project_number = 'DEMO-PSE-01'), 'DemoDataset', '127.0.0.1',
  'Three demo projects, actor accounts, assignments, phases and draft workflows prepared.', null,
  'seed_all_tables.sql', id
FROM app_users WHERE email = 'admin@squ.edu.om'
ON CONFLICT (id) DO UPDATE SET updated_at = now(), new_value = excluded.new_value;

COMMIT;

-- Compact verification shown in the CLI.
SELECT 'demo_users' AS item, count(*) AS total FROM app_users WHERE email LIKE 'demo.%@squ.edu.om'
UNION ALL SELECT 'demo_students', count(*) FROM student_profiles WHERE student_number LIKE '2027000%'
UNION ALL SELECT 'demo_projects', count(*) FROM projects WHERE project_number LIKE 'DEMO-%'
UNION ALL SELECT 'demo_teams', count(*) FROM teams WHERE academic_year = '2026-2027'
UNION ALL SELECT 'demo_phases', count(*) FROM phases WHERE id::text LIKE '70000000-%'
UNION ALL SELECT 'demo_assignments', count(*) FROM project_evaluator_assignments WHERE id::text LIKE '81000000-%'
UNION ALL SELECT 'demo_drafts', count(*) FROM evaluation_submissions WHERE id::text LIKE 'a0000000-%' AND status = 'DRAFT'
UNION ALL SELECT 'demo_locked', count(*) FROM evaluation_submissions WHERE id::text LIKE 'a0000000-%' AND status = 'LOCKED';
