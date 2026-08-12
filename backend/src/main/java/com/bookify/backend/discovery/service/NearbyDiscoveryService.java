package com.bookify.backend.discovery.service;

import com.bookify.backend.availability.service.AvailabilitySlotService;
import com.bookify.backend.business.model.Business;
import com.bookify.backend.business.model.ServiceOffering;
import com.bookify.backend.business.repository.BusinessRepository;
import com.bookify.backend.business.repository.OfferingLocationRepository;
import com.bookify.backend.business.repository.ServiceOfferingRepository;
import com.bookify.backend.common.exception.BadRequestException;
import com.bookify.backend.common.exception.ResourceNotFoundException;
import com.bookify.backend.discovery.dto.*;
import com.bookify.backend.location.model.BusinessLocation;
import com.bookify.backend.location.repository.BusinessLocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class NearbyDiscoveryService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_PAGE = 100;
    private static final int AVAILABILITY_BATCH_SIZE = 100;
    private static final int MAX_AVAILABILITY_CANDIDATES = 5_000;

    private final BusinessLocationRepository locationRepository;
    private final BusinessRepository businessRepository;
    private final ServiceOfferingRepository serviceRepository;
    private final OfferingLocationRepository offeringLocationRepository;
    private final AvailabilitySlotService availabilitySlotService;

    public NearbyDiscoveryService(
            BusinessLocationRepository locationRepository,
            BusinessRepository businessRepository,
            ServiceOfferingRepository serviceRepository,
            OfferingLocationRepository offeringLocationRepository,
            AvailabilitySlotService availabilitySlotService
    ) {
        this.locationRepository = locationRepository;
        this.businessRepository = businessRepository;
        this.serviceRepository = serviceRepository;
        this.offeringLocationRepository = offeringLocationRepository;
        this.availabilitySlotService = availabilitySlotService;
    }

    @Transactional(readOnly = true)
    public List<NearbyBusinessResponse> findNearby(
            double latitude,
            double longitude,
            double radiusKm,
            String categoryCode,
            BigDecimal minRating,
            int limit
    ) {
        DiscoverySearchPageResponse page = search(
                latitude, longitude, radiusKm, null, categoryCode,
                minRating, null, 0, limit
        );
        return page.items().stream().map(item -> new NearbyBusinessResponse(
                item.businessId(), item.businessName(), item.categoryCode(),
                item.ratingAverage(), item.ratingCount(), item.locationId(),
                item.locationName(), item.address(), item.city(), item.countryCode(),
                item.latitude(), item.longitude(), item.distanceMeters()
        )).toList();
    }

    @Transactional(readOnly = true)
    public DiscoverySearchPageResponse search(
            double latitude,
            double longitude,
            double radiusKm,
            String text,
            String categoryCode,
            BigDecimal minRating,
            LocalDateTime availableAt,
            int page,
            int size
    ) {
        validate(latitude, longitude, radiusKm, minRating, page, size);
        String normalizedCategory = normalizeCategory(categoryCode);
        String normalizedText = normalizeText(text);

        if (availableAt == null) {
            List<DiscoverySearchItemResponse> items = locationRepository.searchVerifiedNearby(
                            latitude, longitude, radiusKm * 1000, normalizedCategory,
                            minRating, normalizedText, page * size, size + 1
                    ).stream()
                    .map(row -> map(row, null, List.of()))
                    .toList();
            boolean hasNext = items.size() > size;
            return new DiscoverySearchPageResponse(
                    page, size, hasNext, items.stream().limit(size).toList()
            );
        }

        int eligibleToSkip = page * size;
        int scanned = 0;
        int eligibleSeen = 0;
        boolean exhausted = false;
        List<DiscoverySearchItemResponse> pageItems = new ArrayList<>();
        while (scanned < MAX_AVAILABILITY_CANDIDATES
                && pageItems.size() <= size
                && !exhausted) {
            int batchSize = Math.min(
                    AVAILABILITY_BATCH_SIZE,
                    MAX_AVAILABILITY_CANDIDATES - scanned
            );
            List<NearbyLocationProjection> batch = locationRepository.searchVerifiedNearby(
                    latitude, longitude, radiusKm * 1000, normalizedCategory,
                    minRating, normalizedText, scanned, batchSize
            );
            exhausted = batch.size() < batchSize;
            scanned += batch.size();
            for (NearbyLocationProjection row : batch) {
                List<Long> availableServices = availableServices(
                        row.getBusinessId(), row.getLocationId(), availableAt
                );
                if (availableServices.isEmpty()) {
                    continue;
                }
                if (eligibleSeen++ < eligibleToSkip) {
                    continue;
                }
                pageItems.add(map(row, availableAt, availableServices));
                if (pageItems.size() > size) {
                    break;
                }
            }
        }
        if (!exhausted && pageItems.size() <= size) {
            throw new BadRequestException(
                    "Availability search matched too many locations; narrow the filters"
            );
        }
        boolean hasNext = pageItems.size() > size;
        return new DiscoverySearchPageResponse(
                page,
                size,
                hasNext,
                pageItems.stream().limit(size).toList()
        );
    }

    @Transactional(readOnly = true)
    public PublicBusinessDetailResponse findPublicBusiness(String slug) {
        String normalizedSlug = slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT);
        Business business = businessRepository.findBySlugAndActiveTrue(normalizedSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));
        List<BusinessLocation> locations = locationRepository
                .findByBusinessIdAndActiveTrueAndCoordinatesVerifiedTrueOrderByIdAsc(
                        business.getId()
                );
        Set<Long> publicLocationIds = locations.stream()
                .map(BusinessLocation::getId)
                .collect(java.util.stream.Collectors.toSet());
        List<PublicServiceResponse> services = serviceRepository
                .findByBusinessIdAndActiveTrueOrderByNameAscIdAsc(business.getId())
                .stream()
                .map(service -> mapService(service, publicLocationIds))
                .filter(service -> !service.locationIds().isEmpty())
                .toList();
        return new PublicBusinessDetailResponse(
                business.getId(), business.getSlug(), business.getName(),
                business.getDescription(), business.getCategoryCode(),
                business.getPhone(), business.getEmail(), business.getRatingAverage(),
                business.getRatingCount(), locations.stream().map(this::mapLocation).toList(),
                services
        );
    }

    private List<Long> availableServices(
            Long businessId,
            Long locationId,
            LocalDateTime availableAt
    ) {
        return offeringLocationRepository.findActiveServiceIdsAtLocation(businessId, locationId)
                .stream()
                .filter(serviceId -> availabilitySlotService.findAvailability(
                                businessId, locationId, serviceId,
                                availableAt.toLocalDate(), availableAt.toLocalDate(), 5
                        ).slots().stream()
                        .anyMatch(slot -> slot.localStart().equals(availableAt)))
                .toList();
    }

    private DiscoverySearchItemResponse map(
            NearbyLocationProjection row,
            LocalDateTime requestedAt,
            List<Long> availableServices
    ) {
        return new DiscoverySearchItemResponse(
                row.getBusinessId(), row.getBusinessSlug(), row.getBusinessName(), row.getCategoryCode(),
                row.getRatingAverage(), row.getRatingCount(), row.getLocationId(),
                row.getLocationName(), row.getAddress(), row.getCity(), row.getCountryCode(),
                row.getTimezone(), row.getLatitude(), row.getLongitude(),
                row.getDistanceMeters(), requestedAt, List.copyOf(availableServices)
        );
    }

    private PublicLocationResponse mapLocation(BusinessLocation location) {
        return new PublicLocationResponse(
                location.getId(), location.getName(), location.getAddress(), location.getCity(),
                location.getCountryCode(), location.getTimezone(), location.getLatitude(),
                location.getLongitude()
        );
    }

    private PublicServiceResponse mapService(
            ServiceOffering service,
            Set<Long> publicLocationIds
    ) {
        List<Long> locationIds = offeringLocationRepository
                .findLocationIdsByServiceId(service.getId())
                .stream()
                .filter(publicLocationIds::contains)
                .toList();
        return new PublicServiceResponse(
                service.getId(), service.getName(), service.getDescription(),
                service.getDurationMinutes(), service.getPrice(), service.getCurrency(),
                service.getBookingMode(), locationIds
        );
    }

    private void validate(
            double latitude,
            double longitude,
            double radiusKm,
            BigDecimal minRating,
            int page,
            int size
    ) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90
                || longitude < -180 || longitude > 180) {
            throw new BadRequestException("Coordinates are outside valid WGS84 bounds");
        }
        if (!Double.isFinite(radiusKm) || radiusKm <= 0 || radiusKm > 100) {
            throw new BadRequestException("Radius must be greater than 0 and at most 100 km");
        }
        if (minRating == null || minRating.compareTo(BigDecimal.ZERO) < 0
                || minRating.compareTo(BigDecimal.valueOf(5)) > 0) {
            throw new BadRequestException("Minimum rating must be between 0 and 5");
        }
        if (page < 0 || page > MAX_PAGE) {
            throw new BadRequestException("Page must be between 0 and 100");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Page size must be between 1 and 100");
        }
    }

    private String normalizeCategory(String value) {
        return value == null || value.isBlank()
                ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() > 100) {
            throw new BadRequestException("Search text cannot exceed 100 characters");
        }
        return normalized;
    }
}
