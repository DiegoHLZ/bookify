package com.bookify.backend.discovery.service;

import com.bookify.backend.common.exception.BadRequestException;
import com.bookify.backend.discovery.dto.NearbyBusinessResponse;
import com.bookify.backend.location.repository.BusinessLocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
public class NearbyDiscoveryService {
    private final BusinessLocationRepository locationRepository;

    public NearbyDiscoveryService(BusinessLocationRepository locationRepository) {
        this.locationRepository = locationRepository;
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
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90
                || longitude < -180 || longitude > 180) {
            throw new BadRequestException("Coordinates are outside valid WGS84 bounds");
        }
        if (!Double.isFinite(radiusKm) || radiusKm <= 0 || radiusKm > 100) {
            throw new BadRequestException("Radius must be greater than 0 and at most 100 km");
        }
        if (minRating.compareTo(BigDecimal.ZERO) < 0
                || minRating.compareTo(BigDecimal.valueOf(5)) > 0) {
            throw new BadRequestException("Minimum rating must be between 0 and 5");
        }
        if (limit < 1 || limit > 100) {
            throw new BadRequestException("Limit must be between 1 and 100");
        }
        String normalizedCategory = categoryCode == null || categoryCode.isBlank()
                ? null : categoryCode.trim().toUpperCase(Locale.ROOT);
        return locationRepository.findVerifiedNearby(
                        latitude, longitude, radiusKm * 1000,
                        normalizedCategory, minRating, limit
                )
                .stream()
                .map(row -> new NearbyBusinessResponse(
                        row.getBusinessId(), row.getBusinessName(), row.getCategoryCode(),
                        row.getRatingAverage(), row.getRatingCount(), row.getLocationId(),
                        row.getLocationName(), row.getAddress(), row.getCity(),
                        row.getCountryCode(), row.getLatitude(), row.getLongitude(),
                        row.getDistanceMeters()
                ))
                .toList();
    }
}
