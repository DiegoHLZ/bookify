package com.bookify.backend.availability.dto;

import java.time.LocalDate;
import java.util.List;

public record ServiceAvailabilityResponse(
        Long businessId,
        Long locationId,
        Long serviceId,
        Integer durationMinutes,
        Integer intervalMinutes,
        String timezone,
        LocalDate from,
        LocalDate to,
        List<AvailabilitySlotResponse> slots
) {
}
