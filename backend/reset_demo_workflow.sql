\set ON_ERROR_STOP on

-- Resets only workflow results created for DEMO-* projects.
-- Run seed_all_tables.sql again immediately after this file.
BEGIN;

DELETE FROM criterion_scores
WHERE submission_id IN (
  SELECT es.id
  FROM evaluation_submissions es
  JOIN projects p ON p.id = es.project_id
  WHERE p.project_number LIKE 'DEMO-%'
);

DELETE FROM grades
WHERE project_id IN (SELECT id FROM projects WHERE project_number LIKE 'DEMO-%');

DELETE FROM reports
WHERE project_id IN (SELECT id FROM projects WHERE project_number LIKE 'DEMO-%');

DELETE FROM evaluation_submissions
WHERE project_id IN (SELECT id FROM projects WHERE project_number LIKE 'DEMO-%');

DELETE FROM phase_extension_requests
WHERE phase_id::text LIKE '70000000-%'
  AND requester_id IN (SELECT id FROM app_users WHERE email LIKE 'demo.%@squ.edu.om');

DELETE FROM email_notifications
WHERE recipient_user_id IN (SELECT id FROM app_users WHERE email LIKE 'demo.%@squ.edu.om')
   OR recipient IN ('fyp-coordinator@university.edu', 'demo.coordinator@squ.edu.om');

COMMIT;

SELECT 'Demo workflow reset. Run seed_all_tables.sql now.' AS result;
