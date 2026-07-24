ALTER TABLE bookings DROP CONSTRAINT ex_bookings_resource_time;

ALTER TABLE bookings
    ADD CONSTRAINT ex_bookings_resource_time
    EXCLUDE USING gist (
        resource_id WITH =,
        tstzrange(starts_at, ends_at, '[)') WITH &&
    )
    WHERE (
        status IN ('PENDING', 'CONFIRMED')
        AND capacity_session_id IS NULL
    );
