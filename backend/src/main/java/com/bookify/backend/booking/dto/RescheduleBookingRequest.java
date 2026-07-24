package com.bookify.backend.booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record RescheduleBookingRequest(
        @NotNull Long resourceId,
        @NotNull @Future Instant startsAt,
        Long capacitySessionId
) {
}
