ALTER TABLE businesses
    ADD COLUMN rating_sum INTEGER NOT NULL DEFAULT 0;

UPDATE businesses
SET rating_sum = CAST(ROUND(rating_average * rating_count) AS INTEGER);

ALTER TABLE businesses
    ADD CONSTRAINT ck_business_rating_sum CHECK (
        rating_sum >= 0 AND rating_sum <= rating_count * 5
    );
