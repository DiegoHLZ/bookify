package com.bookify.backend.resource.repository;

import com.bookify.backend.resource.model.BookableResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookableResourceRepository extends JpaRepository<BookableResource, Long> {
    List<BookableResource> findByBusinessIdAndLocationIdOrderByNameAsc(Long businessId, Long locationId);
    Optional<BookableResource> findByIdAndBusinessIdAndLocationId(Long id, Long businessId, Long locationId);
    boolean existsByLocationIdAndNameIgnoreCase(Long locationId, String name);
    boolean existsByLocationIdAndNameIgnoreCaseAndIdNot(Long locationId, String name, Long id);
    List<BookableResource> findAllByIdInAndBusinessIdAndActiveTrueAndLocationActiveTrue(
            Collection<Long> ids,
            Long businessId
    );
}
