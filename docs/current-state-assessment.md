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

## Foundation work completed

- Runtime secrets and database credentials are environment-backed.
- Flyway owns schema changes and Hibernate validates the migrated schema.
- The Maven Wrapper is reproducible from a clean checkout.
- Local PostgreSQL/PostGIS infrastructure is described by Compose.
- Token/test controllers load only under the `dev` profile.
- Health probes and UTC JDBC handling are configured.
- GitHub Actions verifies the backend with Java 17.

The previously committed JWT value must still be considered compromised and rotated anywhere it was used.

## Critical items before feature expansion

1. Replace the single `User → Business` relationship with memberships.
2. Authorize business IDs against the authenticated user's active memberships.
3. Replace ambiguous `LocalDateTime` entity fields with the documented location-timezone policy.
4. Decide UUID migration before durable production data exists.
5. Replace `Double` prices with fixed-precision decimal values.

## Missing capabilities

- Business locations and PostGIS.
- Resources, schedules, exceptions, slots and bookings.
- Capacity/idempotency/concurrency guarantees.
- Verified reviews and rating projections.
- Search projection, relevance evaluation and NVIDIA adapters.
- PostgreSQL integration, concurrency and architecture tests.
- Production metrics/logging and broader quality gates.

## Recommended handling

Preserve the prototype history and migrate incrementally after the ER model is approved. The next foundation slice is memberships plus tenant authorization, followed by one end-to-end booking mode. Semantic search comes after a trustworthy catalog and availability flow exist.
