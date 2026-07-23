# Non-functional Requirements

These targets guide implementation and release decisions. They are initial service-level objectives (SLOs), not claims about the current prototype.

## Reliability and consistency

- Reservation creation is transactional and has no partial-success state.
- The database is the final guard against overlapping active reservations.
- Mutation endpoints that may be retried support idempotency.
- Production schema changes use versioned migrations; automatic Hibernate schema updates are disabled.
- Backups and restore procedures must be exercised before production launch.

## Performance targets

Measured at the API boundary under the agreed MVP load profile:

- Read endpoints: p95 latency below 500 ms.
- Reservation creation: p95 latency below 800 ms, excluding external notifications.
- Monthly API availability target: 99.5% for the MVP.

Before launch, a lightweight load test must define the actual concurrent-user baseline and confirm these targets.

## Security and privacy

- Passwords use an adaptive hash such as Argon2id or bcrypt; plaintext passwords are never logged or stored.
- Secrets come from environment variables or a secret manager, never committed configuration.
- Authorization is deny-by-default and enforced in the backend.
- Every tenant-scoped query includes an authorized business boundary.
- Input validation, safe error responses, rate limits on authentication endpoints and secure HTTP headers are required.
- Logs redact tokens, passwords and unnecessary personal data.
- Personal-data retention and deletion policies must be defined before production.

## Observability

- Structured logs include a correlation ID, route, outcome and safe tenant/user identifiers.
- Metrics cover request latency/error rate, authentication failures, reservation conflicts and database health.
- Health endpoints distinguish liveness from readiness.
- Alerts are actionable and initially focus on sustained server errors, database unavailability and failed reservation writes.

## Maintainability

- Modules expose use cases/contracts, not repositories or persistence entities.
- Controllers contain transport concerns only.
- Domain and application services own business rules and state transitions.
- Architecture tests enforce module boundaries when implementation begins.
- Public API changes are documented and backward-compatible within the supported version.

## Accessibility and usability

- Customer and staff web flows target WCAG 2.2 AA.
- Forms provide keyboard access, programmatic labels and useful validation messages.
- Dates and times display the business-location timezone clearly.

## Testing quality gates

- Unit tests cover domain policies and reservation state transitions.
- Integration tests use PostgreSQL and cover constraints, transactions and tenant isolation.
- API tests cover authentication, authorization, validation and stable errors.
- End-to-end tests cover the primary customer booking and staff-management journeys.
- Concurrency tests attempt simultaneous overlapping bookings.

Coverage percentage is not used as the sole quality measure; critical business rules require explicit scenario coverage.
