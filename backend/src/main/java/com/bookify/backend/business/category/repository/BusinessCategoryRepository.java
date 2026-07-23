package com.bookify.backend.business.category.repository;

import com.bookify.backend.business.category.model.BusinessCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessCategoryRepository extends JpaRepository<BusinessCategory, String> {

    boolean existsByCodeAndActiveTrue(String code);
}
