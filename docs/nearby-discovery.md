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
