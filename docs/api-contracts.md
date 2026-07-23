# API Contracts

All `/api/v1/**` endpoints require `Authorization: Bearer <jwt>` unless explicitly documented otherwise.

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
