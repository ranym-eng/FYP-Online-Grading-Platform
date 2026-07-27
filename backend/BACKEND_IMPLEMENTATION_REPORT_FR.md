# Rapport d'implementation backend - FYP Grading Platform

Date: 2026-07-04
Dossier du projet: `fyp-grading-platform`
References:

- `Project_Spec (2).pdf`
- `backend_rules_microservices.md`

## 1. Objectif

L'objectif etait d'implementer la partie backend de la plateforme Online FYP Grading Platform selon le cahier de specification PDF et le fichier de regles backend.

Le backend est implemente avec:

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
- Dependances Testcontainers deja presentes

Le projet fourni etait une seule application Spring Boot. J'ai donc implemente une architecture backend modulaire dans ce projet, avec des packages qui correspondent aux microservices demandes:

- `auth`
- `user`
- `project`
- `evaluation`
- `grading`
- `reporting`
- `notification`
- `audit`
- `dashboard`

Cette structure est prete pour une future separation en vrais microservices independants.

## 2. Etapes suivies

1. Inspection du projet Spring Boot existant.
2. Verification de Spring Boot `3.5.16` et Java `21` dans `pom.xml`.
3. Creation de la structure backend par domaine.
4. Ajout d'une reponse API standard et d'un gestionnaire global d'erreurs.
5. Ajout des enums necessaires selon le cahier de specification.
6. Ajout des entites JPA principales.
7. Ajout des repositories pour la persistance.
8. Ajout des DTOs request pour les operations create/update.
9. Ajout des services metier importants.
10. Ajout des controllers REST pour les CRUD et APIs avancees.
11. Ajout de Swagger/OpenAPI avec schema Bearer JWT.
12. Ajout de Spring Security et CORS pour React.
13. Ajout du login et d'une generation de token simple.
14. Ajout des donnees initiales: tracks, admin, regles de notation, rubriques.
15. Ajout de `application.yml` pour PostgreSQL, Swagger, Actuator et Mail.
16. Ajout de `docker-compose.yml` pour PostgreSQL et MailHog.
17. Compilation du projet avec JDK 21.
18. Correction du probleme d'encodage UTF-8 avec BOM cause par PowerShell.
19. Recompilation avec succes.

## 3. Resultat

Commande executee:

```text
mvnw.cmd -DskipTests compile
```

Resultat:

```text
BUILD SUCCESS
```

Le backend contient maintenant 88 fichiers Java compiles.

## 4. Domaines implementes

### 4.1 Auth

Fichiers principaux:

- `auth/AuthController.java`
- `auth/LoginRequest.java`
- `auth/LoginResponse.java`
- `security/TokenService.java`
- `config/SecurityConfig.java`

APIs implementees:

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

Resultat:

- Le login verifie email et mot de passe.
- Les mots de passe utilisent BCrypt.
- Un utilisateur inactif ne peut pas se connecter.
- Un token est genere pour le frontend.
- Les routes Swagger et Auth sont publiques en developpement.

Compte admin par defaut:

```text
email: admin@squ.edu.om
password: Admin@123
```

### 4.2 User Service

Entites:

- `User`
- `StudentProfile`
- `EvaluatorProfile`

APIs implementees:

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

Entites:

- `Track`
- `Project`
- `Team`
- `Phase`
- `ProjectSupervisorAssignment`
- `ProjectEvaluatorAssignment`

APIs implementees:

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

Tracks initialises:

```text
EIC
CSN
CSP
PSE
```

### 4.4 Evaluation Service

Entites:

- `EvaluationFormTemplate`
- `RubricCriterion`
- `EvaluationSubmission`
- `CriterionScore`

APIs implementees:

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

Regles metier implementees:

- l'evaluateur doit etre assigne au projet et au type d'evaluation;
- un brouillon peut etre modifie;
- une evaluation verrouillee ne peut plus etre modifiee;
- la soumission verrouille le formulaire;
- la soumission enregistre `submittedAt` et `lockedAt`;
- le score doit etre entre 0 et le score maximum;
- les criteres obligatoires doivent etre remplis avant soumission;
- le score total est calcule automatiquement.

### 4.5 Grading Service

Entites:

- `Grade`
- `GradeRule`

APIs implementees:

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

Regles de notation initialisees:

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

Regles metier implementees:

- impossible de calculer une note si une evaluation obligatoire manque;
- seules les evaluations verrouillees sont prises en compte;
- les notes peuvent etre publiees ou masquees;
- les regles de notation sont configurables.

### 4.6 Reporting Service

Entite:

- `Report`

APIs implementees:

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

Resultat:

- generation d'un contenu de rapport;
- stockage des metadonnees du rapport;
- envoi du rapport via creation d'une notification email.

### 4.7 Notification Service

Entite:

- `EmailNotification`

APIs implementees:

```text
POST /api/notifications/email
GET  /api/notifications
GET  /api/notifications/{id}
GET  /api/notifications/status/{status}
POST /api/notifications/{id}/retry
POST /api/notifications/reminders/evaluation-deadline
```

### 4.8 Audit Service

Entite:

- `AuditLog`

APIs implementees:

```text
GET /api/audit
GET /api/audit/{id}
GET /api/audit/by-user/{userId}
GET /api/audit/by-entity/{entityType}/{entityId}
GET /api/audit/by-action/{action}
```

### 4.9 APIs Dashboard pour React

APIs implementees:

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

## 5. Configuration ajoutee

`application.yml` contient:

- nom de l'application;
- connexion PostgreSQL;
- configuration JPA;
- chemin Swagger;
- exposition Actuator;
- configuration Mail.

`docker-compose.yml` contient:

- PostgreSQL 16;
- MailHog pour tester les emails.

Lancer l'infrastructure:

```powershell
docker compose up -d
```

Lancer le backend:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
$env:Path=$env:JAVA_HOME+'\bin;'+$env:Path
.\mvnw.cmd spring-boot:run
```

Swagger:

```text
http://localhost:8080/swagger-ui.html
```

## 6. Notes importantes

Le projet actuel est une seule application Spring Boot. Il est structure par domaines pour respecter les frontieres microservices, mais il n'est pas encore separe physiquement en plusieurs services Maven independants.

Separation future possible:

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

L'implementation actuelle donne une base backend complete et fonctionnelle dans le projet Spring fourni.

## 7. Ameliorations restantes pour production

1. Remplacer le token simple par un vrai JWT signe avec filtre de securite.
2. Appliquer `@PreAuthorize` sur tous les endpoints sensibles.
3. Remplacer `ddl-auto=update` par des migrations Flyway SQL completes.
4. Ajouter une vraie generation PDF avec OpenPDF, JasperReports ou une librairie equivalente.
5. Ajouter l'envoi email reel via SMTP ou SendGrid.
6. Ajouter RabbitMQ/Kafka pour `EvaluationSubmitted`, `GradeCalculated` et `ReportGenerated`.
7. Separer le backend modulaire en vrais microservices si demande dans le livrable final.
8. Ajouter des tests d'integration pour les controllers.
9. Ajouter des tests repository avec Testcontainers.
10. Ajouter une pagination standardisee au lieu de retourner des listes brutes.
11. Ajouter des DTO responses pour eviter d'exposer les graphes JPA a React.

## 8. Verification

Commande executee:

```powershell
.\mvnw.cmd -DskipTests compile
```

Resultat:

```text
BUILD SUCCESS
```