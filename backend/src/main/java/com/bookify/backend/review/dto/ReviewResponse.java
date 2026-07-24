package com.bookify.backend.review.dto;

import com.bookify.backend.review.model.Review;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        Long businessId,
        Long bookingId,
        Long customerId,
        String customerName,
        Integer score,
        String comment,
        boolean verified,
        Instant createdAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getBusiness().getId(),
                review.getBooking().getId(),
                review.getCustomer().getId(),
                review.getCustomer().getFirstName(),
                review.getScore(),
                review.getComment(),
                review.isVerified(),
                review.getCreatedAt()
        );
    }
}
