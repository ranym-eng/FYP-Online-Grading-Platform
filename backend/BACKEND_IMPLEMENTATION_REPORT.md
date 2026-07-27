# Backend Implementation Report - FYP Grading Platform

Date: 2026-07-04
Project folder: `fyp-grading-platform`
References:

- `Project_Spec (2).pdf`
- `backend_rules_microservices.md`

## 1. Goal

The goal was to implement the backend part of the Online FYP Grading Platform according to the specification PDF and the backend rules file.

The backend is implemented with:

- Java 21 LTS
- Spring Boot 3.5.16
- Maven
- PostgreSQL
- Spring Web
- Spring Data JPA
- Spring Security
- Spring Validation
- Spring Mail
- Springdoc Swagger/OpenAPI
- Actuator
- Testcontainers dependencies already present

The provided project was a single Spring Boot application. I implemented a modular backend inside this project using domain packages that map directly to the requested microservices:

- `auth`
- `user`
- `project`
- `evaluation`
- `grading`
- `reporting`
- `notification`
- `audit`
- `dashboard`

This gives a microservices-ready backend structure. These modules can later be split into independent Spring Boot services if required.

## 2. Steps Followed

1. Inspected the existing Spring Boot project.
2. Verified the project uses Spring Boot `3.5.16` and Java `21` in `pom.xml`.
3. Created backend package structure by domain.
4. Added common API response and global exception handling.
5. Added enums required by the specification.
6. Added JPA entities for all core domains.
7. Added repositories for persistence.
8. Added DTO request records for create/update operations.
9. Added services for important business workflows.
10. Added REST controllers for all CRUD and advanced APIs.
11. Added Swagger/OpenAPI configuration with Bearer JWT scheme.
12. Added Spring Security configuration and CORS for React.
13. Added login endpoint and token generation placeholder.
14. Added seed data for tracks, admin user, grade rules, and default rubrics.
15. Added `application.yml` for PostgreSQL, Swagger, Actuator and Mail.
16. Added `docker-compose.yml` for PostgreSQL and MailHog.
17. Compiled the project with JDK 21.
18. Fixed UTF-8 BOM encoding issue caused by PowerShell file generation.
19. Recompiled successfully.

## 3. Result

Compilation result:

```text
mvnw.cmd -DskipTests compile
BUILD SUCCESS
```

The backend now contains 88 compiled Java source files.

## 4. Implemented Domains

### 4.1 Auth

Files:

- `auth/AuthController.java`
- `auth/LoginRequest.java`
- `auth/LoginResponse.java`
- `security/TokenService.java`
- `config/SecurityConfig.java`

Implemented APIs:

```text
POST /api/auth/login
POST /api/auth/logout
POST /api/auth/refresh-token
GET  /api/auth/me
GET  /api/auth/validate-token
POST /api/auth/change-password
POST /api/auth/forgot-password
POST /api/auth/reset-password
```

Result:

- Login checks email/password.
- Passwords use BCrypt.
- Inactive users cannot log in.
- Token is generated for the frontend.
- Swagger and auth routes are public in development.

Default admin account:

```text
email: admin@squ.edu.om
password: Admin@123
```

### 4.2 User Service

Entities:

- `User`
- `StudentProfile`
- `EvaluatorProfile`

Implemented APIs:

```text
POST   /api/users
GET    /api/users
GET    /api/users/{id}
PUT    /api/users/{id}
PATCH  /api/users/{id}/activate
PATCH  /api/users/{id}/deactivate
DELETE /api/users/{id}
GET    /api/users/by-role/{role}
GET    /api/users/search?keyword=

POST   /api/students
GET    /api/students
GET    /api/students/{id}
PUT    /api/students/{id}
DELETE /api/students/{id}
GET    /api/students/by-track/{trackCode}

POST   /api/evaluators
GET    /api/evaluators
GET    /api/evaluators/{id}
PUT    /api/evaluators/{id}
DELETE /api/evaluators/{id}
GET    /api/evaluators/internal
GET    /api/evaluators/external
GET    /api/evaluators/available
```

### 4.3 Project Service

Entities:

- `Track`
- `Project`
- `Team`
- `Phase`
- `ProjectSupervisorAssignment`
- `ProjectEvaluatorAssignment`

Implemented APIs:

```text
POST   /api/tracks
GET    /api/tracks
GET    /api/tracks/{id}
GET    /api/tracks/code/{code}
PUT    /api/tracks/{id}
DELETE /api/tracks/{id}
PATCH  /api/tracks/{id}/activate
PATCH  /api/tracks/{id}/deactivate

POST   /api/projects
GET    /api/projects
GET    /api/projects/{id}
PUT    /api/projects/{id}
DELETE /api/projects/{id}
PATCH  /api/projects/{id}/status
GET    /api/projects/by-track/{trackId}
GET    /api/projects/by-academic-year/{year}
GET    /api/projects/search?keyword=

POST   /api/projects/{projectId}/supervisor/{supervisorId}
GET    /api/projects/{projectId}/supervisor
DELETE /api/projects/{projectId}/supervisor
POST   /api/projects/{projectId}/evaluators
GET    /api/projects/{projectId}/evaluators
DELETE /api/projects/{projectId}/evaluators/{assignmentId}

POST   /api/teams
GET    /api/teams
GET    /api/teams/{id}
PUT    /api/teams/{id}
DELETE /api/teams/{id}
GET    /api/teams/by-project/{projectId}
GET    /api/teams/by-academic-year/{year}
POST   /api/teams/{teamId}/students/{studentId}
DELETE /api/teams/{teamId}/students/{studentId}
GET    /api/teams/{teamId}/students

POST   /api/phases
GET    /api/phases
GET    /api/phases/{id}
PUT    /api/phases/{id}
DELETE /api/phases/{id}
PATCH  /api/phases/{id}/open
PATCH  /api/phases/{id}/close
PATCH  /api/phases/{id}/archive
GET    /api/phases/current
GET    /api/phases/by-academic-year/{year}
GET    /api/phases/status/{status}
```

Seeded tracks:

```text
EIC
CSN
CSP
PSE
```

### 4.4 Evaluation Service

Entities:

- `EvaluationFormTemplate`
- `RubricCriterion`
- `EvaluationSubmission`
- `CriterionScore`

Implemented APIs:

```text
POST   /api/evaluation-forms
GET    /api/evaluation-forms
GET    /api/evaluation-forms/{id}
PUT    /api/evaluation-forms/{id}
DELETE /api/evaluation-forms/{id}
GET    /api/evaluation-forms/by-type/{type}
GET    /api/evaluation-forms/by-phase/{phaseType}
PATCH  /api/evaluation-forms/{id}/activate
PATCH  /api/evaluation-forms/{id}/deactivate

POST   /api/evaluation-forms/{formId}/criteria
GET    /api/evaluation-forms/{formId}/criteria
GET    /api/criteria/{id}
PUT    /api/criteria/{id}
DELETE /api/criteria/{id}
PATCH  /api/criteria/{id}/order

POST   /api/evaluations/draft
PUT    /api/evaluations/{submissionId}/draft
POST   /api/evaluations/{submissionId}/submit
POST   /api/evaluations/{submissionId}/lock
GET    /api/evaluations/{submissionId}
GET    /api/evaluations/{submissionId}/scores
GET    /api/evaluations/by-project/{projectId}
GET    /api/evaluations/by-evaluator/{evaluatorId}
GET    /api/evaluations/by-project/{projectId}/phase/{phaseId}
GET    /api/evaluations/by-project/{projectId}/type/{type}
GET    /api/evaluations/progress/project/{projectId}
GET    /api/evaluations/status/project/{projectId}
```

Business rules implemented:

- evaluator must be assigned to the project and evaluation type;
- draft can be edited;
- locked evaluation cannot be edited;
- submit locks the form;
- submit stores `submittedAt` and `lockedAt`;
- score must be between 0 and max score;
- required criteria must be filled before submission;
- total score is calculated automatically.

### 4.5 Grading Service

Entities:

- `Grade`
- `GradeRule`

Implemented APIs:

```text
POST  /api/grades/calculate/project/{projectId}/phase/{phaseId}
POST  /api/grades/recalculate/project/{projectId}/phase/{phaseId}
GET   /api/grades/project/{projectId}
GET   /api/grades/project/{projectId}/phase/{phaseId}
PATCH /api/grades/{gradeId}/publish
PATCH /api/grades/project/{projectId}/publish
PATCH /api/grades/project/{projectId}/unpublish
GET   /api/grades/rules
PUT   /api/grades/rules/{id}?weight=
```

Default grade rules seeded:

```text
Phase I:
- Supervisor Phase I: 40
- Report Phase I: 35
- Oral Phase I: 25

Phase II:
- Supervisor Phase II: 30
- Report Phase II: 25
- Oral Phase II: 25
- Demo Day Industry: 20
```

Business rules implemented:

- grade cannot be calculated if required evaluations are missing;
- only locked evaluations are counted;
- grades can be published/unpublished;
- grade rules are configurable.

### 4.6 Reporting Service

Entities:

- `Report`

Implemented APIs:

```text
POST   /api/reports/project/{projectId}/phase/{phaseId}
POST   /api/reports/project/{projectId}/final
GET    /api/reports
GET    /api/reports/{id}
GET    /api/reports/project/{projectId}
GET    /api/reports/status/{status}
POST   /api/reports/{id}/send
POST   /api/reports/{id}/regenerate
DELETE /api/reports/{id}
```

Result:

- Generates report content snapshot.
- Stores report metadata.
- Sends report by creating an email notification record.

### 4.7 Notification Service

Entity:

- `EmailNotification`

Implemented APIs:

```text
POST /api/notifications/email
GET  /api/notifications
GET  /api/notifications/{id}
GET  /api/notifications/status/{status}
POST /api/notifications/{id}/retry
POST /api/notifications/reminders/evaluation-deadline
```

### 4.8 Audit Service

Entity:

- `AuditLog`

Implemented APIs:

```text
GET /api/audit
GET /api/audit/{id}
GET /api/audit/by-user/{userId}
GET /api/audit/by-entity/{entityType}/{entityId}
GET /api/audit/by-action/{action}
```

### 4.9 Dashboard APIs for React

Implemented APIs:

```text
GET /api/dashboard/admin/summary
GET /api/dashboard/admin/tracks-status
GET /api/dashboard/admin/phases-status
GET /api/dashboard/admin/evaluation-completion
GET /api/dashboard/admin/pending-evaluations

GET /api/dashboard/student/me/project
GET /api/dashboard/student/me/team
GET /api/dashboard/student/me/progress
GET /api/dashboard/student/me/grades

GET /api/dashboard/evaluator/me/projects
GET /api/dashboard/evaluator/me/pending-evaluations
GET /api/dashboard/evaluator/me/submitted-evaluations
GET /api/dashboard/evaluator/me/deadlines
```

## 5. Configuration Added

`application.yml` includes:

- app name;
- PostgreSQL datasource;
- JPA config;
- Swagger path;
- Actuator exposure;
- Mail host config.

`docker-compose.yml` includes:

- PostgreSQL 16;
- MailHog for email testing.

Run infrastructure:

```powershell
docker compose up -d
```

Run backend:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
$env:Path=$env:JAVA_HOME+'\bin;'+$env:Path
.\mvnw.cmd spring-boot:run
```

Swagger:

```text
http://localhost:8080/swagger-ui.html
```

## 6. Important Notes

The current project is one Spring Boot application. It is structured by domains to match microservices boundaries, but it is not physically split into separate Maven modules/services yet.

A future split can be done like this:

```text
auth-service
user-service
project-service
evaluation-service
grading-service
reporting-service
notification-service
audit-service
api-gateway-service
discovery-service
config-service
```

The current implementation focuses on giving a complete functional backend foundation inside the provided Spring project.

## 7. Remaining Improvements For Production

These are recommended next steps:

1. Replace placeholder token with real signed JWT filter.
2. Enforce method-level RBAC using `@PreAuthorize` on every endpoint.
3. Replace Hibernate `ddl-auto=update` with Flyway SQL migrations.
4. Add real PDF generation using OpenPDF, JasperReports, or iText-compatible library.
5. Add real email sending via SMTP or SendGrid.
6. Add RabbitMQ/Kafka events for `EvaluationSubmitted`, `GradeCalculated`, and `ReportGenerated`.
7. Split the modular monolith into real microservices if required by the final architecture deliverable.
8. Add integration tests for all controllers.
9. Add Testcontainers-based repository tests.
10. Add pagination response DTOs instead of raw lists.
11. Add DTO responses to avoid exposing JPA entity graphs to React.

## 8. Verification

Executed:

```powershell
.\mvnw.cmd -DskipTests compile
```

Result:

```text
BUILD SUCCESS
```
