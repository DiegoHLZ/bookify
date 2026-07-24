ALTER TABLE services
    ADD COLUMN customer_cancellation_allowed BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE services
    ADD COLUMN cancellation_notice_minutes INTEGER NOT NULL DEFAULT 0;

ALTER TABLE services
    ADD COLUMN customer_reschedule_allowed BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE services
    ADD COLUMN reschedule_notice_minutes INTEGER NOT NULL DEFAULT 0;

ALTER TABLE services
    ADD COLUMN max_reschedules INTEGER NOT NULL DEFAULT 1;

ALTER TABLE services
    ADD CONSTRAINT ck_service_cancellation_notice CHECK (
        cancellation_notice_minutes BETWEEN 0 AND 525600
    );

ALTER TABLE services
    ADD CONSTRAINT ck_service_reschedule_notice CHECK (
        reschedule_notice_minutes BETWEEN 0 AND 525600
    );

ALTER TABLE services
    ADD CONSTRAINT ck_service_max_reschedules CHECK (
        max_reschedules BETWEEN 0 AND 100
    );

ALTER TABLE bookings
    ADD COLUMN reschedule_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE bookings
    ADD COLUMN last_rescheduled_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE bookings
    ADD CONSTRAINT ck_booking_reschedule_count CHECK (
        reschedule_count BETWEEN 0 AND 100
    );

ALTER TABLE bookings
    ADD CONSTRAINT ck_booking_reschedule_audit CHECK (
        (reschedule_count = 0 AND last_rescheduled_at IS NULL)
        OR (reschedule_count > 0 AND last_rescheduled_at IS NOT NULL)
    );
