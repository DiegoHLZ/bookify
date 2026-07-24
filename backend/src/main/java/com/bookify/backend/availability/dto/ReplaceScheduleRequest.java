package com.bookify.backend.availability.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReplaceScheduleRequest(
        @NotNull List<@Valid ScheduleRuleRequest> rules
) {
}
