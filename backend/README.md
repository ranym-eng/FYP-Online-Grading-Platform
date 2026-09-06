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

See the [main README](../README.md) for Docker startup, local development,
Swagger, and test commands.
