# Bookify backend

Spring Boot API for Bookify.

## Requirements

- Java 17 or newer.
- Docker Desktop running when executing the complete test suite.
- PostgreSQL 16 for local development (the root Compose file uses PostGIS).

## Local setup

From the repository root:

1. Copy `.env.example` to `.env` and replace every placeholder.
2. Load the `BOOKIFY_*` variables into your shell or IDE run configuration.
3. Start PostgreSQL with `docker compose up -d database`.
4. Run the API with `./mvnw spring-boot:run`.

Spring Boot intentionally has no committed database password or JWT fallback. Startup fails when required variables are absent.
The Compose environment exposes Bookify PostgreSQL on host port `5433` by default to avoid conflicting with a native PostgreSQL installation on `5432`.

## Database changes

Flyway owns the schema under `src/main/resources/db/migration`. Hibernate uses `ddl-auto: validate`; entities cannot silently mutate a shared database.

The first migration captures the existing prototype schema. New multi-category entities will be introduced through later reviewed migrations after the ER model is approved.

Business authorization is membership-based. Tenant-scoped service endpoints use:

```text
/api/businesses/{businessId}/services
```

The backend validates the authenticated user's active membership instead of trusting a business ID from a JWT.

## Tests

Run:

```bash
./mvnw verify
```

Most integration tests use an in-memory PostgreSQL-compatible H2 profile for fast feedback.
The infrastructure suite starts an isolated `postgis/postgis:16-3.4` container with
Testcontainers, applies every Flyway migration from an empty schema and verifies PostGIS,
GiST indexes, exclusion constraints and the native nearby-discovery query. The container and
its data are removed automatically after the test JVM exits.

The NVIDIA live relevance benchmark is opt-in and skipped by normal CI. See
`docs/semantic-search.md` for the required runtime-only key and benchmark command.

## Development-only endpoints

Controllers under `/api/test` load only with the `dev` Spring profile. They must not be enabled in production.
