package com.bookify.backend.discovery.dto;

import com.bookify.backend.business.model.BookingMode;

import java.math.BigDecimal;
import java.util.List;

public record PublicServiceResponse(
        Long id,
        String name,
        String description,
        Integer durationMinutes,
        BigDecimal price,
        String currency,
        BookingMode bookingMode,
        List<Long> locationIds
) {
}
