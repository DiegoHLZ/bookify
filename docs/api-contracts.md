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
