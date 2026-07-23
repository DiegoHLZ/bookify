package com.bookify.backend.location.dto;

import com.bookify.backend.location.model.BusinessLocation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BusinessLocationResponse(
        Long id,
        Long businessId,
        String name,
        String address,
        String city,
        String countryCode,
        String timezone,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static BusinessLocationResponse from(BusinessLocation location) {
        return new BusinessLocationResponse(
                location.getId(),
                location.getBusiness().getId(),
                location.getName(),
                location.getAddress(),
                location.getCity(),
                location.getCountryCode(),
                location.getTimezone(),
                location.getLatitude(),
                location.getLongitude(),
                location.isActive(),
                location.getCreatedAt(),
                location.getUpdatedAt()
        );
    }
}
