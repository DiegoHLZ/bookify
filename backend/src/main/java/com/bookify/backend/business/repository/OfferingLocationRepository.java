package com.bookify.backend.business.repository;

import com.bookify.backend.business.model.OfferingLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OfferingLocationRepository extends JpaRepository<OfferingLocation, Long> {

    @Query("""
            select link.location.id
            from OfferingLocation link
            where link.service.id = :serviceId
            order by link.location.id
            """)
    List<Long> findLocationIdsByServiceId(@Param("serviceId") Long serviceId);

    @Modifying
    @Query("delete from OfferingLocation link where link.service.id = :serviceId")
    void deleteByServiceId(@Param("serviceId") Long serviceId);

    boolean existsByBusinessIdAndServiceIdAndLocationId(
            Long businessId,
            Long serviceId,
            Long locationId
    );

    @Query("""
            select link.service.id
            from OfferingLocation link
            where link.business.id = :businessId
              and link.location.id = :locationId
              and link.service.active = true
            order by link.service.id
            """)
    List<Long> findActiveServiceIdsAtLocation(
            @Param("businessId") Long businessId,
            @Param("locationId") Long locationId
    );

    @Query("""
            select
                link.location.id as locationId,
                link.business.description as businessDescription,
                link.service.name as serviceName,
                link.service.description as serviceDescription
            from OfferingLocation link
            where link.location.id in :locationIds
              and link.business.active = true
              and link.location.active = true
              and link.service.active = true
            order by link.location.id, link.service.id
            """)
    List<CanonicalSearchTextProjection> findCanonicalSearchText(
            @Param("locationIds") Collection<Long> locationIds
    );
}
