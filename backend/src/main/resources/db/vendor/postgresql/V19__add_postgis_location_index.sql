CREATE EXTENSION IF NOT EXISTS postgis;

ALTER TABLE business_locations
    ADD COLUMN geography_point geography(Point, 4326)
    GENERATED ALWAYS AS (
        CAST(
            ST_SetSRID(
                ST_MakePoint(
                    CAST(longitude AS double precision),
                    CAST(latitude AS double precision)
                ),
                4326
            )
            AS geography
        )
    ) STORED;

CREATE INDEX idx_locations_geography
    ON business_locations USING GIST (geography_point);
