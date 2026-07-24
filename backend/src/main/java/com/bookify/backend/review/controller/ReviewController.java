package com.bookify.backend.review.controller;

import com.bookify.backend.common.SecurityUtils;
import com.bookify.backend.review.dto.BusinessRatingResponse;
import com.bookify.backend.review.dto.CreateReviewRequest;
import com.bookify.backend.review.dto.ReviewResponse;
import com.bookify.backend.review.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/bookings/{bookingId}/review")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(
            @PathVariable Long bookingId,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        return reviewService.create(
                bookingId, SecurityUtils.getCurrentUserEmail(), request
        );
    }

    @GetMapping("/businesses/{businessId}/reviews")
    public List<ReviewResponse> findByBusiness(@PathVariable Long businessId) {
        return reviewService.findByBusiness(businessId);
    }

    @GetMapping("/businesses/{businessId}/rating")
    public BusinessRatingResponse rating(@PathVariable Long businessId) {
        return reviewService.rating(businessId);
    }
}
