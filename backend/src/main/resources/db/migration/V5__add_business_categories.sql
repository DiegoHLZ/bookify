CREATE TABLE business_categories (
    code VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO business_categories (code, name, active) VALUES
    ('BARBERSHOP', 'Barbería', TRUE),
    ('BEAUTY_SALON', 'Salón de belleza', TRUE),
    ('SPORTS_VENUE', 'Instalación deportiva', TRUE),
    ('WELLNESS', 'Bienestar', TRUE),
    ('COWORKING', 'Coworking', TRUE),
    ('PROFESSIONAL_SERVICES', 'Servicios profesionales', TRUE);

ALTER TABLE businesses
    ADD COLUMN category_code VARCHAR(50);

UPDATE businesses
SET category_code = 'PROFESSIONAL_SERVICES'
WHERE category_code IS NULL;

ALTER TABLE businesses
    ALTER COLUMN category_code SET NOT NULL;

ALTER TABLE businesses
    ADD CONSTRAINT fk_business_category
        FOREIGN KEY (category_code) REFERENCES business_categories (code);

CREATE INDEX idx_businesses_category_active
    ON businesses (category_code, active);
