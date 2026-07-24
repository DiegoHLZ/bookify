package com.bookify.backend.booking.dto;

import com.bookify.backend.booking.model.Booking;
import com.bookify.backend.booking.model.BookingStatus;
import com.bookify.backend.resource.model.ResourceType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public record BookingResponse(
        Long id,
        Long businessId,
        Long locationId,
        Long serviceId,
        String serviceName,
        Long resourceId,
        String resourceName,
        ResourceType resourceType,
        Long capacitySessionId,
        Long customerId,
        String customerEmail,
        Instant startsAt,
        Instant endsAt,
        LocalDateTime localStart,
        LocalDateTime localEnd,
        String timezone,
        BookingStatus status,
        Integer quantity,
        String notes,
        Instant cancelledAt,
        Integer rescheduleCount,
        Instant lastRescheduledAt,
        boolean cancellationAllowed,
        Integer cancellationNoticeMinutes,
        boolean rescheduleAllowed,
        Integer rescheduleNoticeMinutes,
        Integer maxReschedules,
        Instant createdAt,
        Instant updatedAt
) {
    public static BookingResponse from(Booking booking) {
        ZoneId zone = ZoneId.of(booking.getLocation().getTimezone());
        return new BookingResponse(
                booking.getId(),
                booking.getBusiness().getId(),
                booking.getLocation().getId(),
                booking.getService().getId(),
                booking.getService().getName(),
                booking.getResource().getId(),
                booking.getResource().getName(),
                booking.getResource().getType(),
                booking.getCapacitySession() == null
                        ? null : booking.getCapacitySession().getId(),
                booking.getCustomer().getId(),
                booking.getCustomer().getEmail(),
                booking.getStartsAt(),
                booking.getEndsAt(),
                LocalDateTime.ofInstant(booking.getStartsAt(), zone),
                LocalDateTime.ofInstant(booking.getEndsAt(), zone),
                zone.getId(),
                booking.getStatus(),
                booking.getQuantity(),
                booking.getNotes(),
                booking.getCancelledAt(),
                booking.getRescheduleCount(),
                booking.getLastRescheduledAt(),
                booking.isCancellationAllowedSnapshot(),
                booking.getCancellationNoticeSnapshot(),
                booking.isRescheduleAllowedSnapshot(),
                booking.getRescheduleNoticeSnapshot(),
                booking.getMaxReschedulesSnapshot(),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }
}
