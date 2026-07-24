package com.bookify.backend.booking.controller;

import com.bookify.backend.booking.dto.BookingResponse;
import com.bookify.backend.booking.dto.BookingStatusHistoryResponse;
import com.bookify.backend.booking.dto.BookingStatusRequest;
import com.bookify.backend.booking.dto.CreateBookingRequest;
import com.bookify.backend.booking.service.BookingService;
import com.bookify.backend.common.SecurityUtils;
import com.bookify.backend.tenancy.service.BusinessAccessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class BookingController {

    private final BookingService bookingService;
    private final BusinessAccessService accessService;

    public BookingController(
            BookingService bookingService,
            BusinessAccessService accessService
    ) {
        this.bookingService = bookingService;
        this.accessService = accessService;
    }

    @PostMapping("/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateBookingRequest request
    ) {
        return bookingService.create(
                SecurityUtils.getCurrentUserEmail(), idempotencyKey, request
        );
    }

    @GetMapping("/bookings")
    public List<BookingResponse> findMine() {
        return bookingService.findMine(SecurityUtils.getCurrentUserEmail());
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    public BookingResponse cancel(@PathVariable Long bookingId) {
        return bookingService.cancel(bookingId, SecurityUtils.getCurrentUserEmail());
    }

    @GetMapping("/businesses/{businessId}/bookings")
    public List<BookingResponse> findForBusiness(@PathVariable Long businessId) {
        accessService.requireMembership(
                businessId, SecurityUtils.getCurrentUserEmail()
        );
        return bookingService.findForBusiness(businessId);
    }

    @PatchMapping("/businesses/{businessId}/bookings/{bookingId}/status")
    public BookingResponse changeStatus(
            @PathVariable Long businessId,
            @PathVariable Long bookingId,
            @Valid @RequestBody BookingStatusRequest request
    ) {
        accessService.requireMembership(
                businessId, SecurityUtils.getCurrentUserEmail()
        );
        return bookingService.changeStatus(
                businessId,
                bookingId,
                request.status(),
                request.reason(),
                SecurityUtils.getCurrentUserEmail()
        );
    }

    @GetMapping("/businesses/{businessId}/bookings/{bookingId}/history")
    public List<BookingStatusHistoryResponse> findHistory(
            @PathVariable Long businessId,
            @PathVariable Long bookingId
    ) {
        accessService.requireMembership(
                businessId, SecurityUtils.getCurrentUserEmail()
        );
        return bookingService.findHistory(businessId, bookingId);
    }
}
