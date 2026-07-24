package com.bookify.backend.resource.repository;

import com.bookify.backend.resource.model.BookableResource;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookableResourceRepository extends JpaRepository<BookableResource, Long> {
    List<BookableResource> findByBusinessIdAndLocationIdOrderByNameAsc(Long businessId, Long locationId);
    Optional<BookableResource> findByIdAndBusinessIdAndLocationId(Long id, Long businessId, Long locationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<BookableResource> findForUpdateByIdAndBusinessIdAndLocationId(
            Long id,
            Long businessId,
            Long locationId
    );
    boolean existsByLocationIdAndNameIgnoreCase(Long locationId, String name);
    boolean existsByLocationIdAndNameIgnoreCaseAndIdNot(Long locationId, String name, Long id);
    List<BookableResource> findAllByIdInAndBusinessIdAndActiveTrueAndLocationActiveTrue(
            Collection<Long> ids,
            Long businessId
    );
}
