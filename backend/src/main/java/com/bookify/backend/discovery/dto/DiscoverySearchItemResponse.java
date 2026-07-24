package com.bookify.backend.discovery.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DiscoverySearchItemResponse(
        Long businessId,
        String businessName,
        String categoryCode,
        BigDecimal ratingAverage,
        Integer ratingCount,
        Long locationId,
        String locationName,
        String address,
        String city,
        String countryCode,
        String timezone,
        BigDecimal latitude,
        BigDecimal longitude,
        double distanceMeters,
        LocalDateTime requestedAt,
        List<Long> availableServiceIds
) {
}
