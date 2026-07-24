package com.bookify.backend.availability.dto;

import java.util.List;

public record ResourceScheduleResponse(
        Long businessId,
        Long locationId,
        Long resourceId,
        String timezone,
        List<ScheduleRuleResponse> rules
) {
}
