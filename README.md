# FYP Online Grading Platform

A platform for managing and grading Final Year Projects (FYP) at Sultan Qaboos University. It replaces the former workflow based on separate Excel evaluation sheets and MATLAB consolidation with one secure, traceable, role-based application.

The platform covers the complete FYP I and FYP II process: annual data initialization, project and team management, evaluator assignments, deadline-controlled assessments, grade calculation, publication, Excel reporting, e-mail delivery, notifications, and audit history.

## Live Demo

- [Watch the recorded platform demonstration](https://drive.google.com/file/d/1EmcknRu2H6jg8D8RO5O9LL1t7mJ2P7XL/view?usp=sharing)
- [Download the fictional demonstration workbook](outputs/delivery-finalization/FYP_FULL_DEMO_DATA.xlsx)
- [Download the clean initialization template](outputs/template-refresh/FYP_PLATFORM_INITIALIZATION_TEMPLATE.xlsx)

## What the Platform Replaces

Before this platform, each academic evaluator completed a multi-sheet Excel workbook. The university then used a MATLAB program to read those files, average the submitted marks, apply the official assessment weights, and generate `Final_Evaluation_Summary.xlsx`.

The application now performs the same academic workflow centrally:

1. The administrator imports the official academic data once.
2. Each evaluator receives only the projects and forms assigned to that account.
3. Scores are saved as drafts while the evaluator is working.
4. A form contributes to grading only after final validation and locking.
5. The backend averages multiple locked evaluator submissions and applies the configured weights.
6. The administrator reviews, calculates, and publishes the results.
7. The platform generates downloadable Excel summaries and can send them by e-mail.

This removes manual file collection, duplicated spreadsheets, MATLAB execution, uncontrolled formula changes, and the risk of including unfinished forms.

## Actors and Access Rules

Students are academic records used for teams and individual grades. They are not platform users and do not have accounts or dashboards.

| Actor | Main responsibilities and restrictions |
| --- | --- |
| Administrator | Initializes annual data, manages all academic records, assignments, phases, deadlines, forms, criteria, grading rules, extensions, grades, reports, notifications, and audit logs. |
| Supervisor | Sees only supervised projects; completes the individual Supervisor FYP I and FYP II forms; saves drafts; validates final forms; requests an extension after an expired deadline. |
| Report Evaluator | Sees only assigned projects; evaluates the written Report I and Report II using the official paper-report rubric. |
| Faculty Evaluator | Sees only assigned projects; evaluates Oral I and Oral II presentations using individual and group criteria. |
| Industry Representative | Receives an invitation-based, time-limited account; sees only assigned Demo Day projects; can complete only the Industry Demo Day form. |
| FYP Coordinator | Monitors progress and published grades and accesses authorized reports and exports without receiving administrative data-management permissions. |

There is no public sign-up page. Internal users must be provisioned from official university data before their first connection. Production authentication is designed for SQU OpenID Connect single sign-on. Local password login remains available only as a configurable demonstration fallback.

## Detailed Features

### Authentication and Account Provisioning

- Role-based authentication and automatic redirection to the correct dashboard.
- Separate session, navigation, project scope, and permissions for every actor.
- No self-registration for internal university actors.
- Configurable SQU OpenID Connect authorization-code flow.
- Imported internal e-mail addresses must match the identity returned by SQU SSO.
- One-time Industry Guest invitation links with hashed tokens and configurable expiration.
- Mandatory Industry Guest access-expiration date and Demo Day-only authorization.
- Secure logout, session validation, token refresh, and password change.
- Functional forgot-password workflow with a generic anti-enumeration response, single-use token, expiration, reset form, and e-mail delivery.
- Backend authorization prevents users from opening unassigned projects even if an API URL is entered manually.

### Annual Excel Initialization

The administrator can initialize a cohort from one master workbook instead of manually creating every record.

The workbook contains nine sheets:

| Sheet | Imported data |
| --- | --- |
| `STUDENTS` | Student ID, name, e-mail, cohort, track, and level. |
| `ADMINISTRATORS` | Official administrator identities and account status. |
| `COORDINATORS` | FYP coordinator identities. |
| `SUPERVISORS` | Supervisor identities and academic profile data. |
| `REPORT_EVALUATORS` | Written-report evaluator identities. |
| `FACULTY_EVALUATORS` | Oral-presentation evaluator identities. |
| `INDUSTRY_GUESTS` | Guest identity, organization, invitation state, and access-expiration date. |
| `PHASES` | Academic phase, cohort/year, opening date, deadline, sequence, and status. |
| `PROJECT_ASSIGNMENTS` | Projects, tracks, students, supervisors, report evaluators, oral evaluators, and Industry Guests. |

Import behavior:

- preview and validate the complete workbook without writing to PostgreSQL;
- report errors by sheet, row, and field;
- validate references between students, actors, tracks, projects, and phases;
- initialize accepted data in one transaction;
- support idempotent updates using stable student IDs, actor identifiers/e-mails, and project numbers;
- keep one to five students per project;
- keep one or two supervisors per project;
- accept multiple assigned evaluators separated by commas or semicolons;
- provide a separate student-update import for official database extracts;
- preserve students as academic records without creating student login accounts.

The clean workbook contains only a few fictional examples to explain the required structure. The full demo workbook contains fictional data covering all roles, tracks, team sizes, assignment combinations, phases, and evaluation types.

### Academic Data Management

The administrator has searchable, paginated management views and confirmed create, read, update, and delete operations for:

- users and role assignments;
- students and cohorts;
- tracks: EIC, CSN, CSP, and PSE;
- projects and project metadata;
- teams and student membership;
- supervisor assignments;
- report, oral, and Industry evaluator assignments;
- FYP phases and deadlines;
- evaluation-form templates and rubric criteria;
- grading rules;
- notifications, generated reports, and audit records.

Destructive operations use confirmation dialogs. Tables include compact actions, search, filters, responsive layouts, loading states, empty states, and error feedback.

### Phase and Deadline Management

- Create, edit, open, close, archive, filter, and inspect FYP I and FYP II phases.
- Configure the phase name, type, cohort/academic year, sequence, start date, deadline, and status.
- Edit deadlines from the administrator calendar.
- Reject final evaluation submission when the phase is closed or its deadline has expired.
- Keep unfinished drafts excluded after expiration.
- Generate 24-hour and 12-hour deadline reminders.
- Allow an evaluator to request an extension by selecting the phase and writing a reason.
- Prevent non-administrators from choosing their own extension date.
- Allow only the administrator to approve or reject a request and set the new personal deadline.
- Notify the requester in the application and by e-mail after the decision.

### Evaluation Forms

The score-entry interface deliberately follows the familiar structure of the former Excel sheets while adding validation, draft handling, status information, and secure submission.

Supported assessment types:

- Supervisor FYP I;
- Report FYP I;
- Oral Presentation FYP I;
- Supervisor FYP II;
- Report FYP II;
- Oral Presentation FYP II;
- Industry Demo Day.

Evaluation behavior:

- evaluators see only projects assigned to them;
- each actor sees only the assessment types authorized for that role;
- Industry Guests cannot select or modify FYP I, supervisor, report, or oral forms;
- score cells are validated against the criterion maximum;
- required criteria must be complete before final validation;
- comments and overall remarks can be saved with the form;
- changes are created or updated as a draft;
- drafts never contribute to official results;
- final validation requires confirmation;
- validated submissions are locked against later editing;
- only locked submissions are eligible for consolidation.

### Official Scoring Logic

- Supervisor assessments are calculated per student.
- Report assessments are calculated at project level using ten criteria scored out of 10. Criterion 4 has double weight, so the normalized report score is `(C1 + C2 + C3 + 2*C4 + C5 + ... + C10) / 11`.
- Oral presentation assessments combine the individual and group cells defined by the active rubric.
- Industry Demo Day uses the official five-criterion weighting `2 / 1 / 4 / 2 / 1`.
- When several evaluators submit the same assessment type, their locked results are averaged.
- Valid zero marks are retained; missing values are not silently treated as zero.
- The phase calculation applies administrator-configured percentages for every evaluation type.
- The default FYP I weights are Supervisor 40%, Report 35%, and Oral 25%.
- The default FYP II weights are Supervisor 30%, Report 25%, Oral 25%, and Industry Demo Day 20%.
- Recalculation is available before publication when an authorized locked input changes.
- Published student grades remain available for authorized reporting and traceability.

### Grade Consolidation and MATLAB Replacement

The Spring Boot grading engine replaces the former MATLAB aggregation process. It reads normalized scores from PostgreSQL instead of evaluator workbooks, checks that submissions are locked, groups marks by project, student, phase, and evaluator type, averages repeated evaluator contributions, applies the configured rules, and stores the calculated grade.

Administrators can:

- inspect completion and missing-form status;
- calculate or recalculate a project phase;
- compare project and student-level results;
- publish approved grades;
- verify that drafts and expired unvalidated forms are excluded;
- export phase, project, and final summaries.

### Reports and Excel Exports

- Generate project-phase reports and final project reports.
- Check phase completeness before official reporting.
- Archive generated reports with status and generation history.
- Download, regenerate, e-mail, or delete a generated report through confirmed actions.
- Persist generated workbooks in a dedicated Docker volume.
- Export a MATLAB-compatible final summary plus enhanced traceability sheets.

The final workbook can contain:

- `LEGACY_SUMMARY` for compatibility with the former process;
- `FINAL_SUMMARY` for consolidated student results;
- `EVALUATOR_DETAILS` for locked evaluator contributions;
- `MISSING_FORMS` for incomplete assessment tracking;
- `AUDIT_TRAIL` for sensitive workflow events.

### Notifications, E-mail, and Audit

- Notification bell available in every actor dashboard.
- Compact notification popover with unread count, summary, timestamps, and read status.
- Contextual navigation from a notification to the relevant evaluation, phase, extension, or report.
- In-app and SMTP notifications for Industry invitations, password resets, approaching deadlines, extension decisions, and generated reports.
- Retry support for failed notification delivery.
- Audit history for sensitive administrative and grading actions.

Mailpit is included only for local development. It is an SMTP-compatible inbox that captures messages without sending them to real users. Production uses the same Spring Mail code with the university SMTP server or SendGrid SMTP by changing environment variables; no backend code change is required.

### User Interface

- English-only interface across login, dashboards, forms, tables, messages, and notifications.
- Dedicated dashboard and actions for every actor.
- Responsive desktop, tablet, and mobile navigation.
- Light theme by default in the authenticated workspace and a dark institutional login experience.
- Global navigation search and contextual shortcuts.
- Accessible dialogs, confirmation popups, form validation, skeleton loaders, loading indicators, empty states, error states, and success feedback.
- SQU visual identity and university favicon.

## End-to-End Academic Workflow

1. Start the platform and sign in with the provisioned administrator account.
2. Download or prepare the annual initialization workbook.
3. Upload it to **Excel Imports** and run **Analyze without saving**.
4. Correct every validation error, then run **Initialize platform**.
5. Review users, students, projects, teams, assignments, phases, deadlines, forms, and grading rules.
6. Send Industry Guest invitations and verify e-mail delivery.
7. Open the required academic phases.
8. Supervisors, Report Evaluators, Faculty Evaluators, and Industry Guests sign in and see only their assigned work.
9. Evaluators enter marks. Their work remains a draft until they choose **Validate form**.
10. The platform locks validated forms and sends deadline reminders for pending work.
11. An evaluator blocked by an expired deadline can request an extension; the administrator decides and sets the personal deadline.
12. The administrator verifies completeness and calculates FYP I or FYP II results.
13. The administrator reviews and publishes the grades.
14. The administrator or coordinator generates, downloads, archives, and sends the official Excel reports.
15. Audit and notification history provide traceability for the delivered process.

## Technology Stack

| Layer | Technology |
| --- | --- |
| Frontend | React 19, Vite 8, JavaScript, CSS, Lucide icons |
| Frontend server | Nginx with SPA routing and `/api` reverse proxy |
| Backend | Spring Boot 3.5.16, Java 21 LTS, Maven |
| Security | Spring Security, BCrypt, token sessions, OAuth2/OIDC client |
| Persistence | Spring Data JPA, Hibernate, Flyway, PostgreSQL 18 |
| Excel processing | Apache POI |
| API documentation | Springdoc OpenAPI and Swagger UI |
| E-mail | Spring Mail over SMTP; Mailpit locally, SQU SMTP or SendGrid in production |
| Deployment | Docker and Docker Compose |
| Testing | JUnit, Mockito, Spring Security Test, Testcontainers, ESLint, grading verification scripts |

## Architecture

This repository is a monorepo. The backend is a modular Spring Boot application organized by business domain, and Docker Compose runs four cooperating services:

```text
Browser
   |
   v
React application served by Nginx
   |
   | /api reverse proxy
   v
Spring Boot REST API --------> SMTP / Mailpit
   |
   v
PostgreSQL
```

The Docker services are:

- `frontend`: React production build served by Nginx;
- `backend`: Spring Boot REST API and grading engine;
- `postgres`: persistent academic and workflow data;
- `mailpit`: local SMTP capture and e-mail inspection.

The backend modules cover authentication, users, imports, projects, teams, phases, evaluations, grading, notifications, reporting, dashboards, and auditing.

## Repository Structure

```text
.
|-- backend/                 Spring Boot application and tests
|-- frontend/                React application, assets, and public templates
|-- docs/                    UML, demo, import, and implementation documents
|-- outputs/                 Demonstration workbooks and generated media
|-- scripts/                 Verification and media-generation scripts
|-- compose.yaml             Complete local Docker environment
|-- .env.example             Development and production variable examples
`-- README.md                Final project documentation
```

## Quick Start with Docker

### Prerequisites

- Git
- Docker Desktop with the Docker engine running
- Docker Compose, included with Docker Desktop

Java, Node.js, Maven, and PostgreSQL do not need to be installed on the host when Docker is used.

### 1. Clone and enter the repository

```powershell
git clone https://github.com/ranym-eng/FYP-Online-Grading-Platform.git
Set-Location "FYP-Online-Grading-Platform"
```

When the repository already exists at the project path:

```powershell
Set-Location "D:\Desktop\sultan qaboos\FYP-Online-Grading-Platform"
```

In PowerShell, do not use `cd /d`; that syntax belongs to Command Prompt.

### 2. Start all services

```powershell
docker compose up --build -d
```

The first build can take several minutes because Docker downloads the Java, Node.js, Nginx, PostgreSQL, and Mailpit images.

### 3. Verify service health

```powershell
docker compose ps
```

The frontend, backend, PostgreSQL, and Mailpit containers should be running. PostgreSQL and the backend should report `healthy` after startup.

### 4. Open the application

| Service | URL |
| --- | --- |
| Platform | http://localhost:3000 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Backend health | http://localhost:8080/actuator/health |
| Mailpit inbox | http://localhost:8025 |
| PostgreSQL from the host | `localhost:5433` |

### 5. Sign in for a local demonstration

```text
E-mail:   admin@squ.edu.om
Password: Admin@123
```

The administrator and local password login are demonstration defaults. They must not be used unchanged in a shared or production deployment.

### 6. Import demonstration data

1. Open **Excel Imports** from the administrator workspace.
2. Select **Full demo dataset** or upload [`FYP_FULL_DEMO_DATA.xlsx`](outputs/delivery-finalization/FYP_FULL_DEMO_DATA.xlsx).
3. Choose **Analyze without saving**.
4. Review the sheet and row validation result.
5. Choose **Initialize platform** only when the preview contains no blocking errors.
6. Review the imported users, students, projects, teams, phases, and assignments.
7. Open Mailpit to inspect generated Industry Guest invitations.

The demonstration data is fictional and intended only for testing and presentations.

## Useful Docker Commands

```powershell
# Show container status
docker compose ps

# Follow all logs
docker compose logs -f

# Follow backend logs only
docker compose logs -f backend

# Rebuild after source-code changes
docker compose up --build -d

# Stop containers and keep database/report data
docker compose down

# Stop containers and delete all Docker database/report volumes
docker compose down -v
```

`docker compose down -v` permanently removes the Docker-managed PostgreSQL and report volumes. Use it only when a completely empty local environment is required.

## Port Configuration

Copy the environment template before changing ports or credentials:

```powershell
Copy-Item .env.example .env
```

Important defaults:

| Variable | Default |
| --- | --- |
| `FRONTEND_PORT` | `3000` |
| `BACKEND_PORT` | `8080` |
| `POSTGRES_PORT` | `5433` |
| `MAILPIT_UI_PORT` | `8025` |
| `MAILPIT_SMTP_PORT` | `1025` |
| `POSTGRES_DB` | `fyp_grading_platform` |
| `POSTGRES_USER` | `postgres` |
| `POSTGRES_PASSWORD` | `root` |

If port 3000 is already used, edit `.env`:

```text
FRONTEND_PORT=3010
APP_FRONTEND_URL=http://localhost:3010
```

Then recreate the containers:

```powershell
docker compose up --build -d
```

Open `http://localhost:3010` instead of port 3000.

PostgreSQL uses host port 5433 by default to avoid conflict with a locally installed PostgreSQL server on port 5432. Inside Docker, the backend still connects to the `postgres` service on port 5432.

## E-mail Configuration

### Local demonstration

Docker configures Spring Mail to send messages to Mailpit:

```text
MAIL_HOST=mailpit
MAIL_PORT=1025
```

Open http://localhost:8025 to test Industry invitations, password reset links, deadline reminders, extension decisions, and report attachments.

### Production SMTP or SendGrid

Replace the local values in the deployment environment:

```text
MAIL_HOST=smtp.example.edu
MAIL_PORT=587
MAIL_USERNAME=<smtp-user>
MAIL_PASSWORD=<smtp-password>
MAIL_FROM=no-reply@squ.edu.om
MAIL_SMTP_AUTH=true
MAIL_STARTTLS_ENABLE=true
MAIL_STARTTLS_REQUIRED=true
```

For SendGrid SMTP, use `smtp.sendgrid.net`, port `587`, username `apikey`, and the SendGrid API key as the password.

## SQU Single Sign-On Configuration

Obtain the OIDC values from the SQU identity-management team, then configure:

```text
LOCAL_INTERNAL_LOGIN_ENABLED=false
SQU_SSO_ENABLED=true
SQU_SSO_CLIENT_ID=<client-id>
SQU_SSO_CLIENT_SECRET=<client-secret>
SQU_SSO_ISSUER_URI=<issuer-uri>
SQU_SSO_SCOPES=openid,profile,email
SQU_SSO_EMAIL_CLAIM=email
SQU_SSO_ALLOWED_DOMAIN=squ.edu.om
APP_FRONTEND_URL=https://<public-platform-domain>
```

Register the production HTTPS callback URL with the identity provider. An internal user must already exist in the platform with the same institutional e-mail and an active role.

## Running without Docker

### Backend

Requirements: Java 21, PostgreSQL, and a database named `fyp_grading_platform`.

```powershell
Set-Location backend
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/fyp_grading_platform"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="root"
$env:LOCAL_INTERNAL_LOGIN_ENABLED="true"
.\mvnw.cmd spring-boot:run
```

### Frontend

Requirement: Node.js 22 or later.

```powershell
Set-Location frontend
npm ci
npm run dev
```

Vite serves the development interface and proxies `/api` requests to `http://localhost:8080`.

## API and Swagger Testing

1. Start the backend or the complete Docker environment.
2. Open http://localhost:8080/swagger-ui.html.
3. Authenticate through `POST /api/auth/login` in local demonstration mode.
4. Use the returned token with the Swagger **Authorize** action.
5. Test only endpoints permitted for that account role.

Important API groups include:

- `/api/auth`
- `/api/import/initialization`
- `/api/users`, `/api/students`, and `/api/evaluators`
- `/api/tracks`, `/api/projects`, `/api/teams`, and `/api/phases`
- `/api/evaluation-forms`, `/api/criteria`, and `/api/evaluations`
- `/api/phase-extension-requests`
- `/api/grades`
- `/api/reports`
- `/api/notifications`
- `/api/dashboard`
- `/api/audit`

## Tests and Verification

Backend tests:

```powershell
Set-Location backend
.\mvnw.cmd test
```

Frontend checks:

```powershell
Set-Location frontend
npm ci
npm run test
npm run lint
npm run build
```

Docker configuration validation:

```powershell
docker compose config
```

Testcontainers-based backend integration tests require Docker Desktop to be running.

## UML Documentation

### Global Use Case Diagram

<p align="center">
  <a href="docs/uml/use-case-global.svg">
    <img src="./docs/uml/readme-use-case-diagram.png" alt="Global Use Case Diagram" width="100%">
  </a>
</p>

### Global Class Diagram

<p align="center">
  <a href="docs/uml/class-diagram-global.svg">
    <img src="./docs/uml/readme-class-diagram.png" alt="Global Class Diagram" width="100%">
  </a>
</p>

Additional UML artifacts:

- [Use-case diagram SVG](docs/uml/use-case-global.svg)
- [Class diagram SVG](docs/uml/class-diagram-global.svg)
- [Combined UML PDF](docs/uml/fyp-uml-diagrams.pdf)
- Editable Graphviz and PlantUML sources in [`docs/uml`](docs/uml)

## Troubleshooting

### Docker cannot connect to its engine

Start Docker Desktop and wait until the Linux container engine reports that it is running. Then repeat `docker compose up --build -d`.

### Docker cannot find the Compose file

Run the command from the repository root, where `compose.yaml` is located:

```powershell
Set-Location "D:\Desktop\sultan qaboos\FYP-Online-Grading-Platform"
docker compose config
```

### A port is already allocated

Change the relevant value in `.env`, then run:

```powershell
docker compose up --build -d
```

To inspect a Windows port:

```powershell
Get-NetTCPConnection -LocalPort 3000 -ErrorAction SilentlyContinue
```

### Backend or database is unhealthy

```powershell
docker compose ps
docker compose logs backend
docker compose logs postgres
```

### Start again with an empty Docker database

```powershell
docker compose down -v
docker compose up --build -d
```

## Security and Production Checklist

Before deployment outside a local demonstration environment:

- replace all default database and administrator credentials;
- set a private `APP_TOKEN_SECRET` of at least 32 random characters;
- disable local internal login and enable reviewed SQU SSO settings;
- use HTTPS for the platform, OAuth callback, invitations, and password-reset links;
- configure the university SMTP service or SendGrid with secrets outside Git;
- restrict PostgreSQL and management endpoints from public access;
- back up the PostgreSQL and generated-report volumes;
- review role assignments, grading rules, phase deadlines, and evaluator allocations;
- remove demonstration records before importing official university data;
- never commit `.env`, SMTP passwords, API keys, or production database credentials.

## License and Institutional Use

This repository is an academic project for the Sultan Qaboos University FYP grading workflow. Deployment, branding, identity integration, and production use must be reviewed and approved by the university.
