package com.bookify.backend.capacity.dto;

import com.bookify.backend.capacity.model.CapacitySession;
import com.bookify.backend.capacity.model.CapacitySessionStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public record CapacitySessionResponse(
        Long id, Long businessId, Long locationId, Long serviceId, Long resourceId,
        Instant startsAt, Instant endsAt, LocalDateTime localStart, LocalDateTime localEnd,
        String timezone, Integer capacityTotal, Integer capacityReserved,
        Integer remainingCapacity, CapacitySessionStatus status
) {
    public static CapacitySessionResponse from(CapacitySession session) {
        ZoneId zone = ZoneId.of(session.getLocation().getTimezone());
        return new CapacitySessionResponse(
                session.getId(), session.getBusiness().getId(), session.getLocation().getId(),
                session.getService().getId(), session.getResource().getId(),
                session.getStartsAt(), session.getEndsAt(),
                LocalDateTime.ofInstant(session.getStartsAt(), zone),
                LocalDateTime.ofInstant(session.getEndsAt(), zone), zone.getId(),
                session.getCapacityTotal(), session.getCapacityReserved(),
                session.getRemainingCapacity(), session.getStatus()
        );
    }
}
