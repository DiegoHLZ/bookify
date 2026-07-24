# Current State Assessment

Assessment date: 2026-07-23.

## Summary

The Angular/Spring Boot repository is an early generic service-booking implementation. Business onboarding, initial locations and owner memberships now form a working transactional slice, while resources, robust availability, bookings, reviews, geospatial discovery and AI search are not yet implemented.

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
- Business access is modeled through active memberships rather than a business ID embedded in JWT claims.
- Flyway migrations preserve legacy user/business associations and then remove the obsolete direct foreign key.
- Authenticated onboarding creates a categorized business, its first WGS84 location and the owner's membership atomically.
- Business categories are data-backed so the core model is not tied to restaurants or another single vertical.
- Users can list their active business memberships through `/api/v1/me/businesses`.
- Location management supports create, list, detail, update and activation changes with tenant/role checks.
- The final active location cannot be disabled; the invariant is protected with a transactional business-row lock.
- Service prices use fixed-precision decimals rather than floating-point values.
- Services are assigned to one or more active business locations, with database-enforced tenant consistency.
- Generic bookable resources cover professionals, courts, rooms, desks and equipment with capacity and active state.
- Service-resource assignments are atomic and constrained to locations where the service is offered.
- Resources support recurring local-time availability, contained breaks and deterministic dated exceptions.

The previously committed JWT value must still be considered compromised and rotated anywhere it was used.

## Critical items before feature expansion

1. Replace ambiguous `LocalDateTime` entity fields with the documented location-timezone policy.
2. Decide UUID migration before durable production data exists.
3. Add PostGIS geography indexing, coordinate verification and verified nearby queries.

## Missing capabilities

- PostGIS nearby search and coordinate verification.
- Concrete availability slots and bookings.
- Capacity/idempotency/concurrency guarantees.
- Verified reviews and rating projections.
- Search projection, relevance evaluation and NVIDIA adapters.
- PostgreSQL integration, concurrency and architecture tests.
- Production metrics/logging and broader quality gates.

## Recommended handling

Preserve the prototype history and migrate incrementally after the ER model is approved. Membership-based tenant authorization, transactional onboarding and location management are now established. The next slice links offerings to locations and introduces resources/schedules for one end-to-end booking mode. Semantic search comes after a trustworthy catalog and availability flow exist.
