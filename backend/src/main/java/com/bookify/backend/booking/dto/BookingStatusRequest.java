package com.bookify.backend.booking.dto;

import com.bookify.backend.booking.model.BookingStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookingStatusRequest(
        @NotNull BookingStatus status,
        @Size(max = 500) String reason
) {
}
