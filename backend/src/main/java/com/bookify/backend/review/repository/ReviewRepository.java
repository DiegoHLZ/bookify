package com.bookify.backend.review.repository;

import com.bookify.backend.review.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByBookingId(Long bookingId);
    List<Review> findByBusinessIdOrderByCreatedAtDescIdDesc(Long businessId);
}
