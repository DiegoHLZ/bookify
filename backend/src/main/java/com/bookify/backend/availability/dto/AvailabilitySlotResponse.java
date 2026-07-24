package com.bookify.backend.availability.dto;

import com.bookify.backend.resource.model.ResourceType;

import java.time.Instant;
import java.time.LocalDateTime;

public record AvailabilitySlotResponse(
        Long resourceId,
        String resourceName,
        ResourceType resourceType,
        Long capacitySessionId,
        Integer remainingCapacity,
        LocalDateTime localStart,
        LocalDateTime localEnd,
        Instant startAt,
        Instant endAt
) {
}
