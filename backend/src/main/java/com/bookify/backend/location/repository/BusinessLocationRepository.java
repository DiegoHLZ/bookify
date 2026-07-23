package com.bookify.backend.location.repository;

import com.bookify.backend.location.model.BusinessLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BusinessLocationRepository extends JpaRepository<BusinessLocation, Long> {

    List<BusinessLocation> findByBusinessIdOrderByNameAsc(Long businessId);

    Optional<BusinessLocation> findByIdAndBusinessId(Long id, Long businessId);

    boolean existsByBusinessIdAndNameIgnoreCase(Long businessId, String name);

    boolean existsByBusinessIdAndNameIgnoreCaseAndIdNot(Long businessId, String name, Long id);

    long countByBusinessIdAndActiveTrue(Long businessId);
}
