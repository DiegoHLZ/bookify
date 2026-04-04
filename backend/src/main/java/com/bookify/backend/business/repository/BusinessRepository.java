package com.bookify.backend.business.repository;

import com.bookify.backend.business.model.Business;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusinessRepository extends JpaRepository<Business, Long> {
    Optional<Business> findBySlug(String slug);
}
