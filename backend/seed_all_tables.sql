\c fyp_grading_platform

BEGIN;

-- ============================================================
-- Seed complet pour Online FYP Grading Platform
-- Mot de passe des utilisateurs de test: Test@123
-- ============================================================

-- 1. Tracks
INSERT INTO tracks (id, created_at, updated_at, active, code, description, name)
VALUES
('10000000-0000-0000-0000-000000000001', now(), now(), true, 'EIC', 'Electronics, Instrumentation and Control', 'Electronics, Instrumentation and Control'),
('10000000-0000-0000-0000-000000000002', now(), now(), true, 'CSN', 'Communication and Signal Networks', 'Communication and Signal Networks'),
('10000000-0000-0000-0000-000000000003', now(), now(), true, 'CSP', 'Computer Systems and Programming', 'Computer Systems and Programming'),
('10000000-0000-0000-0000-000000000004', now(), now(), true, 'PSE', 'Power Systems Engineering', 'Power Systems Engineering')
ON CONFLICT DO NOTHING;

-- 2. Users
-- Tous ces users ont le mot de passe: Test@123
INSERT INTO app_users (id, created_at, updated_at, email, full_name, password_hash, phone, role, status, university_id)
VALUES
('20000000-0000-0000-0000-000000000001', now(), now(), 'seed.student1@squ.edu.om', 'Ali Al-Harthy', '$2a$10$IvbPJqjMvSVP0TPRgwJ4C.Cy0lxtUwaIYbOvdo9a3kFpzfrosPj9W', '90000001', 'STUDENT', 'ACTIVE', 'SEED-STU-001'),
('20000000-0000-0000-0000-000000000002', now(), now(), 'seed.student2@squ.edu.om', 'Maha Al-Balushi', '$2a$10$IvbPJqjMvSVP0TPRgwJ4C.Cy0lxtUwaIYbOvdo9a3kFpzfrosPj9W', '90000002', 'STUDENT', 'ACTIVE', 'SEED-STU-002'),
('20000000-0000-0000-0000-000000000003', now(), now(), 'seed.supervisor1@squ.edu.om', 'Dr Ahmed Supervisor', '$2a$10$IvbPJqjMvSVP0TPRgwJ4C.Cy0lxtUwaIYbOvdo9a3kFpzfrosPj9W', '90000003', 'SUPERVISOR', 'ACTIVE', 'SEED-SUP-001'),
('20000000-0000-0000-0000-000000000004', now(), now(), 'seed.faculty.report@squ.edu.om', 'Dr Fatma Report Evaluator', '$2a$10$IvbPJqjMvSVP0TPRgwJ4C.Cy0lxtUwaIYbOvdo9a3kFpzfrosPj9W', '90000004', 'FACULTY_EVALUATOR', 'ACTIVE', 'SEED-FAC-001'),
('20000000-0000-0000-0000-000000000005', now(), now(), 'seed.faculty.oral@squ.edu.om', 'Dr Said Oral Evaluator', '$2a$10$IvbPJqjMvSVP0TPRgwJ4C.Cy0lxtUwaIYbOvdo9a3kFpzfrosPj9W', '90000005', 'FACULTY_EVALUATOR', 'ACTIVE', 'SEED-FAC-002'),
('20000000-0000-0000-0000-000000000006', now(), now(), 'seed.industry@squ.edu.om', 'Industry Representative One', '$2a$10$IvbPJqjMvSVP0TPRgwJ4C.Cy0lxtUwaIYbOvdo9a3kFpzfrosPj9W', '90000006', 'INDUSTRY_REPRESENTATIVE', 'ACTIVE', 'SEED-IND-001'),
('20000000-0000-0000-0000-000000000007', now(), now(), 'seed.coordinator@squ.edu.om', 'FYP Coordinator', '$2a$10$IvbPJqjMvSVP0TPRgwJ4C.Cy0lxtUwaIYbOvdo9a3kFpzfrosPj9W', '90000007', 'COORDINATOR', 'ACTIVE', 'SEED-COR-001')
ON CONFLICT DO NOTHING;

-- 3. Student profiles
INSERT INTO student_profiles (id, created_at, updated_at, academic_year, level, student_number, track_code, user_id)
VALUES
('30000000-0000-0000-0000-000000000001', now(), now(), '2025-2026', 'Final Year', 'SEED20260001', 'PSE', '20000000-0000-0000-0000-000000000001'),
('30000000-0000-0000-0000-000000000002', now(), now(), '2025-2026', 'Final Year', 'SEED20260002', 'PSE', '20000000-0000-0000-0000-000000000002')
ON CONFLICT DO NOTHING;

-- 4. Evaluator profiles
INSERT INTO evaluator_profiles (id, created_at, updated_at, department, external, external_organization, specialization, user_id)
VALUES
('40000000-0000-0000-0000-000000000001', now(), now(), 'Electrical Engineering', false, '', 'Power Systems', '20000000-0000-0000-0000-000000000003'),
('40000000-0000-0000-0000-000000000002', now(), now(), 'Electrical Engineering', false, '', 'Technical Report Evaluation', '20000000-0000-0000-0000-000000000004'),
('40000000-0000-0000-0000-000000000003', now(), now(), 'Electrical Engineering', false, '', 'Oral Defense Evaluation', '20000000-0000-0000-0000-000000000005'),
('40000000-0000-0000-0000-000000000004', now(), now(), 'External', true, 'Oman Energy Industry', 'Industry Demo Evaluation', '20000000-0000-0000-0000-000000000006')
ON CONFLICT DO NOTHING;

-- 5. Project
INSERT INTO projects (id, created_at, updated_at, abstract_text, academic_year, status, title, track_id)
VALUES
('50000000-0000-0000-0000-000000000001', now(), now(), 'Final year project about monitoring smart electrical grids using sensors and dashboards.', '2025-2026', 'ACTIVE', 'Smart Grid Monitoring System', (SELECT id FROM tracks WHERE code = 'PSE' LIMIT 1))
ON CONFLICT DO NOTHING;

-- 6. Team
INSERT INTO teams (id, created_at, updated_at, academic_year, name, section, project_id)
VALUES
('60000000-0000-0000-0000-000000000001', now(), now(), '2025-2026', 'PSE Team 01', 'PSE-A', '50000000-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;

-- 7. Team members
INSERT INTO team_members (team_id, students_id)
SELECT '60000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001'
WHERE NOT EXISTS (SELECT 1 FROM team_members WHERE team_id='60000000-0000-0000-0000-000000000001' AND students_id='30000000-0000-0000-0000-000000000001');

INSERT INTO team_members (team_id, students_id)
SELECT '60000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000002'
WHERE NOT EXISTS (SELECT 1 FROM team_members WHERE team_id='60000000-0000-0000-0000-000000000001' AND students_id='30000000-0000-0000-0000-000000000002');

-- 8. Phases
INSERT INTO phases (id, created_at, updated_at, academic_year, deadline, name, start_date, status, type)
VALUES
('70000000-0000-0000-0000-000000000001', now(), now(), '2025-2026', '2026-01-30 23:59:00', 'FYP Part I Evaluation', '2025-09-01 08:00:00', 'OPEN', 'PHASE_I'),
('70000000-0000-0000-0000-000000000002', now(), now(), '2025-2026', '2026-05-30 23:59:00', 'FYP Completion and Demo Day', '2026-02-01 08:00:00', 'OPEN', 'PHASE_II')
ON CONFLICT DO NOTHING;

-- 9. Project assignments
INSERT INTO project_supervisor_assignments (id, created_at, updated_at, active, project_id, supervisor_id)
VALUES
('80000000-0000-0000-0000-000000000001', now(), now(), true, '50000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;

INSERT INTO project_evaluator_assignments (id, created_at, updated_at, active, evaluation_type, evaluator_id, project_id)
VALUES
('81000000-0000-0000-0000-000000000001', now(), now(), true, 'SUPERVISOR_PHASE_I', '40000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001'),
('81000000-0000-0000-0000-000000000002', now(), now(), true, 'REPORT_PHASE_I', '40000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000001'),
('81000000-0000-0000-0000-000000000003', now(), now(), true, 'ORAL_PHASE_I', '40000000-0000-0000-0000-000000000003', '50000000-0000-0000-0000-000000000001'),
('81000000-0000-0000-0000-000000000004', now(), now(), true, 'SUPERVISOR_PHASE_II', '40000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001'),
('81000000-0000-0000-0000-000000000005', now(), now(), true, 'REPORT_PHASE_II', '40000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000001'),
('81000000-0000-0000-0000-000000000006', now(), now(), true, 'ORAL_PHASE_II', '40000000-0000-0000-0000-000000000003', '50000000-0000-0000-0000-000000000001'),
('81000000-0000-0000-0000-000000000007', now(), now(), true, 'DEMO_DAY_INDUSTRY', '40000000-0000-0000-0000-000000000004', '50000000-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;

-- 10. Evaluation forms
INSERT INTO evaluation_form_templates (id, created_at, updated_at, active, description, evaluation_type, name, phase_type, total_weight)
VALUES
('90000000-0000-0000-0000-000000000001', now(), now(), true, 'Supervisor evaluation for Phase I', 'SUPERVISOR_PHASE_I', 'Supervisor Phase I Form', 'PHASE_I', 100),
('90000000-0000-0000-0000-000000000002', now(), now(), true, 'Report evaluation for Phase I', 'REPORT_PHASE_I', 'Report Phase I Form', 'PHASE_I', 100),
('90000000-0000-0000-0000-000000000003', now(), now(), true, 'Oral defense evaluation for Phase I', 'ORAL_PHASE_I', 'Oral Phase I Form', 'PHASE_I', 100),
('90000000-0000-0000-0000-000000000004', now(), now(), true, 'Final supervisor evaluation', 'SUPERVISOR_PHASE_II', 'Supervisor Phase II Form', 'PHASE_II', 100),
('90000000-0000-0000-0000-000000000005', now(), now(), true, 'Final report evaluation', 'REPORT_PHASE_II', 'Report Phase II Form', 'PHASE_II', 100),
('90000000-0000-0000-0000-000000000006', now(), now(), true, 'Final oral defense evaluation', 'ORAL_PHASE_II', 'Oral Phase II Form', 'PHASE_II', 100),
('90000000-0000-0000-0000-000000000007', now(), now(), true, 'Industry Demo Day evaluation', 'DEMO_DAY_INDUSTRY', 'Demo Day Industry Form', 'PHASE_II', 100)
ON CONFLICT DO NOTHING;

-- 11. Rubric criteria, one criterion per form for easy direct CLI seed
INSERT INTO rubric_criteria (id, created_at, updated_at, description, display_order, max_score, required, title, weight, form_template_id)
VALUES
('91000000-0000-0000-0000-000000000001', now(), now(), 'Overall supervisor Phase I assessment', 1, 100, true, 'Overall Supervisor Phase I Score', 1.0, '90000000-0000-0000-0000-000000000001'),
('91000000-0000-0000-0000-000000000002', now(), now(), 'Overall report Phase I assessment', 1, 100, true, 'Overall Report Phase I Score', 1.0, '90000000-0000-0000-0000-000000000002'),
('91000000-0000-0000-0000-000000000003', now(), now(), 'Overall oral Phase I assessment', 1, 100, true, 'Overall Oral Phase I Score', 1.0, '90000000-0000-0000-0000-000000000003'),
('91000000-0000-0000-0000-000000000004', now(), now(), 'Overall final supervisor assessment', 1, 100, true, 'Overall Supervisor Phase II Score', 1.0, '90000000-0000-0000-0000-000000000004'),
('91000000-0000-0000-0000-000000000005', now(), now(), 'Overall final report assessment', 1, 100, true, 'Overall Report Phase II Score', 1.0, '90000000-0000-0000-0000-000000000005'),
('91000000-0000-0000-0000-000000000006', now(), now(), 'Overall final oral assessment', 1, 100, true, 'Overall Oral Phase II Score', 1.0, '90000000-0000-0000-0000-000000000006'),
('91000000-0000-0000-0000-000000000007', now(), now(), 'Overall industry demo assessment', 1, 100, true, 'Overall Demo Day Score', 1.0, '90000000-0000-0000-0000-000000000007')
ON CONFLICT DO NOTHING;

-- 12. Evaluation submissions
INSERT INTO evaluation_submissions (id, created_at, updated_at, draft_saved_at, evaluation_type, general_comment, locked, locked_at, status, submitted_at, total_score, evaluator_id, form_template_id, phase_id, project_id)
VALUES
('a0000000-0000-0000-0000-000000000001', now(), now(), now(), 'SUPERVISOR_PHASE_I', 'Good progress and clear initial design.', true, now(), 'LOCKED', now(), 88, '40000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001'),
('a0000000-0000-0000-0000-000000000002', now(), now(), now(), 'REPORT_PHASE_I', 'Report is well structured with minor methodology gaps.', true, now(), 'LOCKED', now(), 84, '40000000-0000-0000-0000-000000000002', '90000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001'),
('a0000000-0000-0000-0000-000000000003', now(), now(), now(), 'ORAL_PHASE_I', 'Students answered most design questions confidently.', true, now(), 'LOCKED', now(), 86, '40000000-0000-0000-0000-000000000003', '90000000-0000-0000-0000-000000000003', '70000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001'),
('a0000000-0000-0000-0000-000000000004', now(), now(), now(), 'SUPERVISOR_PHASE_II', 'Strong final implementation and consistent teamwork.', true, now(), 'LOCKED', now(), 90, '40000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000004', '70000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000001'),
('a0000000-0000-0000-0000-000000000005', now(), now(), now(), 'REPORT_PHASE_II', 'Final report is complete and clear.', true, now(), 'LOCKED', now(), 87, '40000000-0000-0000-0000-000000000002', '90000000-0000-0000-0000-000000000005', '70000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000001'),
('a0000000-0000-0000-0000-000000000006', now(), now(), now(), 'ORAL_PHASE_II', 'Final defense was convincing and technically sound.', true, now(), 'LOCKED', now(), 89, '40000000-0000-0000-0000-000000000003', '90000000-0000-0000-0000-000000000006', '70000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000001'),
('a0000000-0000-0000-0000-000000000007', now(), now(), now(), 'DEMO_DAY_INDUSTRY', 'Prototype demo was practical and industry relevant.', true, now(), 'LOCKED', now(), 92, '40000000-0000-0000-0000-000000000004', '90000000-0000-0000-0000-000000000007', '70000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;

-- 13. Criterion scores
INSERT INTO criterion_scores (id, created_at, updated_at, comment, score, criterion_id, submission_id)
VALUES
('b0000000-0000-0000-0000-000000000001', now(), now(), 'Supervisor Phase I score', 88, '91000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001'),
('b0000000-0000-0000-0000-000000000002', now(), now(), 'Report Phase I score', 84, '91000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000002'),
('b0000000-0000-0000-0000-000000000003', now(), now(), 'Oral Phase I score', 86, '91000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000003'),
('b0000000-0000-0000-0000-000000000004', now(), now(), 'Supervisor Phase II score', 90, '91000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000004'),
('b0000000-0000-0000-0000-000000000005', now(), now(), 'Report Phase II score', 87, '91000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000005'),
('b0000000-0000-0000-0000-000000000006', now(), now(), 'Oral Phase II score', 89, '91000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000006'),
('b0000000-0000-0000-0000-000000000007', now(), now(), 'Demo Day score', 92, '91000000-0000-0000-0000-000000000007', 'a0000000-0000-0000-0000-000000000007')
ON CONFLICT DO NOTHING;

-- 14. Grade rules, insert only if the same rule does not already exist
INSERT INTO grade_rules (id, created_at, updated_at, active, evaluation_type, phase_type, weight)
SELECT 'c0000000-0000-0000-0000-000000000001', now(), now(), true, 'SUPERVISOR_PHASE_I', 'PHASE_I', 40
WHERE NOT EXISTS (SELECT 1 FROM grade_rules WHERE phase_type='PHASE_I' AND evaluation_type='SUPERVISOR_PHASE_I');
INSERT INTO grade_rules (id, created_at, updated_at, active, evaluation_type, phase_type, weight)
SELECT 'c0000000-0000-0000-0000-000000000002', now(), now(), true, 'REPORT_PHASE_I', 'PHASE_I', 35
WHERE NOT EXISTS (SELECT 1 FROM grade_rules WHERE phase_type='PHASE_I' AND evaluation_type='REPORT_PHASE_I');
INSERT INTO grade_rules (id, created_at, updated_at, active, evaluation_type, phase_type, weight)
SELECT 'c0000000-0000-0000-0000-000000000003', now(), now(), true, 'ORAL_PHASE_I', 'PHASE_I', 25
WHERE NOT EXISTS (SELECT 1 FROM grade_rules WHERE phase_type='PHASE_I' AND evaluation_type='ORAL_PHASE_I');
INSERT INTO grade_rules (id, created_at, updated_at, active, evaluation_type, phase_type, weight)
SELECT 'c0000000-0000-0000-0000-000000000004', now(), now(), true, 'SUPERVISOR_PHASE_II', 'PHASE_II', 30
WHERE NOT EXISTS (SELECT 1 FROM grade_rules WHERE phase_type='PHASE_II' AND evaluation_type='SUPERVISOR_PHASE_II');
INSERT INTO grade_rules (id, created_at, updated_at, active, evaluation_type, phase_type, weight)
SELECT 'c0000000-0000-0000-0000-000000000005', now(), now(), true, 'REPORT_PHASE_II', 'PHASE_II', 25
WHERE NOT EXISTS (SELECT 1 FROM grade_rules WHERE phase_type='PHASE_II' AND evaluation_type='REPORT_PHASE_II');
INSERT INTO grade_rules (id, created_at, updated_at, active, evaluation_type, phase_type, weight)
SELECT 'c0000000-0000-0000-0000-000000000006', now(), now(), true, 'ORAL_PHASE_II', 'PHASE_II', 25
WHERE NOT EXISTS (SELECT 1 FROM grade_rules WHERE phase_type='PHASE_II' AND evaluation_type='ORAL_PHASE_II');
INSERT INTO grade_rules (id, created_at, updated_at, active, evaluation_type, phase_type, weight)
SELECT 'c0000000-0000-0000-0000-000000000007', now(), now(), true, 'DEMO_DAY_INDUSTRY', 'PHASE_II', 20
WHERE NOT EXISTS (SELECT 1 FROM grade_rules WHERE phase_type='PHASE_II' AND evaluation_type='DEMO_DAY_INDUSTRY');

-- 15. Grades
INSERT INTO grades (id, created_at, updated_at, final_score, phase_type, published, raw_score, weighted_score, phase_id, project_id)
VALUES
('d0000000-0000-0000-0000-000000000001', now(), now(), 86.10, 'PHASE_I', true, 86.10, 86.10, '70000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001'),
('d0000000-0000-0000-0000-000000000002', now(), now(), 89.40, 'PHASE_II', true, 89.40, 89.40, '70000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;

-- 16. Reports
INSERT INTO reports (id, created_at, updated_at, content_snapshot, file_path, generated_at, recipient_email, sent_at, status, phase_id, project_id)
VALUES
('e0000000-0000-0000-0000-000000000001', now(), now(), 'Phase I report for Smart Grid Monitoring System. Final Phase I score: 86.10.', 'generated/phase-i-smart-grid.pdf', now(), 'seed.coordinator@squ.edu.om', now(), 'SENT', '70000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001'),
('e0000000-0000-0000-0000-000000000002', now(), now(), 'Phase II final report for Smart Grid Monitoring System. Final Phase II score: 89.40.', 'generated/phase-ii-smart-grid.pdf', now(), 'seed.coordinator@squ.edu.om', now(), 'SENT', '70000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;

-- 17. Email notifications
INSERT INTO email_notifications (id, created_at, updated_at, attachment_path, body, recipient, sent_at, status, subject)
VALUES
('f0000000-0000-0000-0000-000000000001', now(), now(), 'generated/phase-ii-smart-grid.pdf', 'Final FYP grade report for Smart Grid Monitoring System has been generated and sent.', 'seed.coordinator@squ.edu.om', now(), 'SENT', 'FYP Final Grade Report')
ON CONFLICT DO NOTHING;

-- 18. Audit logs
INSERT INTO audit_logs (id, created_at, updated_at, action, entity_id, entity_type, ip_address, new_value, old_value, user_agent, user_id)
VALUES
('ab000000-0000-0000-0000-000000000001', now(), now(), 'SEED_PROJECT_CREATED', '50000000-0000-0000-0000-000000000001', 'Project', '127.0.0.1', 'Smart Grid Monitoring System', null, 'psql seed script', '20000000-0000-0000-0000-000000000007'),
('ab000000-0000-0000-0000-000000000002', now(), now(), 'SEED_EVALUATIONS_SUBMITTED', '50000000-0000-0000-0000-000000000001', 'EvaluationSubmission', '127.0.0.1', 'All required evaluations inserted as LOCKED', null, 'psql seed script', '20000000-0000-0000-0000-000000000007'),
('ab000000-0000-0000-0000-000000000003', now(), now(), 'SEED_REPORT_SENT', 'e0000000-0000-0000-0000-000000000002', 'Report', '127.0.0.1', 'Report sent to coordinator', null, 'psql seed script', '20000000-0000-0000-0000-000000000007')
ON CONFLICT DO NOTHING;

COMMIT;

-- Verification rapide
SELECT 'tracks' AS table_name, count(*) FROM tracks
UNION ALL SELECT 'app_users', count(*) FROM app_users
UNION ALL SELECT 'student_profiles', count(*) FROM student_profiles
UNION ALL SELECT 'evaluator_profiles', count(*) FROM evaluator_profiles
UNION ALL SELECT 'projects', count(*) FROM projects
UNION ALL SELECT 'teams', count(*) FROM teams
UNION ALL SELECT 'phases', count(*) FROM phases
UNION ALL SELECT 'project_evaluator_assignments', count(*) FROM project_evaluator_assignments
UNION ALL SELECT 'evaluation_form_templates', count(*) FROM evaluation_form_templates
UNION ALL SELECT 'rubric_criteria', count(*) FROM rubric_criteria
UNION ALL SELECT 'evaluation_submissions', count(*) FROM evaluation_submissions
UNION ALL SELECT 'criterion_scores', count(*) FROM criterion_scores
UNION ALL SELECT 'grade_rules', count(*) FROM grade_rules
UNION ALL SELECT 'grades', count(*) FROM grades
UNION ALL SELECT 'reports', count(*) FROM reports
UNION ALL SELECT 'email_notifications', count(*) FROM email_notifications
UNION ALL SELECT 'audit_logs', count(*) FROM audit_logs;