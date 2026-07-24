package com.bookify.backend.discovery.dto;

import java.math.BigDecimal;
import java.util.List;

public record PublicBusinessDetailResponse(
        Long id,
        String slug,
        String name,
        String description,
        String categoryCode,
        String phone,
        String email,
        BigDecimal ratingAverage,
        Integer ratingCount,
        List<PublicLocationResponse> locations,
        List<PublicServiceResponse> services
) {
}
