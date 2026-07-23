package com.bookify.backend.location.repository;

import com.bookify.backend.location.model.BusinessLocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessLocationRepository extends JpaRepository<BusinessLocation, Long> {
}
