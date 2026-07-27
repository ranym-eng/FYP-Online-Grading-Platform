# Backend

Spring Boot 3.5 backend using Java 21, Maven, PostgreSQL, Spring Security,
Spring Data JPA, and springdoc-openapi.

The student import module accepts the official SQU columns `stdID`, `cohort`,
`name`, and `Email`. Use `POST /api/import/students/preview` to validate a
multipart file, then `POST /api/import/students` to atomically create or update
records. The detailed administration workflow and Industry Guest grading rules
are documented in the
[French integration guide](../docs/IMPORT_ETUDIANTS_ET_EVALUATION_INDUSTRIE_FR.md).

See the [main README](../README.md) for Docker startup, local development,
Swagger, and test commands.