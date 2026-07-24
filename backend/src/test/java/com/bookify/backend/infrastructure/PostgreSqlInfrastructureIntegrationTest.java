package com.bookify.backend.infrastructure;

import com.bookify.backend.discovery.dto.NearbyLocationProjection;
import com.bookify.backend.location.repository.BusinessLocationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
class PostgreSqlInfrastructureIntegrationTest {
    private static final String PROBE_SLUG = "_postgres_it_probe";

    @Container
    static final PostgreSQLContainer<?> postgreSql =
            new PostgreSQLContainer<>(
                    DockerImageName.parse("postgis/postgis:16-3.4")
                            .asCompatibleSubstituteFor("postgres")
            )
                    .withDatabaseName("bookify_it")
                    .withUsername("bookify")
                    .withPassword("bookify_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSql::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSql::getUsername);
        registry.add("spring.datasource.password", postgreSql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BusinessLocationRepository locationRepository;

    @AfterEach
    void removeProbe() {
        jdbcTemplate.update("""
                DELETE FROM offering_locations
                WHERE business_id IN (SELECT id FROM businesses WHERE slug = ?)
                """, PROBE_SLUG);
        jdbcTemplate.update("""
                DELETE FROM services
                WHERE business_id IN (SELECT id FROM businesses WHERE slug = ?)
                """, PROBE_SLUG);
        jdbcTemplate.update("""
                DELETE FROM business_locations
                WHERE business_id IN (SELECT id FROM businesses WHERE slug = ?)
                """, PROBE_SLUG);
        jdbcTemplate.update("DELETE FROM businesses WHERE slug = ?", PROBE_SLUG);
    }

    @Test
    void appliesPostgreSqlOnlyMigrationsAndSpatialIndexes() {
        assertNotNull(jdbcTemplate.queryForObject(
                "SELECT extversion FROM pg_extension WHERE extname = 'postgis'",
                String.class
        ));
        assertNotNull(jdbcTemplate.queryForObject(
                "SELECT extversion FROM pg_extension WHERE extname = 'btree_gist'",
                String.class
        ));

        String indexDefinition = jdbcTemplate.queryForObject("""
                SELECT indexdef
                FROM pg_indexes
                WHERE schemaname = 'public' AND indexname = 'idx_locations_geography'
                """, String.class);
        assertNotNull(indexDefinition);
        assertTrue(indexDefinition.toLowerCase().contains("using gist"));

        assertEquals("ALWAYS", jdbcTemplate.queryForObject("""
                SELECT is_generated
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'business_locations'
                  AND column_name = 'geography_point'
                """, String.class));

        assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM pg_constraint
                WHERE conname = 'ex_bookings_resource_time'
                  AND contype = 'x'
                """, Integer.class));
    }

    @Test
    void executesVerifiedNearbyQueryAgainstPostGis() {
        insertDiscoveryProbe();

        List<NearbyLocationProjection> results = locationRepository.searchVerifiedNearby(
                -12.046374,
                -77.042793,
                1_000,
                "PROFESSIONAL_SERVICES",
                new BigDecimal("4.5"),
                "masaje",
                0,
                10
        );

        assertEquals(1, results.size());
        NearbyLocationProjection result = results.get(0);
        assertEquals("PostgreSQL IT Probe", result.getBusinessName());
        assertEquals("Probe Lima", result.getLocationName());
        assertEquals(0.0, result.getDistanceMeters(), 0.01);
        assertEquals(new BigDecimal("4.80"), result.getRatingAverage());
    }

    private void insertDiscoveryProbe() {
        jdbcTemplate.update("""
                WITH business AS (
                    INSERT INTO businesses (
                        name, slug, category_code, active, created_at, updated_at,
                        rating_average, rating_count, rating_sum
                    )
                    VALUES (
                        'PostgreSQL IT Probe', ?, 'PROFESSIONAL_SERVICES', TRUE,
                        NOW(), NOW(), 4.80, 25, 120
                    )
                    RETURNING id
                ),
                location AS (
                    INSERT INTO business_locations (
                        business_id, name, address, city, country_code, timezone,
                        latitude, longitude, active, coordinates_verified,
                        coordinates_verified_at, coordinate_source, created_at, updated_at
                    )
                    SELECT
                        id, 'Probe Lima', 'Temporal', 'Lima', 'PE', 'America/Lima',
                        -12.046374, -77.042793, TRUE, TRUE,
                        NOW(), 'postgres-integration-test', NOW(), NOW()
                    FROM business
                    RETURNING business_id, id
                ),
                service AS (
                    INSERT INTO services (
                        name, duration_minutes, price, currency, active,
                        business_id, created_at, updated_at
                    )
                    SELECT
                        'Masáje terapéutico', 30, 10.00, 'PEN', TRUE, id, NOW(), NOW()
                    FROM business
                    RETURNING business_id, id
                )
                INSERT INTO offering_locations (business_id, service_id, location_id)
                SELECT service.business_id, service.id, location.id
                FROM service
                JOIN location ON location.business_id = service.business_id
                """, PROBE_SLUG);
    }
}
