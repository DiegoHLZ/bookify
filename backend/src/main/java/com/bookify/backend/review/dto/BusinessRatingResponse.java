package com.bookify.backend.review.dto;

import java.math.BigDecimal;

public record BusinessRatingResponse(
        Long businessId,
        BigDecimal average,
        Integer count
) {
}
