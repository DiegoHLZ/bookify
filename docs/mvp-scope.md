# Bookify — MVP Scope

## Objective

Validate a multi-category local discovery and booking flow without building separate domain logic for every industry.

## MVP launch categories

Begin with two or three categories that exercise the common engine, for example:

- barbershop/beauty appointment;
- sports court or coworking-room rental;
- capacity-based class.

Restaurants can be added when party-size and table-allocation policies are accepted. The platform model supports future categories, but each category must pass capability and usability tests before launch.

## In scope

### Identity and tenancy

- Customer and business-staff registration/login.
- Secure password hashing and role-based authorization.
- Multiple staff members per business through memberships.
- One business with one or more locations.
- Strict tenant isolation.

### Catalog and discovery

- Business profile, category, location and geocoordinates.
- Service offerings with duration, optional price and booking mode.
- Search by text, category, distance and requested date/time.
- Rating average and count from Bookify reviews.
- Deterministic search fallback without AI.

### Schedules, resources and availability

- Location opening hours.
- Resource schedules and dated exceptions.
- Resources such as staff, rooms, courts, chairs or equipment.
- Exclusive-resource and capacity-based availability.
- Authoritative revalidation during booking creation.

### Bookings

- Create, view and cancel a booking.
- Business staff can list and update bookings for authorized businesses.
- States: `PENDING`, `CONFIRMED`, `CANCELLED`, `REJECTED`, `COMPLETED`, `NO_SHOW`.
- Idempotent creation and database-level conflict protection.
- Historical retention.

### Reviews

- A customer can review a completed booking once.
- Rating is 1–5 with optional bounded text.
- Businesses cannot directly edit customer reviews.

### AI discovery experiment

- Semantic retrieval over approved business and offering content.
- Hybrid ranking using semantic relevance plus structured eligibility.
- Optional NVIDIA reranking over a bounded candidate set.
- Feature flag, timeout and deterministic fallback.
- Offline relevance test set and online search-quality metrics.

## Out of scope for the first release

- Universal support for every business category.
- Payments, deposits, promotions and loyalty programs.
- Paid ranking or advertising.
- Imported third-party ratings presented as Bookify ratings.
- Fully conversational agents that create bookings autonomously.
- Personalized ranking based on sensitive profiling.
- Dynamic pricing, overbooking and AI-generated availability.
- Native mobile applications.
- Microservices, event brokers and mandatory GPU infrastructure.

## Core rules

1. A location belongs to exactly one business.
2. A service offering belongs to one business and is available at one or more of its locations.
3. A resource and booking cannot cross business/location boundaries.
4. Concrete bookings use UTC instants; each location stores an IANA timezone.
5. Active exclusive-resource bookings cannot overlap.
6. Capacity-based bookings cannot exceed the configured capacity.
7. Search availability is advisory; creation revalidates in a transaction.
8. Replayed requests with the same customer and idempotency key return the original booking.
9. Only completed bookings can produce one verified review.
10. AI ranking can reorder eligible candidates but cannot bypass location, availability, authorization or policy filters.
11. If AI fails or exceeds its latency budget, structured search still works.

## Acceptance criteria

- Cross-tenant access attempts are rejected and integration-tested.
- Two concurrent requests cannot overbook the same exclusive resource or capacity.
- A natural-language query returns only indexed businesses within active filters.
- Every displayed distance is calculated from stored coordinates.
- Ratings identify their source and Bookify aggregates use verified reviews.
- AI-disabled and AI-timeout paths still allow discovery and booking.
- Search and booking meet the targets in [Non-functional Requirements](./non-functional-requirements.md).

## Definition of done

A feature includes domain rules, authorization, validation, migration, automated tests, API contract, observability and documentation. AI features additionally require a quality benchmark, cost/latency measurement, fallback and safety evaluation.
