# Verified reviews

Only the authenticated owner of a `COMPLETED` booking can create its review through
`POST /api/v1/bookings/{bookingId}/review`. The payload contains a score from 1 to 5 and an
optional comment of up to 1,000 characters.

One booking can produce exactly one review. The database ties the review tenant, booking and
customer through a composite foreign key, preventing fabricated or cross-tenant reviews.
Reviews are marked verified because their source is a completed Bookify reservation.

`GET /api/v1/businesses/{businessId}/reviews` returns verified reviews, and
`GET /api/v1/businesses/{businessId}/rating` returns the average and count.

The business aggregate is updated under a pessimistic business-row lock in the same
transaction as review creation. The review table remains authoritative; the average/count
fields are a rebuildable search projection.
