package com.bookify.backend.availability.service;

import com.bookify.backend.availability.dto.AvailabilitySlotResponse;
import com.bookify.backend.availability.dto.ServiceAvailabilityResponse;
import com.bookify.backend.availability.model.ResourceScheduleException;
import com.bookify.backend.availability.model.ResourceScheduleRule;
import com.bookify.backend.availability.model.ScheduleExceptionType;
import com.bookify.backend.availability.model.ScheduleRuleType;
import com.bookify.backend.availability.repository.ResourceScheduleExceptionRepository;
import com.bookify.backend.availability.repository.ResourceScheduleRuleRepository;
import com.bookify.backend.booking.model.Booking;
import com.bookify.backend.booking.model.BookingStatus;
import com.bookify.backend.booking.repository.BookingRepository;
import com.bookify.backend.business.model.ServiceOffering;
import com.bookify.backend.business.model.BookingMode;
import com.bookify.backend.business.repository.OfferingLocationRepository;
import com.bookify.backend.business.repository.ServiceOfferingRepository;
import com.bookify.backend.common.exception.BadRequestException;
import com.bookify.backend.common.exception.ResourceNotFoundException;
import com.bookify.backend.capacity.model.CapacitySessionStatus;
import com.bookify.backend.capacity.repository.CapacitySessionRepository;
import com.bookify.backend.location.model.BusinessLocation;
import com.bookify.backend.location.repository.BusinessLocationRepository;
import com.bookify.backend.resource.model.BookableResource;
import com.bookify.backend.resource.repository.OfferingResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AvailabilitySlotService {

    private static final long MAX_RANGE_DAYS = 30;
    private static final int MIN_INTERVAL_MINUTES = 5;
    private static final int MAX_INTERVAL_MINUTES = 1440;
    private static final int MAX_SLOTS = 10_000;
    private static final EnumSet<BookingStatus> ACTIVE_BOOKING_STATUSES =
            EnumSet.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    private final ServiceOfferingRepository serviceRepository;
    private final BusinessLocationRepository locationRepository;
    private final OfferingLocationRepository offeringLocationRepository;
    private final OfferingResourceRepository offeringResourceRepository;
    private final ResourceScheduleRuleRepository ruleRepository;
    private final ResourceScheduleExceptionRepository exceptionRepository;
    private final BookingRepository bookingRepository;
    private final CapacitySessionRepository capacitySessionRepository;

    public AvailabilitySlotService(
            ServiceOfferingRepository serviceRepository,
            BusinessLocationRepository locationRepository,
            OfferingLocationRepository offeringLocationRepository,
            OfferingResourceRepository offeringResourceRepository,
            ResourceScheduleRuleRepository ruleRepository,
            ResourceScheduleExceptionRepository exceptionRepository,
            BookingRepository bookingRepository,
            CapacitySessionRepository capacitySessionRepository
    ) {
        this.serviceRepository = serviceRepository;
        this.locationRepository = locationRepository;
        this.offeringLocationRepository = offeringLocationRepository;
        this.offeringResourceRepository = offeringResourceRepository;
        this.ruleRepository = ruleRepository;
        this.exceptionRepository = exceptionRepository;
        this.bookingRepository = bookingRepository;
        this.capacitySessionRepository = capacitySessionRepository;
    }

    @Transactional(readOnly = true)
    public ServiceAvailabilityResponse findAvailability(
            Long businessId,
            Long locationId,
            Long serviceId,
            LocalDate from,
            LocalDate to,
            Integer intervalMinutes
    ) {
        validateRange(from, to, intervalMinutes);
        BusinessLocation location = requireActiveLocation(businessId, locationId);
        ServiceOffering service = requireActiveService(businessId, serviceId);
        if (!offeringLocationRepository.existsByBusinessIdAndServiceIdAndLocationId(
                businessId, serviceId, locationId
        )) {
            throw new ResourceNotFoundException("Service is not offered at this location");
        }

        ZoneId zone = requireZone(location.getTimezone());
        if (service.getBookingMode() == BookingMode.CAPACITY_SESSION) {
            InstantRange range = queryRange(from, to, zone);
            List<AvailabilitySlotResponse> sessionSlots = capacitySessionRepository
                    .findByBusinessIdAndLocationIdAndServiceIdAndStatusAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                            businessId, locationId, serviceId, CapacitySessionStatus.OPEN,
                            range.start(), range.end()
                    )
                    .stream()
                    .filter(session -> session.getRemainingCapacity() > 0)
                    .map(session -> new AvailabilitySlotResponse(
                            session.getResource().getId(),
                            session.getResource().getName(),
                            session.getResource().getType(),
                            session.getId(),
                            session.getRemainingCapacity(),
                            LocalDateTime.ofInstant(session.getStartsAt(), zone),
                            LocalDateTime.ofInstant(session.getEndsAt(), zone),
                            session.getStartsAt(),
                            session.getEndsAt()
                    ))
                    .toList();
            return response(
                    businessId, locationId, service, intervalMinutes, zone, from, to, sessionSlots
            );
        }
        List<BookableResource> resources = offeringResourceRepository.findActiveResources(
                businessId, serviceId, locationId
        );
        if (resources.isEmpty()) {
            return response(
                    businessId, locationId, service, intervalMinutes, zone, from, to, List.of()
            );
        }

        List<Long> resourceIds = resources.stream().map(BookableResource::getId).toList();
        Map<Long, List<ResourceScheduleRule>> rulesByResource = ruleRepository
                .findByBusinessIdAndLocationIdAndResourceIdIn(
                        businessId, locationId, resourceIds
                )
                .stream()
                .collect(Collectors.groupingBy(rule -> rule.getResource().getId()));
        Map<Long, Map<LocalDate, ResourceScheduleException>> exceptionsByResource =
                exceptionRepository
                        .findByBusinessIdAndLocationIdAndResourceIdInAndExceptionDateBetween(
                                businessId, locationId, resourceIds, from, to
                        )
                        .stream()
                        .collect(Collectors.groupingBy(
                                exception -> exception.getResource().getId(),
                                Collectors.toMap(
                                        ResourceScheduleException::getExceptionDate,
                                        Function.identity()
                                )
                        ));
        InstantRange queryRange = queryRange(from, to, zone);
        Map<Long, List<Booking>> bookingsByResource = bookingRepository
                .findActiveOverlapping(
                        resourceIds,
                        ACTIVE_BOOKING_STATUSES,
                        queryRange.start(),
                        queryRange.end()
                )
                .stream()
                .collect(Collectors.groupingBy(booking -> booking.getResource().getId()));

        List<AvailabilitySlotResponse> slots = new ArrayList<>();
        for (BookableResource resource : resources) {
            generateForResource(
                    resource,
                    rulesByResource.getOrDefault(resource.getId(), List.of()),
                    exceptionsByResource.getOrDefault(resource.getId(), Map.of()),
                    bookingsByResource.getOrDefault(resource.getId(), List.of()),
                    from,
                    to,
                    service.getDurationMinutes(),
                    intervalMinutes,
                    zone,
                    slots
            );
        }
        slots.sort(Comparator
                .comparing(AvailabilitySlotResponse::startAt)
                .thenComparing(AvailabilitySlotResponse::resourceId));
        return response(
                businessId, locationId, service, intervalMinutes, zone, from, to, slots
        );
    }

    private void generateForResource(
            BookableResource resource,
            List<ResourceScheduleRule> rules,
            Map<LocalDate, ResourceScheduleException> exceptions,
            List<Booking> bookings,
            LocalDate from,
            LocalDate to,
            int durationMinutes,
            int intervalMinutes,
            ZoneId zone,
            List<AvailabilitySlotResponse> target
    ) {
        Map<java.time.DayOfWeek, List<ResourceScheduleRule>> rulesByDay = rules.stream()
                .collect(Collectors.groupingBy(ResourceScheduleRule::getDayOfWeek));
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            List<ResourceScheduleRule> dayRules =
                    rulesByDay.getOrDefault(date.getDayOfWeek(), List.of());
            List<TimeRange> breaks = dayRules.stream()
                    .filter(rule -> rule.getRuleType() == ScheduleRuleType.BREAK)
                    .map(rule -> new TimeRange(rule.getStartTime(), rule.getEndTime()))
                    .sorted(Comparator.comparing(TimeRange::start))
                    .toList();

            ResourceScheduleException exception = exceptions.get(date);
            List<TimeRange> available = availableRanges(dayRules, exception);
            for (TimeRange free : subtractBreaks(available, breaks)) {
                appendSlots(
                        resource,
                        date,
                        free,
                        durationMinutes,
                        intervalMinutes,
                        zone,
                        target,
                        bookings
                );
            }
        }
    }

    private List<TimeRange> availableRanges(
            List<ResourceScheduleRule> dayRules,
            ResourceScheduleException exception
    ) {
        if (exception != null) {
            if (exception.getExceptionType() == ScheduleExceptionType.CLOSED) {
                return List.of();
            }
            return List.of(new TimeRange(exception.getStartTime(), exception.getEndTime()));
        }
        return dayRules.stream()
                .filter(rule -> rule.getRuleType() == ScheduleRuleType.AVAILABLE)
                .map(rule -> new TimeRange(rule.getStartTime(), rule.getEndTime()))
                .sorted(Comparator.comparing(TimeRange::start))
                .toList();
    }

    private List<TimeRange> subtractBreaks(
            List<TimeRange> available,
            List<TimeRange> breaks
    ) {
        List<TimeRange> result = new ArrayList<>();
        for (TimeRange interval : available) {
            List<TimeRange> remaining = new ArrayList<>(List.of(interval));
            for (TimeRange breakRange : breaks) {
                List<TimeRange> next = new ArrayList<>();
                for (TimeRange current : remaining) {
                    if (!breakRange.start().isBefore(current.end())
                            || !breakRange.end().isAfter(current.start())) {
                        next.add(current);
                        continue;
                    }
                    if (breakRange.start().isAfter(current.start())) {
                        next.add(new TimeRange(
                                current.start(),
                                min(breakRange.start(), current.end())
                        ));
                    }
                    if (breakRange.end().isBefore(current.end())) {
                        next.add(new TimeRange(
                                max(breakRange.end(), current.start()),
                                current.end()
                        ));
                    }
                }
                remaining = next;
            }
            result.addAll(remaining);
        }
        return result;
    }

    private void appendSlots(
            BookableResource resource,
            LocalDate date,
            TimeRange free,
            int durationMinutes,
            int intervalMinutes,
            ZoneId zone,
            List<AvailabilitySlotResponse> target,
            List<Booking> bookings
    ) {
        LocalDateTime localBoundary = LocalDateTime.of(date, free.end());
        for (LocalDateTime localStart = LocalDateTime.of(date, free.start());
             localStart.isBefore(localBoundary);
             localStart = localStart.plusMinutes(intervalMinutes)) {
            List<ZoneOffset> offsets = zone.getRules().getValidOffsets(localStart);
            for (ZoneOffset offset : offsets) {
                ZonedDateTime zonedStart = ZonedDateTime.ofLocal(localStart, zone, offset);
                ZonedDateTime zonedEnd = zonedStart.plus(Duration.ofMinutes(durationMinutes));
                if (zonedEnd.toLocalDateTime().isAfter(localBoundary)) {
                    continue;
                }
                if (overlapsBooking(zonedStart, zonedEnd, bookings)) {
                    continue;
                }
                target.add(new AvailabilitySlotResponse(
                        resource.getId(),
                        resource.getName(),
                        resource.getType(),
                        null,
                        null,
                        localStart,
                        zonedEnd.toLocalDateTime(),
                        zonedStart.toInstant(),
                        zonedEnd.toInstant()
                ));
                if (target.size() > MAX_SLOTS) {
                    throw new BadRequestException(
                            "Availability query produces more than 10000 slots"
                    );
                }
            }
        }
    }

    private ServiceAvailabilityResponse response(
            Long businessId,
            Long locationId,
            ServiceOffering service,
            Integer intervalMinutes,
            ZoneId zone,
            LocalDate from,
            LocalDate to,
            List<AvailabilitySlotResponse> slots
    ) {
        return new ServiceAvailabilityResponse(
                businessId,
                locationId,
                service.getId(),
                service.getDurationMinutes(),
                intervalMinutes,
                zone.getId(),
                from,
                to,
                List.copyOf(slots)
        );
    }

    private boolean overlapsBooking(
            ZonedDateTime start,
            ZonedDateTime end,
            List<Booking> bookings
    ) {
        return bookings.stream().anyMatch(booking ->
                booking.getStartsAt().isBefore(end.toInstant())
                        && booking.getEndsAt().isAfter(start.toInstant())
        );
    }

    private InstantRange queryRange(LocalDate from, LocalDate to, ZoneId zone) {
        return new InstantRange(
                from.atStartOfDay(zone).toInstant(),
                to.plusDays(1).atStartOfDay(zone).toInstant()
        );
    }

    private void validateRange(LocalDate from, LocalDate to, Integer intervalMinutes) {
        if (from.isAfter(to)) {
            throw new BadRequestException("Availability range start must not be after end");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new BadRequestException("Availability range cannot exceed 31 days");
        }
        if (intervalMinutes == null
                || intervalMinutes < MIN_INTERVAL_MINUTES
                || intervalMinutes > MAX_INTERVAL_MINUTES) {
            throw new BadRequestException(
                    "Interval minutes must be between 5 and 1440"
            );
        }
    }

    private BusinessLocation requireActiveLocation(Long businessId, Long locationId) {
        BusinessLocation location = locationRepository.findByIdAndBusinessId(
                        locationId, businessId
                )
                .orElseThrow(() -> new ResourceNotFoundException("Business location not found"));
        if (!location.isActive()) {
            throw new BadRequestException("Business location is inactive");
        }
        return location;
    }

    private ServiceOffering requireActiveService(Long businessId, Long serviceId) {
        ServiceOffering service = serviceRepository.findByIdAndBusinessId(serviceId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        if (!service.isActive()) {
            throw new BadRequestException("Service is inactive");
        }
        return service;
    }

    private ZoneId requireZone(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (RuntimeException exception) {
            throw new BadRequestException("Location timezone is invalid");
        }
    }

    private LocalTime min(LocalTime left, LocalTime right) {
        return left.isBefore(right) ? left : right;
    }

    private LocalTime max(LocalTime left, LocalTime right) {
        return left.isAfter(right) ? left : right;
    }

    private record TimeRange(LocalTime start, LocalTime end) {
    }

    private record InstantRange(java.time.Instant start, java.time.Instant end) {
    }
}
