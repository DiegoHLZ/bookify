# API Contracts

All `/api/v1/**` endpoints require `Authorization: Bearer <jwt>` unless explicitly documented otherwise.

## Errors and request correlation

Every response includes `X-Correlation-ID`. Clients may send a correlation ID containing
1–100 letters, digits, dots, underscores or hyphens; invalid or missing values are replaced
with a server-generated UUID. Include this value in support reports.

API errors use a stable JSON envelope:

```json
{
  "timestamp": "2026-07-23T22:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERROR",
  "message": "One or more fields are invalid",
  "path": "/api/auth/register",
  "correlationId": "registration-1",
  "validationErrors": {
    "email": "must not be blank"
  }
}
```

`validationErrors` is present only for field-validation failures. Authentication failures
return `UNAUTHORIZED`; authorization failures return `FORBIDDEN`. Unexpected failures return
`INTERNAL_ERROR` with a generic message, while internal details are restricted to server logs.
The API accepts Bearer JWT authentication only; HTTP Basic and form login are disabled.

## Transactional business onboarding

`POST /api/v1/businesses`

Creates the business, its first location and an active `OWNER` membership for the authenticated user in one database transaction. Any failure rolls back all three writes.

Request:

```json
{
  "name": "Studio Norte",
  "slug": "studio-norte",
  "categoryCode": "BARBERSHOP",
  "description": "Reservas para servicios profesionales",
  "phone": "+51 999 999 999",
  "email": "contacto@studio.test",
  "location": {
    "name": "Sede principal",
    "address": "Av. Principal 123",
    "city": "Lima",
    "countryCode": "PE",
    "timezone": "America/Lima",
    "latitude": -12.046374,
    "longitude": -77.042793
  }
}
```

Returns `201 Created`. Slugs use lowercase kebab-case, country codes use ISO 3166-1 alpha-2 uppercase values, coordinates use WGS84 and timezones use IANA identifiers.

Initial category codes:

- `BARBERSHOP`
- `BEAUTY_SALON`
- `SPORTS_VENUE`
- `WELLNESS`
- `COWORKING`
- `PROFESSIONAL_SERVICES`

Categories are data-backed and can evolve without adding restaurant-specific logic to the business model.

## Authenticated user's businesses

`GET /api/v1/me/businesses`

Returns the active businesses for which the authenticated user has an active membership, including that membership's role and the business category.

## Business locations

Base path: `/api/v1/businesses/{businessId}/locations`

- `POST /` creates an additional location. Requires an active `OWNER` or `ADMIN` membership.
- `GET /` lists the business locations. Requires any active membership.
- `GET /{locationId}` returns one location and verifies that it belongs to the path's business.
- `PUT /{locationId}` replaces the editable location details. Requires `OWNER` or `ADMIN`.
- `PATCH /{locationId}/status` accepts `{"active": true|false}`. Requires `OWNER` or `ADMIN`.

Location names are unique per business ignoring case at the application boundary. A business must retain at least one active location; status changes lock the business row so concurrent requests cannot violate that invariant.

## Service offerings

Base path: `/api/v1/businesses/{businessId}/services`

- `POST /` creates a service and requires `OWNER` or `ADMIN`.
- `GET /` and `GET /{serviceId}` require any active business membership.
- `PUT /{serviceId}` updates the service and replaces its location assignments atomically.
- `DELETE /{serviceId}` performs a soft deletion and requires `OWNER` or `ADMIN`.

Create and update payloads include a non-empty `locationIds` set. Every referenced location must be active and belong to the path's business. Prices use fixed-precision decimal values with at most two fractional digits; currencies currently accept `PEN`, `USD` or `EUR`.

The database stores `business_id` in `offering_locations` and uses composite foreign keys, preventing cross-tenant service/location associations even if an application-layer check is bypassed.

## Bookable resources

Base path: `/api/v1/businesses/{businessId}/locations/{locationId}/resources`

- `POST /` creates a resource. Requires `OWNER` or `ADMIN`.
- `GET /` and `GET /{resourceId}` require any active membership.
- `PUT /{resourceId}` updates its details. Requires `OWNER` or `ADMIN`.
- `PATCH /{resourceId}/status` activates or deactivates it. Requires `OWNER` or `ADMIN`.

Supported types are `PROFESSIONAL`, `COURT`, `ROOM`, `DESK` and `EQUIPMENT`. Capacity must be between 1 and 10,000. Names are unique per location ignoring case at the application boundary. Resources use soft deletion, and a resource cannot be created or reactivated inside an inactive location.

Service-resource assignment uses:

- `GET /api/v1/businesses/{businessId}/services/{serviceId}/resources`
- `PUT /api/v1/businesses/{businessId}/services/{serviceId}/resources`

The `PUT` body is `{"resourceIds":[...]}` and replaces the complete assignment atomically; an empty set removes all assignments. Each resource must be active, belong to the business and be located at a site where the service is offered.

Composite database constraints ensure that service, location and resource tenant identifiers remain consistent even if application validation is bypassed.

## Resource schedules

Base path:
`/api/v1/businesses/{businessId}/locations/{locationId}/resources/{resourceId}`

- `GET /schedule` returns the recurring weekly schedule and the location timezone.
- `PUT /schedule` atomically replaces all recurring rules and requires `OWNER` or `ADMIN`.
- `GET /exceptions?from=YYYY-MM-DD&to=YYYY-MM-DD` lists dated exceptions in a bounded range.
- `PUT /exceptions/{date}` idempotently creates or replaces one exception for that date.
- `DELETE /exceptions/{date}` removes an exception.

Weekly rules use local location time, Java `DayOfWeek` names and the types `AVAILABLE` or `BREAK`. Available intervals cannot overlap. Breaks cannot overlap and each break must be fully contained in one available interval. An empty `rules` list clears the weekly schedule.

Exceptions use `CLOSED`, with no times, or `CUSTOM_HOURS`, with a valid `startTime` and `endTime`. One exception per resource/date makes exception precedence deterministic. Query ranges cannot exceed 366 days.

Database checks repeat the time, enum and tenant invariants. Slot generation converts these
local rules through the location's IANA timezone and returns concrete UTC instants.

## Service availability

`GET /api/v1/businesses/{businessId}/locations/{locationId}/services/{serviceId}/availability`
accepts `from`, `to` (inclusive local dates) and optional `intervalMinutes` (default `15`,
minimum `5`, maximum `1440`). The range is limited to 31 days.
It is a customer-facing read endpoint available to any authenticated user; business membership
is not required.

The service and location must be active and linked. Only active resources explicitly assigned
to that service and location participate. Each slot contains the resource identity, local
start/end, and concrete UTC `startAt`/`endAt` instants. Results are ordered by UTC start and
resource id.

Recurring `AVAILABLE` periods are split by `BREAK` periods. A `CLOSED` exception removes the
date; `CUSTOM_HOURS` replaces recurring availability while recurring breaks that intersect it
still apply. Service duration determines whether a candidate fits, and `intervalMinutes`
controls candidate start cadence. IANA timezone rules skip nonexistent DST local starts and
emit both concrete instants for ambiguous repeated local starts.

## Exclusive-resource bookings

- `POST /api/v1/bookings` creates a confirmed booking for the authenticated customer.
  `Idempotency-Key` is required (1–100 characters).
- `GET /api/v1/bookings` lists the authenticated customer's bookings.
- `POST /api/v1/bookings/{bookingId}/cancel` idempotently cancels a customer-owned active
  booking.
- `GET /api/v1/businesses/{businessId}/bookings` lists operational bookings for active
  business members.
- `PATCH /api/v1/businesses/{businessId}/bookings/{bookingId}/status` applies an allowed
  operational transition for an active business member.
- `GET /api/v1/businesses/{businessId}/bookings/{bookingId}/history` returns the immutable
  status audit trail.

Creation accepts `businessId`, `locationId`, `serviceId`, `resourceId`, UTC `startsAt` and
optional bounded `notes`. The server derives `endsAt` from the current service duration and
revalidates the exact slot after locking the customer and resource. Replaying the same
customer/key returns the original booking. Active overlapping intervals return `409 Conflict`.

`PENDING` and `CONFIRMED` bookings remove every intersecting candidate from availability.
Cancellation records `cancelledAt` and releases the interval. Responses include UTC instants
plus local date-times and the location's IANA timezone.

Allowed transitions are `PENDING → CONFIRMED|REJECTED|CANCELLED` and
`CONFIRMED → COMPLETED|NO_SHOW|CANCELLED`. Terminal bookings cannot be reopened.
Business cancellation or rejection requires a reason. Creation, customer cancellation and
every business transition record actor, previous/new status, reason and timestamp in the same
database transaction.
