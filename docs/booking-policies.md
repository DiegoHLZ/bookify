# Cancellation and rescheduling policies

Each service defines customer-facing operational rules:

- `customerCancellationAllowed`;
- `cancellationNoticeMinutes`;
- `customerRescheduleAllowed`;
- `rescheduleNoticeMinutes`;
- `maxReschedules`.

New services default to allowing cancellation and one reschedule with no minimum notice.
Businesses may configure stricter rules through the normal service create/update contracts.
The active values are copied into each booking at creation, so later service changes cannot
retroactively alter conditions already accepted by a customer.

`POST /api/v1/bookings/{bookingId}/cancel` validates the service policy against the booking's
current UTC start. Repeated cancellation remains idempotent. Business-initiated cancellation
continues to bypass the customer notice rule because it is an operational action requiring a
reason and an active membership.

`POST /api/v1/bookings/{bookingId}/reschedule` accepts `resourceId`, UTC `startsAt` and an
optional `capacitySessionId`. It validates ownership, active status, notice period, maximum
changes, service/resource relationships and authoritative availability in one transaction.

Exclusive-resource changes retain range-exclusion protection. Capacity-session changes
reserve the new session and release the old quantity atomically. A successful change updates
`rescheduleCount`/`lastRescheduledAt` and appends an audit entry containing the previous and
new instants and resources. Repeating the exact target is idempotent and does not consume an
additional reschedule.
