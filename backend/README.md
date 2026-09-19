# Backend

Spring Boot 3.5 backend using Java 21, Maven, PostgreSQL, Spring Security,
Spring Data JPA, and springdoc-openapi.

The student import module accepts the official SQU columns `stdID`, `cohort`,
`name`, and `Email`. Use `POST /api/import/students/preview` to validate a
multipart file, then `POST /api/import/students` to atomically create or update
records. The detailed administration workflow and Industry Guest grading rules
are documented in the
[French integration guide](../docs/IMPORT_ETUDIANTS_ET_EVALUATION_INDUSTRIE_FR.md).

Account access supports two controlled local-password workflows:

- `POST /api/auth/signup/request-code` and `POST /api/auth/signup/complete`
  activate only e-mail addresses pre-registered by an administrator;
- an administrator can create an immediately active account or call
  `POST /api/users/{id}/temporary-password`; the generated password is delivered
  by e-mail and must be replaced before a session token is issued.

Passwords are never accepted from the initialization workbook or returned by
the user-management API. SQU SSO can replace local passwords for internal users.

The initialization workbook keeps one sheet per actor type. When one e-mail
appears in several different role sheets, the importer creates one account with
multiple assigned roles. After authentication, the user selects the active
workspace; a signed token and backend authorization enforce that selected role.
Repeating an e-mail within the same role sheet remains a validation error.

See the [main README](../README.md) for Docker startup, local development,
Swagger, and test commands.
