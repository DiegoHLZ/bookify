package com.bookify.backend.discovery.dto;

import java.math.BigDecimal;

public record NearbyBusinessResponse(
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
        BigDecimal latitude,
        BigDecimal longitude,
        double distanceMeters
) {
}
