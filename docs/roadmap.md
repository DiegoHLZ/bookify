# Bookify — Delivery Roadmap

## Phase 0 — Product and architecture baseline

- Approve target launch categories and the three booking modes.
- Approve tenant/location/catalog/availability/booking/review ER model.
- Define NFRs, ADRs, privacy boundaries and AI evaluation plan.
- Reconcile the repository prototype with the target model.

Exit: no unresolved product-domain ambiguity; capacity, location, time and tenant invariants are testable.

## Phase 1 — Secure foundation

- Externalize/rotate secrets.
- Add PostgreSQL/PostGIS migrations and schema validation.
- Establish modular boundaries, stable errors, CI and integration tests.
- Add logs, correlation IDs and health checks.

Exit: clean checkout builds/tests; schema is reproducible; no committed credentials.

Status: **in progress**. Environment-backed secrets, Maven Wrapper repair, Flyway baseline,
schema validation, health probes, backend CI, local PostGIS Compose infrastructure, safe
error envelopes, correlation IDs and request outcome logs are complete. Metrics dashboards
and automated PostgreSQL integration tests remain.

## Phase 2 — Identity, businesses and locations

- Customer identities and business memberships.
- Business/location profiles and verified coordinates.
- Deny-by-default tenant authorization.

Exit: cross-tenant tests pass and nearby queries use correct geospatial calculations.

Status: **complete**. Customer registration is tenant-independent, JWTs no longer embed a
business ID, and service-management access is protected by active business memberships.
Transactional onboarding creates a categorized business, first location and `OWNER`
membership. Additional-location CRUD, role enforcement, cross-tenant isolation and the
last-active-location invariant have integration coverage. Verified coordinates and indexed
PostGIS nearby queries complete the location/discovery foundation.

## Phase 3 — Catalog and booking modes

- Offerings, resources, schedules, exceptions and concrete slots.
- Implement one appointment category and one resource/capacity category.
- Validate each booking-mode policy independently.

Exit: businesses configure offerings and the system calculates correct availability.

Status: **complete**. Offerings use fixed-precision prices and are assigned atomically to active locations. Generic resources enforce tenant/location consistency and support recurring local-time availability, breaks and dated closures/custom hours. Availability queries generate concrete duration-aware UTC slots and handle IANA timezone gaps and overlaps.

## Phase 4 — Transactional booking slice

- Discovery filters and availability.
- Idempotent booking creation/cancellation.
- Capacity locking and concurrency tests.
- Customer/business booking views.

Exit: discovery → booking works end to end and cannot overbook under concurrent load.

Status: **complete**. Exclusive-resource bookings support idempotent creation, customer
cancellation and rescheduling, customer/business views, availability exclusion, two-layer
concurrency protection, audited state transitions and configurable policy snapshots.
Shared-capacity sessions use transactional capacity locking.

## Phase 5 — Reviews and baseline ranking

- One verified review per completed booking.
- Rating aggregates and abuse/moderation controls.
- Deterministic ranking using eligibility, distance, rating confidence and availability.

Exit: ranking is explainable and works without AI.

Status: **complete**. Completed bookings can produce one verified review, aggregate ratings
are maintained transactionally and deterministic nearby discovery orders eligible locations
by exact distance, rating average, rating count and stable identifiers.

## Phase 6 — NVIDIA-assisted discovery experiment

- Create a labeled multilingual query set.
- Build a derived search projection.
- Integrate NVIDIA embeddings behind `EmbeddingPort`.
- Benchmark hybrid retrieval against the deterministic baseline.
- Add bounded NVIDIA reranking only if relevance gains justify latency/cost.
- Add feature flags, timeouts, fallback and model observability.

Exit: zero ineligible/hallucinated results, measurable relevance improvement, accepted latency/unit economics and safe fallback.

## Phase 7 — Production readiness and MVP release

- Security/accessibility/performance testing.
- Backup/restore and deployment runbooks.
- SLO dashboards and alerts.
- Controlled rollout to selected business categories.

Exit: restore tested, critical findings resolved and production-like SLOs met.

## Post-MVP

- More business categories and booking policies.
- Payments, deposits and notifications.
- Personalized recommendations with consent and privacy review.
- Multi-branch operations and staff scheduling.
- Conversational booking assistant with explicit confirmation.

Microservices, dedicated GPU infrastructure and autonomous booking agents require separate ADRs and measured justification.
