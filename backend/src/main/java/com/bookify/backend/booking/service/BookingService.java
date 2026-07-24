package com.bookify.backend.booking.service;

import com.bookify.backend.availability.dto.AvailabilitySlotResponse;
import com.bookify.backend.availability.dto.ServiceAvailabilityResponse;
import com.bookify.backend.availability.service.AvailabilitySlotService;
import com.bookify.backend.booking.dto.BookingResponse;
import com.bookify.backend.booking.dto.CreateBookingRequest;
import com.bookify.backend.booking.model.Booking;
import com.bookify.backend.booking.model.BookingStatus;
import com.bookify.backend.booking.repository.BookingRepository;
import com.bookify.backend.business.model.ServiceOffering;
import com.bookify.backend.business.repository.OfferingLocationRepository;
import com.bookify.backend.business.repository.ServiceOfferingRepository;
import com.bookify.backend.common.exception.BadRequestException;
import com.bookify.backend.common.exception.BookingConflictException;
import com.bookify.backend.common.exception.ResourceNotFoundException;
import com.bookify.backend.location.model.BusinessLocation;
import com.bookify.backend.location.repository.BusinessLocationRepository;
import com.bookify.backend.resource.model.BookableResource;
import com.bookify.backend.resource.repository.BookableResourceRepository;
import com.bookify.backend.resource.repository.OfferingResourceRepository;
import com.bookify.backend.user.model.User;
import com.bookify.backend.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;

@Service
public class BookingService {

    private static final int BOOKING_INTERVAL_MINUTES = 5;
    private static final EnumSet<BookingStatus> ACTIVE_STATUSES =
            EnumSet.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ServiceOfferingRepository serviceRepository;
    private final BusinessLocationRepository locationRepository;
    private final BookableResourceRepository resourceRepository;
    private final OfferingLocationRepository offeringLocationRepository;
    private final OfferingResourceRepository offeringResourceRepository;
    private final AvailabilitySlotService availabilityService;

    public BookingService(
            BookingRepository bookingRepository,
            UserRepository userRepository,
            ServiceOfferingRepository serviceRepository,
            BusinessLocationRepository locationRepository,
            BookableResourceRepository resourceRepository,
            OfferingLocationRepository offeringLocationRepository,
            OfferingResourceRepository offeringResourceRepository,
            AvailabilitySlotService availabilityService
    ) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.serviceRepository = serviceRepository;
        this.locationRepository = locationRepository;
        this.resourceRepository = resourceRepository;
        this.offeringLocationRepository = offeringLocationRepository;
        this.offeringResourceRepository = offeringResourceRepository;
        this.availabilityService = availabilityService;
    }

    @Transactional
    public BookingResponse create(
            String customerEmail,
            String idempotencyKey,
            CreateBookingRequest request
    ) {
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        User customer = userRepository.findForUpdateByEmailIgnoreCase(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        Booking existing = bookingRepository
                .findByCustomerIdAndIdempotencyKey(customer.getId(), normalizedKey)
                .orElse(null);
        if (existing != null) {
            return BookingResponse.from(existing);
        }
        if (!request.startsAt().isAfter(Instant.now())) {
            throw new BadRequestException("Booking start must be in the future");
        }

        BusinessLocation location = locationRepository
                .findByIdAndBusinessId(request.locationId(), request.businessId())
                .orElseThrow(() -> new ResourceNotFoundException("Business location not found"));
        ServiceOffering service = serviceRepository
                .findByIdAndBusinessId(request.serviceId(), request.businessId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        BookableResource resource = resourceRepository
                .findForUpdateByIdAndBusinessIdAndLocationId(
                        request.resourceId(), request.businessId(), request.locationId()
                )
                .orElseThrow(() -> new ResourceNotFoundException("Bookable resource not found"));
        validateRelationships(request, location, service, resource);

        Instant endsAt = request.startsAt().plus(
                service.getDurationMinutes(), ChronoUnit.MINUTES
        );
        if (bookingRepository.existsActiveOverlap(
                resource.getId(), ACTIVE_STATUSES, request.startsAt(), endsAt
        )) {
            throw new BookingConflictException("Resource is already booked");
        }
        requireGeneratedSlot(request, location, endsAt);

        Booking booking = new Booking(
                service.getBusiness(),
                location,
                service,
                resource,
                customer,
                request.startsAt(),
                endsAt,
                trimToNull(request.notes()),
                normalizedKey
        );
        try {
            return BookingResponse.from(bookingRepository.saveAndFlush(booking));
        } catch (DataIntegrityViolationException exception) {
            throw new BookingConflictException("Resource is already booked");
        }
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> findMine(String customerEmail) {
        User customer = requireUser(customerEmail);
        return bookingRepository.findByCustomerIdOrderByStartsAtDesc(customer.getId())
                .stream()
                .map(BookingResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> findForBusiness(Long businessId) {
        return bookingRepository.findByBusinessIdOrderByStartsAtDesc(businessId)
                .stream()
                .map(BookingResponse::from)
                .toList();
    }

    @Transactional
    public BookingResponse cancel(Long bookingId, String customerEmail) {
        User customer = userRepository.findForUpdateByEmailIgnoreCase(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        Booking booking = bookingRepository.findByIdAndCustomerId(bookingId, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        try {
            booking.cancel(Instant.now());
        } catch (IllegalStateException exception) {
            throw new BadRequestException(exception.getMessage());
        }
        return BookingResponse.from(bookingRepository.saveAndFlush(booking));
    }

    private void validateRelationships(
            CreateBookingRequest request,
            BusinessLocation location,
            ServiceOffering service,
            BookableResource resource
    ) {
        if (!location.isActive() || !service.isActive() || !resource.isActive()) {
            throw new BadRequestException("Location, service and resource must be active");
        }
        if (!offeringLocationRepository.existsByBusinessIdAndServiceIdAndLocationId(
                request.businessId(), request.serviceId(), request.locationId()
        )) {
            throw new ResourceNotFoundException("Service is not offered at this location");
        }
        if (!offeringResourceRepository
                .existsByBusinessIdAndServiceIdAndLocationIdAndResourceId(
                        request.businessId(),
                        request.serviceId(),
                        request.locationId(),
                        request.resourceId()
                )) {
            throw new ResourceNotFoundException("Resource is not assigned to this service");
        }
    }

    private void requireGeneratedSlot(
            CreateBookingRequest request,
            BusinessLocation location,
            Instant endsAt
    ) {
        ZoneId zone = ZoneId.of(location.getTimezone());
        LocalDate localDate = request.startsAt().atZone(zone).toLocalDate();
        ServiceAvailabilityResponse availability = availabilityService.findAvailability(
                request.businessId(),
                request.locationId(),
                request.serviceId(),
                localDate,
                localDate,
                BOOKING_INTERVAL_MINUTES
        );
        boolean exists = availability.slots().stream().anyMatch(slot ->
                slot.resourceId().equals(request.resourceId())
                        && slot.startAt().equals(request.startsAt())
                        && slot.endAt().equals(endsAt)
        );
        if (!exists) {
            throw new BookingConflictException("Requested time is not available");
        }
    }

    private User requireUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    private String normalizeIdempotencyKey(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Idempotency-Key header is required");
        }
        String normalized = value.trim();
        if (normalized.length() > 100) {
            throw new BadRequestException("Idempotency-Key must not exceed 100 characters");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
