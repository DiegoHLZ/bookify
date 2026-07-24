package com.bookify.backend.business.repository;

import com.bookify.backend.business.model.ServiceOffering;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, Long> {

    List<ServiceOffering> findByBusinessIdAndActiveTrue(Long businessId);

    List<ServiceOffering> findByBusinessIdAndActiveTrueOrderByNameAscIdAsc(Long businessId);

    Optional<ServiceOffering> findByIdAndBusinessId(Long id, Long businessId);
}
