package com.bookify.backend.availability.dto;

import com.bookify.backend.availability.model.ResourceScheduleRule;
import com.bookify.backend.availability.model.ScheduleRuleType;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ScheduleRuleResponse(
        Long id,
        DayOfWeek dayOfWeek,
        ScheduleRuleType ruleType,
        LocalTime startTime,
        LocalTime endTime
) {
    public static ScheduleRuleResponse from(ResourceScheduleRule rule) {
        return new ScheduleRuleResponse(
                rule.getId(),
                rule.getDayOfWeek(),
                rule.getRuleType(),
                rule.getStartTime(),
                rule.getEndTime()
        );
    }
}
