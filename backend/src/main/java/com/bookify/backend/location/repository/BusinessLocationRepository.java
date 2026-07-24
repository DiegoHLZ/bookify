package com.bookify.backend.location.repository;

import com.bookify.backend.location.model.BusinessLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import com.bookify.backend.discovery.dto.NearbyLocationProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BusinessLocationRepository extends JpaRepository<BusinessLocation, Long> {

    List<BusinessLocation> findByBusinessIdOrderByNameAsc(Long businessId);

    Optional<BusinessLocation> findByIdAndBusinessId(Long id, Long businessId);

    boolean existsByBusinessIdAndNameIgnoreCase(Long businessId, String name);

    boolean existsByBusinessIdAndNameIgnoreCaseAndIdNot(Long businessId, String name, Long id);

    long countByBusinessIdAndActiveTrue(Long businessId);

    List<BusinessLocation> findAllByIdInAndBusinessIdAndActiveTrue(
            Collection<Long> ids,
            Long businessId
    );

    @Query(value = """
            SELECT
                b.id AS "businessId",
                b.name AS "businessName",
                b.category_code AS "categoryCode",
                b.rating_average AS "ratingAverage",
                b.rating_count AS "ratingCount",
                l.id AS "locationId",
                l.name AS "locationName",
                l.address AS address,
                l.city AS city,
                l.country_code AS "countryCode",
                l.latitude AS latitude,
                l.longitude AS longitude,
                ST_Distance(
                    l.geography_point,
                    CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography)
                ) AS "distanceMeters"
            FROM business_locations l
            JOIN businesses b ON b.id = l.business_id
            WHERE l.active = TRUE
              AND l.coordinates_verified = TRUE
              AND b.active = TRUE
              AND (:categoryCode IS NULL OR b.category_code = :categoryCode)
              AND b.rating_average >= :minRating
              AND EXISTS (
                  SELECT 1
                  FROM offering_locations ol
                  JOIN services s ON s.id = ol.service_id
                  WHERE ol.business_id = b.id
                    AND ol.location_id = l.id
                    AND s.active = TRUE
              )
              AND ST_DWithin(
                  l.geography_point,
                  CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography),
                  :radiusMeters
              )
            ORDER BY "distanceMeters" ASC, b.rating_average DESC,
                     b.rating_count DESC, l.id ASC
            LIMIT :resultLimit
            """, nativeQuery = true)
    List<NearbyLocationProjection> findVerifiedNearby(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radiusMeters") double radiusMeters,
            @Param("categoryCode") String categoryCode,
            @Param("minRating") BigDecimal minRating,
            @Param("resultLimit") int resultLimit
    );
}
