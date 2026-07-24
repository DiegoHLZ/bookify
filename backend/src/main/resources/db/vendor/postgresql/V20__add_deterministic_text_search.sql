CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE OR REPLACE FUNCTION bookify_unaccent(value TEXT)
RETURNS TEXT
LANGUAGE SQL
IMMUTABLE
PARALLEL SAFE
AS $$
    SELECT public.unaccent('public.unaccent', value)
$$;

CREATE INDEX idx_businesses_search_text
    ON businesses USING GIN (
        LOWER(bookify_unaccent(
            name || ' ' || COALESCE(description, '')
        )) gin_trgm_ops
    );

CREATE INDEX idx_locations_search_text
    ON business_locations USING GIN (
        LOWER(bookify_unaccent(
            name || ' ' || city || ' ' || address
        )) gin_trgm_ops
    );

CREATE INDEX idx_services_search_text
    ON services USING GIN (
        LOWER(bookify_unaccent(
            name || ' ' || COALESCE(description, '')
        )) gin_trgm_ops
    );
