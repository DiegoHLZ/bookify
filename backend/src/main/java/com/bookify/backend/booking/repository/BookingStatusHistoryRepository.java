package com.bookify.backend.booking.repository;

import com.bookify.backend.booking.model.BookingStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingStatusHistoryRepository
        extends JpaRepository<BookingStatusHistory, Long> {

    List<BookingStatusHistory> findByBusinessIdAndBookingIdOrderByCreatedAtAscIdAsc(
            Long businessId,
            Long bookingId
    );
}
