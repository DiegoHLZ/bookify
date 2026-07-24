# Architecture Decisions

Material changes should become dated ADR files under `docs/adr/`.

## ADR-001 — Horizontal local-booking platform

- **Status:** Accepted
- **Decision:** Bookify serves multiple local-business categories through a common booking kernel and explicit booking modes.
- **Consequences:** Generic abstractions are allowed only when their invariants are clear. Category-specific behavior is implemented as policies/capabilities, not conditionals spread throughout the application.

## ADR-002 — Modular monolith

- **Status:** Accepted
- **Decision:** Use one Spring Boot deployable divided into identity, tenancy, catalog, locations, scheduling, availability, bookings, reviews, search, notifications and administration.
- **Consequences:** Microservices are deferred. Modules communicate through application contracts, never each other's repositories.

## ADR-003 — Shared PostgreSQL and logical tenant isolation

- **Status:** Accepted
- **Decision:** Businesses share one schema. Tenant-scoped data carries `business_id`, and staff access is resolved through memberships.
- **Consequences:** Tenant predicates and cross-tenant integration tests are mandatory.

## ADR-004 — Locations and geospatial search

- **Status:** Accepted
- **Decision:** A business owns locations. Coordinates use WGS84 and distance filtering uses PostgreSQL/PostGIS.
- **Consequences:** Search does not ask an LLM to calculate distance. Address geocoding requires a replaceable provider and stored provenance.

## ADR-005 — Booking modes and derived availability

- **Status:** Accepted
- **Decision:** The common kernel initially supports `APPOINTMENT`, `EXCLUSIVE_RESOURCE` and `CAPACITY_SESSION`. Availability derives from schedules, resources, exceptions and bookings.
- **Consequences:** Each mode has its own conflict/capacity policy behind one application contract.

## ADR-006 — Concurrency and idempotency

- **Status:** Accepted
- **Decision:** Database constraints/locking protect capacity, and idempotency keys protect create requests.
- **Consequences:** Conflicts return a stable `409`; prior search results never guarantee a booking.

## ADR-007 — Time representation

- **Status:** Accepted
- **Decision:** Locations store IANA timezones. Recurring schedules use local time; bookings use UTC instants and API offsets.

## ADR-008 — Hybrid AI-assisted discovery

- **Status:** Accepted
- **Decision:** Structured filters establish eligibility, semantic retrieval improves recall and a bounded reranker may improve ordering. NVIDIA integrations sit behind `EmbeddingPort` and `RerankingPort`.
- **Consequences:** Search has timeouts, feature flags, cost controls and a deterministic fallback. AI cannot write booking state or generate factual listing fields.

## ADR-009 — Verified review source

- **Status:** Accepted
- **Decision:** Bookify ratings come from one review per completed booking. External ratings, if later licensed, remain separately attributed.
- **Consequences:** Ranking stores rating count as well as average and includes anti-abuse monitoring.

## ADR-010 — Exclusive-resource bookings use range exclusion first

- **Status:** Accepted.
- **Decision:** The first transactional slice books one exclusive resource per interval.
  Creation serializes on customer and resource rows, revalidates generated availability, and
  PostgreSQL enforces non-overlap with a partial GiST exclusion constraint over active
  `tstzrange` values. A unique customer/idempotency-key constraint protects retries.
- **Consequences:** Professionals, courts, rooms, desks and equipment can be booked safely.
  Shared-capacity sessions remain a separate policy requiring concrete session capacity and
  row locking; resource `capacity` is not silently treated as concurrent booking capacity.
