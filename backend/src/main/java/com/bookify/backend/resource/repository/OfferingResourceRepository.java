package com.bookify.backend.resource.repository;

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

    @Modifying
    @Query("delete from OfferingResource link where link.service.id = :serviceId")
    void deleteByServiceId(@Param("serviceId") Long serviceId);
}
