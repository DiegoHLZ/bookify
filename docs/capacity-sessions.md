# Shared-capacity sessions

Bookify supports two implemented service booking modes:

- `EXCLUSIVE_RESOURCE` (default): one active booking owns the resource interval and quantity is `1`.
- `CAPACITY_SESSION`: several customers reserve positive quantities in one concrete session.

## Management API

Base path:
`/api/v1/businesses/{businessId}/locations/{locationId}/services/{serviceId}/sessions`

- `POST /` creates an open session from `resourceId`, UTC `startsAt` and `totalCapacity`.
- `GET /` lists open sessions for an active business member.
- `POST /{sessionId}/cancel` cancels a session that has no reserved places.

Creation and cancellation require `OWNER` or `ADMIN`. The service must use
`CAPACITY_SESSION`; the server derives `endsAt` from its configured duration. The selected
resource must be active and assigned to the service at the requested location.

## Availability and booking

Availability for a capacity service returns persisted open sessions with
`capacitySessionId` and `remainingCapacity`. Full or cancelled sessions are excluded.

`POST /api/v1/bookings` accepts the existing tenant, location, service, resource and UTC
start fields plus optional `capacitySessionId` and positive `quantity` (default `1`).

For capacity bookings, the session must exactly match the tenant, location, service,
resource and start instant. Creation pessimistically locks the session row and increments
its reserved counter only when:

`reservedCapacity + quantity <= totalCapacity`

The booking insert and capacity update share one transaction. Customer cancellation and
business rejection/cancellation release the quantity exactly once.

## Database protection

`capacity_sessions` stores total/reserved capacity, status and an optimistic version, with
tenant-consistent composite foreign keys and check constraints. PostgreSQL's active booking
range-exclusion constraint applies only to exclusive bookings, allowing participants in the
same capacity session to overlap intentionally.

Concurrency integration coverage verifies that simultaneous requests cannot exceed the
configured capacity.
