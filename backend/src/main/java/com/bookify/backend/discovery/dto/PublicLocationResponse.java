package com.bookify.backend.discovery.dto;

import java.math.BigDecimal;

public record PublicLocationResponse(
        Long id,
        String name,
        String address,
        String city,
        String countryCode,
        String timezone,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
