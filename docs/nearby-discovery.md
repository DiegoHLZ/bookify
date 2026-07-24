# Deterministic nearby discovery

`GET /api/v1/discovery/nearby` accepts WGS84 `latitude`, `longitude`, optional
`radiusKm` (maximum 100), `categoryCode`, `minRating` and `limit` (maximum 100).

Eligibility is deterministic: the business and location must be active, coordinates must be
verified, the location must expose at least one active service, the category/rating filters
must match and PostGIS `ST_DWithin` must place it inside the requested radius.

Results are ordered by exact PostGIS distance, then verified rating average, rating count and
location id. `ST_Distance` provides display distance in meters. A GiST geography index keeps
radius filtering scalable.

Coordinates are unverified whenever a location is created or edited. A platform `ADMIN`
records verification and provenance through:

`POST /api/v1/businesses/{businessId}/locations/{locationId}/coordinates/verify`

with `{"source":"..."}`. This endpoint is intentionally not granted to ordinary business
members. A later geocoding adapter can call the same application operation. AI is never used
to calculate distance or invent locations.

## Deterministic text and availability search

`GET /api/v1/discovery/search` adds optional `text`, `availableAt`, `page` and `size`
parameters to the geographic/category/rating filters. `availableAt` is an ISO local
date-time (for example `2026-08-03T10:00:00`) and is evaluated independently in each
location's IANA timezone. A result is eligible only when at least one active service has an
authoritative concrete slot at that exact local start; matching service IDs are returned.

Text matching covers business name/description, location name/city/address and active service
name/description. PostgreSQL `unaccent` makes matching case- and accent-insensitive, while
trigram GIN indexes bound lookup cost. Ordering remains distance, rating average, rating
count and location ID. Page sizes are limited to 100, pages to 100, and availability search
scans at most 5,000 ordered candidates; clients must narrow overly broad availability
queries.

`GET /api/v1/discovery/businesses/{slug}` is public and returns only an active business,
active verified locations and active services assigned to those public locations. Discovery
endpoints require no JWT; booking and business-management endpoints remain protected.
