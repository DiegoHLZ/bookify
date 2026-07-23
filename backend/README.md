# Bookify backend

Spring Boot API for Bookify.

## Requirements

- Java 17 or newer.
- PostgreSQL 16 for local development (the root Compose file uses PostGIS).

## Local setup

From the repository root:

1. Copy `.env.example` to `.env` and replace every placeholder.
2. Load the `BOOKIFY_*` variables into your shell or IDE run configuration.
3. Start PostgreSQL with `docker compose up -d database`.
4. Run the API with `./mvnw spring-boot:run`.

Spring Boot intentionally has no committed database password or JWT fallback. Startup fails when required variables are absent.

## Database changes

Flyway owns the schema under `src/main/resources/db/migration`. Hibernate uses `ddl-auto: validate`; entities cannot silently mutate a shared database.

The first migration captures the existing prototype schema. New multi-category entities will be introduced through later reviewed migrations after the ER model is approved.

## Tests

Run:

```bash
./mvnw test
```

Tests use an in-memory PostgreSQL-compatible H2 profile for fast context validation. PostgreSQL/PostGIS integration and concurrency tests will be added with the first transactional booking slice.

## Development-only endpoints

Controllers under `/api/test` load only with the `dev` Spring profile. They must not be enabled in production.
