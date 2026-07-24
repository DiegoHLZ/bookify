package com.bookify.backend.availability.repository;

import com.bookify.backend.availability.model.ResourceScheduleException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ResourceScheduleExceptionRepository
        extends JpaRepository<ResourceScheduleException, Long> {

    Optional<ResourceScheduleException> findByBusinessIdAndLocationIdAndResourceIdAndExceptionDate(
            Long businessId,
            Long locationId,
            Long resourceId,
            LocalDate exceptionDate
    );

    List<ResourceScheduleException>
    findByBusinessIdAndLocationIdAndResourceIdAndExceptionDateBetweenOrderByExceptionDateAsc(
            Long businessId,
            Long locationId,
            Long resourceId,
            LocalDate from,
            LocalDate to
    );
}
