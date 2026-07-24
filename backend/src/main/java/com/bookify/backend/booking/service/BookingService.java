package com.bookify.backend.booking.service;

import com.bookify.backend.availability.dto.AvailabilitySlotResponse;
import com.bookify.backend.availability.dto.ServiceAvailabilityResponse;
import com.bookify.backend.availability.service.AvailabilitySlotService;
import com.bookify.backend.booking.dto.BookingResponse;
import com.bookify.backend.booking.dto.BookingStatusHistoryResponse;
import com.bookify.backend.booking.dto.CreateBookingRequest;
import com.bookify.backend.booking.dto.RescheduleBookingRequest;
import com.bookify.backend.booking.model.Booking;
import com.bookify.backend.booking.model.BookingStatus;
import com.bookify.backend.booking.model.BookingStatusHistory;
import com.bookify.backend.booking.repository.BookingRepository;
import com.bookify.backend.booking.repository.BookingStatusHistoryRepository;
import com.bookify.backend.business.model.ServiceOffering;
import com.bookify.backend.business.model.BookingMode;
import com.bookify.backend.business.repository.OfferingLocationRepository;
import com.bookify.backend.business.repository.ServiceOfferingRepository;
import com.bookify.backend.common.exception.BadRequestException;
import com.bookify.backend.common.exception.BookingConflictException;
import com.bookify.backend.common.exception.ResourceNotFoundException;
import com.bookify.backend.capacity.model.CapacitySession;
import com.bookify.backend.capacity.model.CapacitySessionStatus;
import com.bookify.backend.capacity.repository.CapacitySessionRepository;
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
    private final BookingStatusHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final ServiceOfferingRepository serviceRepository;
    private final BusinessLocationRepository locationRepository;
    private final BookableResourceRepository resourceRepository;
    private final OfferingLocationRepository offeringLocationRepository;
    private final OfferingResourceRepository offeringResourceRepository;
    private final AvailabilitySlotService availabilityService;
    private final CapacitySessionRepository capacitySessionRepository;

    public BookingService(
            BookingRepository bookingRepository,
            BookingStatusHistoryRepository historyRepository,
            UserRepository userRepository,
            ServiceOfferingRepository serviceRepository,
            BusinessLocationRepository locationRepository,
            BookableResourceRepository resourceRepository,
            OfferingLocationRepository offeringLocationRepository,
            OfferingResourceRepository offeringResourceRepository,
            AvailabilitySlotService availabilityService,
            CapacitySessionRepository capacitySessionRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.serviceRepository = serviceRepository;
        this.locationRepository = locationRepository;
        this.resourceRepository = resourceRepository;
        this.offeringLocationRepository = offeringLocationRepository;
        this.offeringResourceRepository = offeringResourceRepository;
        this.availabilityService = availabilityService;
        this.capacitySessionRepository = capacitySessionRepository;
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

        Booking booking = service.getBookingMode() == BookingMode.CAPACITY_SESSION
                ? createCapacityBooking(
                        request, service, location, resource, customer, normalizedKey
                )
                : createExclusiveBooking(
                        request, service, location, resource, customer, normalizedKey
                );
        try {
            Booking saved = bookingRepository.saveAndFlush(booking);
            historyRepository.save(new BookingStatusHistory(
                    saved,
                    customer,
                    null,
                    BookingStatus.CONFIRMED,
                    "Created by customer"
            ));
            return BookingResponse.from(saved);
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
        Booking booking = bookingRepository
                .findForUpdateByIdAndCustomerId(bookingId, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        BookingStatus previous = booking.getStatus();
        requireCustomerCancellationAllowed(booking, Instant.now());
        try {
            booking.cancel(Instant.now());
        } catch (IllegalStateException exception) {
            throw new BadRequestException(exception.getMessage());
        }
        Booking saved = bookingRepository.saveAndFlush(booking);
        if (previous != BookingStatus.CANCELLED) {
            releaseCapacity(saved);
            historyRepository.save(new BookingStatusHistory(
                    saved,
                    customer,
                    previous,
                    BookingStatus.CANCELLED,
                    "Cancelled by customer"
            ));
        }
        return BookingResponse.from(saved);
    }

    @Transactional
    public BookingResponse reschedule(
            Long bookingId,
            String customerEmail,
            RescheduleBookingRequest request
    ) {
        User customer = userRepository.findForUpdateByEmailIgnoreCase(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        Booking booking = bookingRepository
                .findForUpdateByIdAndCustomerId(bookingId, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (sameTarget(booking, request)) {
            return BookingResponse.from(booking);
        }
        Instant now = Instant.now();
        requireCustomerRescheduleAllowed(booking, now);

        BookableResource resource = resourceRepository
                .findForUpdateByIdAndBusinessIdAndLocationId(
                        request.resourceId(),
                        booking.getBusiness().getId(),
                        booking.getLocation().getId()
                )
                .orElseThrow(() -> new ResourceNotFoundException("Bookable resource not found"));
        if (!resource.isActive() || !offeringResourceRepository
                .existsByBusinessIdAndServiceIdAndLocationIdAndResourceId(
                        booking.getBusiness().getId(),
                        booking.getService().getId(),
                        booking.getLocation().getId(),
                        resource.getId()
                )) {
            throw new ResourceNotFoundException("Resource is not assigned to this service");
        }

        Instant oldStart = booking.getStartsAt();
        Long oldResourceId = booking.getResource().getId();
        CapacitySession oldSession = booking.getCapacitySession();
        CapacitySession newSession = null;
        Instant newEnd;

        if (booking.getService().getBookingMode() == BookingMode.CAPACITY_SESSION) {
            if (request.capacitySessionId() == null) {
                throw new BadRequestException("Capacity session id is required");
            }
            newSession = capacitySessionRepository
                    .findForUpdateByIdAndBusinessId(
                            request.capacitySessionId(), booking.getBusiness().getId()
                    )
                    .orElseThrow(() -> new ResourceNotFoundException("Capacity session not found"));
            boolean matches = newSession.getLocation().getId().equals(booking.getLocation().getId())
                    && newSession.getService().getId().equals(booking.getService().getId())
                    && newSession.getResource().getId().equals(resource.getId())
                    && newSession.getStartsAt().equals(request.startsAt())
                    && newSession.getStatus() == CapacitySessionStatus.OPEN;
            if (!matches) {
                throw new BookingConflictException("Capacity session does not match request");
            }
            try {
                newSession.reserve(booking.getQuantity());
            } catch (IllegalStateException exception) {
                throw new BookingConflictException(exception.getMessage());
            }
            newEnd = newSession.getEndsAt();
        } else {
            if (request.capacitySessionId() != null) {
                throw new BadRequestException(
                        "Exclusive-resource rescheduling does not accept a capacity session"
                );
            }
            newEnd = request.startsAt().plus(
                    booking.getService().getDurationMinutes(), ChronoUnit.MINUTES
            );
            if (bookingRepository.existsActiveOverlapExcluding(
                    resource.getId(), booking.getId(), ACTIVE_STATUSES,
                    request.startsAt(), newEnd
            )) {
                throw new BookingConflictException("Resource is already booked");
            }
            requireGeneratedSlot(new CreateBookingRequest(
                    booking.getBusiness().getId(),
                    booking.getLocation().getId(),
                    booking.getService().getId(),
                    resource.getId(),
                    request.startsAt(),
                    null,
                    1,
                    booking.getNotes()
            ), booking.getLocation(), newEnd);
        }

        if (oldSession != null) {
            CapacitySession lockedOld = capacitySessionRepository
                    .findForUpdateByIdAndBusinessId(
                            oldSession.getId(), booking.getBusiness().getId()
                    )
                    .orElseThrow(() -> new ResourceNotFoundException("Capacity session not found"));
            lockedOld.release(booking.getQuantity());
        }
        booking.reschedule(resource, newSession, request.startsAt(), newEnd, now);
        Booking saved = bookingRepository.saveAndFlush(booking);
        historyRepository.save(new BookingStatusHistory(
                saved,
                customer,
                saved.getStatus(),
                saved.getStatus(),
                "Rescheduled from " + oldStart + " (resource " + oldResourceId
                        + ") to " + request.startsAt() + " (resource " + resource.getId() + ")"
        ));
        return BookingResponse.from(saved);
    }

    @Transactional
    public BookingResponse changeStatus(
            Long businessId,
            Long bookingId,
            BookingStatus target,
            String reason,
            String actorEmail
    ) {
        String normalizedReason = trimToNull(reason);
        if ((target == BookingStatus.CANCELLED || target == BookingStatus.REJECTED)
                && normalizedReason == null) {
            throw new BadRequestException(
                    "Reason is required when cancelling or rejecting a booking"
            );
        }
        User actor = requireUser(actorEmail);
        Booking booking = bookingRepository
                .findForUpdateByIdAndBusinessId(bookingId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        BookingStatus previous;
        try {
            previous = booking.transitionTo(target, Instant.now());
        } catch (IllegalStateException exception) {
            throw new BadRequestException(exception.getMessage());
        }
        Booking saved = bookingRepository.saveAndFlush(booking);
        if (target == BookingStatus.CANCELLED || target == BookingStatus.REJECTED) {
            releaseCapacity(saved);
        }
        historyRepository.save(new BookingStatusHistory(
                saved, actor, previous, target, normalizedReason
        ));
        return BookingResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<BookingStatusHistoryResponse> findHistory(
            Long businessId,
            Long bookingId
    ) {
        bookingRepository.findByIdAndBusinessId(bookingId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        return historyRepository
                .findByBusinessIdAndBookingIdOrderByCreatedAtAscIdAsc(
                        businessId, bookingId
                )
                .stream()
                .map(BookingStatusHistoryResponse::from)
                .toList();
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

    private Booking createExclusiveBooking(
            CreateBookingRequest request,
            ServiceOffering service,
            BusinessLocation location,
            BookableResource resource,
            User customer,
            String idempotencyKey
    ) {
        if (request.capacitySessionId() != null
                || (request.quantity() != null && request.quantity() != 1)) {
            throw new BadRequestException(
                    "Exclusive-resource bookings require quantity 1 and no capacity session"
            );
        }
        Instant endsAt = request.startsAt().plus(
                service.getDurationMinutes(), ChronoUnit.MINUTES
        );
        if (bookingRepository.existsActiveOverlap(
                resource.getId(), ACTIVE_STATUSES, request.startsAt(), endsAt
        )) {
            throw new BookingConflictException("Resource is already booked");
        }
        requireGeneratedSlot(request, location, endsAt);
        return new Booking(
                service.getBusiness(), location, service, resource, customer,
                request.startsAt(), endsAt, trimToNull(request.notes()), idempotencyKey
        );
    }

    private Booking createCapacityBooking(
            CreateBookingRequest request,
            ServiceOffering service,
            BusinessLocation location,
            BookableResource resource,
            User customer,
            String idempotencyKey
    ) {
        if (request.capacitySessionId() == null) {
            throw new BadRequestException("Capacity session id is required");
        }
        int quantity = request.quantity() == null ? 1 : request.quantity();
        CapacitySession session = capacitySessionRepository
                .findForUpdateByIdAndBusinessId(request.capacitySessionId(), request.businessId())
                .orElseThrow(() -> new ResourceNotFoundException("Capacity session not found"));
        boolean matches = session.getLocation().getId().equals(location.getId())
                && session.getService().getId().equals(service.getId())
                && session.getResource().getId().equals(resource.getId())
                && session.getStartsAt().equals(request.startsAt())
                && session.getStatus() == CapacitySessionStatus.OPEN;
        if (!matches) {
            throw new BookingConflictException("Capacity session does not match request");
        }
        try {
            session.reserve(quantity);
        } catch (IllegalStateException exception) {
            throw new BookingConflictException(exception.getMessage());
        }
        capacitySessionRepository.saveAndFlush(session);
        return new Booking(
                service.getBusiness(), location, service, resource, session, customer,
                quantity, trimToNull(request.notes()), idempotencyKey
        );
    }

    private void releaseCapacity(Booking booking) {
        if (booking.getCapacitySession() == null) {
            return;
        }
        CapacitySession session = capacitySessionRepository
                .findForUpdateByIdAndBusinessId(
                        booking.getCapacitySession().getId(),
                        booking.getBusiness().getId()
                )
                .orElseThrow(() -> new ResourceNotFoundException("Capacity session not found"));
        session.release(booking.getQuantity());
        capacitySessionRepository.saveAndFlush(session);
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

    private void requireCustomerCancellationAllowed(Booking booking, Instant now) {
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return;
        }
        if (!booking.isCancellationAllowedSnapshot()) {
            throw new BadRequestException("Customer cancellation is disabled for this service");
        }
        Instant deadline = booking.getStartsAt().minus(
                booking.getCancellationNoticeSnapshot(), ChronoUnit.MINUTES
        );
        if (now.isAfter(deadline)) {
            throw new BadRequestException("Cancellation notice period has expired");
        }
    }

    private void requireCustomerRescheduleAllowed(Booking booking, Instant now) {
        if (booking.getStatus() != BookingStatus.PENDING
                && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Only active bookings can be rescheduled");
        }
        if (!booking.isRescheduleAllowedSnapshot()) {
            throw new BadRequestException("Customer rescheduling is disabled for this service");
        }
        if (booking.getRescheduleCount() >= booking.getMaxReschedulesSnapshot()) {
            throw new BadRequestException("Maximum number of reschedules has been reached");
        }
        Instant deadline = booking.getStartsAt().minus(
                booking.getRescheduleNoticeSnapshot(), ChronoUnit.MINUTES
        );
        if (now.isAfter(deadline)) {
            throw new BadRequestException("Reschedule notice period has expired");
        }
    }

    private boolean sameTarget(Booking booking, RescheduleBookingRequest request) {
        Long sessionId = booking.getCapacitySession() == null
                ? null : booking.getCapacitySession().getId();
        return booking.getResource().getId().equals(request.resourceId())
                && booking.getStartsAt().equals(request.startsAt())
                && java.util.Objects.equals(sessionId, request.capacitySessionId());
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
