# Current State Assessment

Assessment date: 2026-07-23.

## Summary

The Angular/Spring Boot repository is an early generic service-booking technical spike. Its `Business` and `ServiceOffering` concepts align partially with the product, but locations, resources, robust availability, bookings, reviews, geospatial discovery and AI search are not yet implemented.

## Reusable foundations

- Angular and Spring Boot projects are initialized.
- Spring Security, JWT parsing, validation and global errors have initial implementations.
- PostgreSQL/JPA dependencies are configured.
- DTOs are used around authentication and service operations.
- Some queries already include a business identifier.

## Critical items before feature expansion

1. `application.yaml` contains a JWT secret and local database username. Externalize configuration and rotate the secret.
2. Replace `ddl-auto: update` with versioned migrations and schema validation.
3. Replace the single `User → Business` relationship with memberships.
4. Authorize business IDs against the authenticated user's active memberships.
5. Remove or development-profile the token/test controllers before production.
6. Replace ambiguous `LocalDateTime` usage with the documented location-timezone policy.
7. Decide UUID migration before durable production data exists.
8. Replace `Double` prices with fixed-precision decimal values.

## Missing capabilities

- Business locations and PostGIS.
- Resources, schedules, exceptions, slots and bookings.
- Capacity/idempotency/concurrency guarantees.
- Verified reviews and rating projections.
- Search projection, relevance evaluation and NVIDIA adapters.
- PostgreSQL integration, concurrency and architecture tests.
- Production observability and CI quality gates.

## Recommended handling

Preserve the prototype history and migrate incrementally after the ER model is approved. Address secrets and schema management first. Then implement one end-to-end booking mode before adding semantic search; AI quality cannot be evaluated meaningfully without a trustworthy catalog and availability flow.
