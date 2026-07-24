package com.bookify.backend.review.service;

import com.bookify.backend.booking.model.Booking;
import com.bookify.backend.booking.model.BookingStatus;
import com.bookify.backend.booking.repository.BookingRepository;
import com.bookify.backend.business.model.Business;
import com.bookify.backend.business.repository.BusinessRepository;
import com.bookify.backend.common.exception.BadRequestException;
import com.bookify.backend.common.exception.ResourceNotFoundException;
import com.bookify.backend.review.dto.BusinessRatingResponse;
import com.bookify.backend.review.dto.CreateReviewRequest;
import com.bookify.backend.review.dto.ReviewResponse;
import com.bookify.backend.review.model.Review;
import com.bookify.backend.review.repository.ReviewRepository;
import com.bookify.backend.user.model.User;
import com.bookify.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;

    public ReviewService(
            ReviewRepository reviewRepository,
            BookingRepository bookingRepository,
            BusinessRepository businessRepository,
            UserRepository userRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.businessRepository = businessRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReviewResponse create(
            Long bookingId,
            String customerEmail,
            CreateReviewRequest request
    ) {
        User customer = userRepository.findForUpdateByEmailIgnoreCase(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        Booking booking = bookingRepository
                .findForUpdateByIdAndCustomerId(bookingId, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BadRequestException("Only completed bookings can be reviewed");
        }
        if (reviewRepository.existsByBookingId(bookingId)) {
            throw new BadRequestException("Booking already has a review");
        }
        Business business = businessRepository.findByIdForUpdate(
                        booking.getBusiness().getId()
                )
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));
        Review review = reviewRepository.saveAndFlush(new Review(
                booking, trimToNull(request.comment()), request.score()
        ));
        business.addVerifiedRating(request.score());
        businessRepository.saveAndFlush(business);
        return ReviewResponse.from(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> findByBusiness(Long businessId) {
        requireBusiness(businessId);
        return reviewRepository.findByBusinessIdOrderByCreatedAtDescIdDesc(businessId)
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BusinessRatingResponse rating(Long businessId) {
        Business business = requireBusiness(businessId);
        return new BusinessRatingResponse(
                business.getId(), business.getRatingAverage(), business.getRatingCount()
        );
    }

    private Business requireBusiness(Long businessId) {
        return businessRepository.findById(businessId)
                .filter(Business::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
