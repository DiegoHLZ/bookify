package com.bookify.backend.availability.dto;

import com.bookify.backend.availability.model.ScheduleRuleType;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ScheduleRuleRequest(
        @NotNull DayOfWeek dayOfWeek,
        @NotNull ScheduleRuleType ruleType,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime
) {
}
