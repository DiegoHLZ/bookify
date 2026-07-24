package com.bookify.backend.booking.dto;

import com.bookify.backend.booking.model.BookingStatus;
import com.bookify.backend.booking.model.BookingStatusHistory;

import java.time.Instant;

public record BookingStatusHistoryResponse(
        Long id,
        Long bookingId,
        Long actorId,
        String actorEmail,
        BookingStatus fromStatus,
        BookingStatus toStatus,
        String reason,
        Instant createdAt
) {
    public static BookingStatusHistoryResponse from(BookingStatusHistory history) {
        return new BookingStatusHistoryResponse(
                history.getId(),
                history.getBooking().getId(),
                history.getActor().getId(),
                history.getActor().getEmail(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getReason(),
                history.getCreatedAt()
        );
    }
}
