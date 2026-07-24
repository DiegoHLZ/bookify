package com.bookify.backend.availability.repository;

import com.bookify.backend.availability.model.ResourceScheduleRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface ResourceScheduleRuleRepository extends JpaRepository<ResourceScheduleRule, Long> {
    List<ResourceScheduleRule> findByBusinessIdAndLocationIdAndResourceIdOrderByDayOfWeekAscStartTimeAsc(
            Long businessId,
            Long locationId,
            Long resourceId
    );

    @Modifying
    void deleteByResourceId(Long resourceId);
}
