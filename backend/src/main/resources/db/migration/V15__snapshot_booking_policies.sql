ALTER TABLE bookings
    ADD COLUMN cancellation_allowed_snapshot BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE bookings
    ADD COLUMN cancellation_notice_snapshot INTEGER NOT NULL DEFAULT 0;

ALTER TABLE bookings
    ADD COLUMN reschedule_allowed_snapshot BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE bookings
    ADD COLUMN reschedule_notice_snapshot INTEGER NOT NULL DEFAULT 0;

ALTER TABLE bookings
    ADD COLUMN max_reschedules_snapshot INTEGER NOT NULL DEFAULT 1;

ALTER TABLE bookings
    ADD CONSTRAINT ck_booking_policy_snapshot CHECK (
        cancellation_notice_snapshot BETWEEN 0 AND 525600
        AND reschedule_notice_snapshot BETWEEN 0 AND 525600
        AND max_reschedules_snapshot BETWEEN 0 AND 100
    );
