# Bookify — Software Architecture

## Style and modules

Use a modular monolith with ports-and-adapters boundaries:

```text
identity
tenancy
catalog
locations
scheduling
availability
bookings
reviews
search
notifications
administration
```

Each module uses:

```text
module/
├── domain/          # Entities, value objects, policies
├── application/     # Commands, queries, use cases, ports
├── infrastructure/  # JPA and provider adapters
└── api/             # REST controllers and DTOs
```

Dependency direction is `api → application → domain`; infrastructure implements inward-facing ports. Modules cannot call another module's repository or expose persistence entities.

## Booking kernel

The common booking language is:

- `Business` and `BusinessLocation`;
- `ServiceOffering`;
- `Resource`;
- `AvailabilitySlot`;
- `Booking`;
- `BookingMode`.

Behavior that differs by mode uses policies:

```text
AvailabilityPolicy
├── AppointmentAvailabilityPolicy
├── ExclusiveResourceAvailabilityPolicy
└── CapacitySessionAvailabilityPolicy
```

This avoids both duplicated vertical applications and a large switch statement distributed throughout controllers/services.

## Search use case

1. Parse explicit filters and customer coordinates.
2. Apply status, category, radius and time/availability filters.
3. Retrieve keyword and semantic candidates.
4. Join only canonical business/offering/location metadata.
5. Optionally rerank a bounded set.
6. Apply stable business rules and return explanations such as distance, rating and availability.
7. Record anonymized quality/latency signals.

`EmbeddingPort` and `RerankingPort` hide NVIDIA-specific APIs. Provider DTOs, model names and SDKs remain in infrastructure. A provider failure returns the structured fallback result.

## Booking use case

1. Validate identity, offering, location, quantity and idempotency key.
2. Begin a transaction and lock the concrete slot/capacity record.
3. Revalidate tenant relationships, current status and capacity.
4. Reserve capacity and insert the booking.
5. Commit and return the canonical representation.
6. Trigger non-authoritative confirmation after commit.

## API conventions

- Versioned REST resources under `/api/v1`.
- Stable errors with `code`, `message`, `status`, `path`, `correlationId`, `timestamp` and field violations.
- Bounded cursor or page pagination with deterministic sorting.
- `Idempotency-Key` for booking creation.
- Coordinates, radius and requested local time have explicit units/formats.
- Tenant authorization is resolved server-side.

## Security and privacy

- Deny-by-default authorization.
- Tenant-scoped repository operations require `businessId`.
- Adaptive password hashing and runtime-injected secrets.
- Rate limiting for authentication, search abuse and review creation.
- Location permission is purpose-limited; precise customer location is not retained unless required and consented.
- Search logs avoid raw sensitive queries and precise coordinates when aggregated data suffices.
- Review moderation and abuse controls are explicit application concerns.

## Testing

- Domain tests for every booking-mode policy.
- PostgreSQL/PostGIS integration tests for tenant, distance and capacity behavior.
- Concurrency tests for exclusive and capacity slots.
- Contract tests for NVIDIA adapters and timeout/fallback behavior.
- Offline search evaluation using labeled queries: recall@k, NDCG@k and zero ineligible results.
- End-to-end tests for discovery → availability → booking → verified review.
- Architecture tests for module boundaries.

## Evolution

Keep bookings strongly consistent and search eventually consistent. Extract search/AI only if its deployment or scaling profile requires it. Extracting the transactional core into microservices is not an MVP objective.
