package com.bookify.backend.resource.repository;

import com.bookify.backend.resource.model.BookableResource;
import com.bookify.backend.resource.model.OfferingResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OfferingResourceRepository extends JpaRepository<OfferingResource, Long> {

    @Query("""
            select link.resource.id
            from OfferingResource link
            where link.service.id = :serviceId
            order by link.resource.id
            """)
    List<Long> findResourceIdsByServiceId(@Param("serviceId") Long serviceId);

    @Query("""
            select link.resource
            from OfferingResource link
            where link.business.id = :businessId
              and link.service.id = :serviceId
              and link.location.id = :locationId
              and link.resource.active = true
            order by link.resource.id
            """)
    List<BookableResource> findActiveResources(
            @Param("businessId") Long businessId,
            @Param("serviceId") Long serviceId,
            @Param("locationId") Long locationId
    );

    boolean existsByBusinessIdAndServiceIdAndLocationIdAndResourceId(
            Long businessId,
            Long serviceId,
            Long locationId,
            Long resourceId
    );

    @Modifying
    @Query("delete from OfferingResource link where link.service.id = :serviceId")
    void deleteByServiceId(@Param("serviceId") Long serviceId);
}
