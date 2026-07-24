package com.bookify.backend.booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateBookingRequest(
        @NotNull Long businessId,
        @NotNull Long locationId,
        @NotNull Long serviceId,
        @NotNull Long resourceId,
        @NotNull @Future Instant startsAt,
        @Size(max = 500) String notes
) {
}
