package com.bookify.backend.business.onboarding.dto;

import com.bookify.backend.location.model.BusinessLocation;

import java.math.BigDecimal;

public record LocationResponse(
        Long id,
        String name,
        String address,
        String city,
        String countryCode,
        String timezone,
        BigDecimal latitude,
        BigDecimal longitude
) {
    public static LocationResponse from(BusinessLocation location) {
        return new LocationResponse(
                location.getId(),
                location.getName(),
                location.getAddress(),
                location.getCity(),
                location.getCountryCode(),
                location.getTimezone(),
                location.getLatitude(),
                location.getLongitude()
        );
    }
}
