ALTER TABLE business_locations
    ADD COLUMN coordinates_verified BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE business_locations
    ADD COLUMN coordinates_verified_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE business_locations
    ADD COLUMN coordinate_source VARCHAR(100);

ALTER TABLE business_locations
    ADD CONSTRAINT ck_location_coordinate_verification CHECK (
        (coordinates_verified = FALSE
            AND coordinates_verified_at IS NULL
            AND coordinate_source IS NULL)
        OR
        (coordinates_verified = TRUE
            AND coordinates_verified_at IS NOT NULL
            AND coordinate_source IS NOT NULL)
    );

CREATE INDEX idx_locations_verified_active
    ON business_locations (coordinates_verified, active);
