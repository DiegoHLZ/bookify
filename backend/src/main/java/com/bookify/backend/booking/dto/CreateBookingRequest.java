package com.bookify.backend.booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateBookingRequest(
        @NotNull Long businessId,
        @NotNull Long locationId,
        @NotNull Long serviceId,
        @NotNull Long resourceId,
        @NotNull @Future Instant startsAt,
        Long capacitySessionId,
        @Min(1) Integer quantity,
        @Size(max = 500) String notes
) {
}
