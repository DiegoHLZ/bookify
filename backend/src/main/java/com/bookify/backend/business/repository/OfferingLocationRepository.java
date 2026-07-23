package com.bookify.backend.business.repository;

import com.bookify.backend.business.model.OfferingLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
