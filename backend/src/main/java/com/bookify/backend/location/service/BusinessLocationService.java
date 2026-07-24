package com.bookify.backend.location.service;

import com.bookify.backend.business.model.Business;
import com.bookify.backend.business.repository.BusinessRepository;
import com.bookify.backend.common.exception.BadRequestException;
import com.bookify.backend.common.exception.ResourceNotFoundException;
import com.bookify.backend.location.dto.BusinessLocationResponse;
import com.bookify.backend.location.dto.UpsertLocationRequest;
import com.bookify.backend.location.model.BusinessLocation;
import com.bookify.backend.location.repository.BusinessLocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class BusinessLocationService {

    private final BusinessLocationRepository locationRepository;
    private final BusinessRepository businessRepository;

    public BusinessLocationService(
            BusinessLocationRepository locationRepository,
            BusinessRepository businessRepository
    ) {
        this.locationRepository = locationRepository;
        this.businessRepository = businessRepository;
    }

    @Transactional
    public BusinessLocationResponse create(Long businessId, UpsertLocationRequest request) {
        Business business = requireActiveBusiness(businessId);
        validateRequest(businessId, null, request);

        BusinessLocation location = new BusinessLocation(
                business,
                request.name().trim(),
                request.address().trim(),
                request.city().trim(),
                request.countryCode().trim().toUpperCase(Locale.ROOT),
                request.timezone().trim(),
                request.latitude(),
                request.longitude()
        );

        return BusinessLocationResponse.from(locationRepository.save(location));
    }

    @Transactional(readOnly = true)
    public List<BusinessLocationResponse> findAll(Long businessId) {
        requireActiveBusiness(businessId);
        return locationRepository.findByBusinessIdOrderByNameAsc(businessId)
                .stream()
                .map(BusinessLocationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BusinessLocationResponse findById(Long businessId, Long locationId) {
        requireActiveBusiness(businessId);
        return BusinessLocationResponse.from(requireLocation(businessId, locationId));
    }

    @Transactional
    public BusinessLocationResponse update(
            Long businessId,
            Long locationId,
            UpsertLocationRequest request
    ) {
        requireActiveBusiness(businessId);
        BusinessLocation location = requireLocation(businessId, locationId);
        validateRequest(businessId, locationId, request);

        location.updateDetails(
                request.name().trim(),
                request.address().trim(),
                request.city().trim(),
                request.countryCode().trim().toUpperCase(Locale.ROOT),
                request.timezone().trim(),
                request.latitude(),
                request.longitude()
        );

        return BusinessLocationResponse.from(locationRepository.saveAndFlush(location));
    }

    @Transactional
    public BusinessLocationResponse changeStatus(Long businessId, Long locationId, boolean active) {
        Business business = businessRepository.findByIdForUpdate(businessId)
                .filter(Business::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));
        BusinessLocation location = requireLocation(business.getId(), locationId);

        if (!active && location.isActive()
                && locationRepository.countByBusinessIdAndActiveTrue(businessId) <= 1) {
            throw new BadRequestException("A business must have at least one active location");
        }

        location.setActive(active);
        return BusinessLocationResponse.from(locationRepository.saveAndFlush(location));
    }

    @Transactional
    public BusinessLocationResponse verifyCoordinates(
            Long businessId,
            Long locationId,
            String source
    ) {
        requireActiveBusiness(businessId);
        BusinessLocation location = requireLocation(businessId, locationId);
        location.verifyCoordinates(source.trim(), Instant.now());
        return BusinessLocationResponse.from(locationRepository.saveAndFlush(location));
    }

    private void validateRequest(Long businessId, Long locationId, UpsertLocationRequest request) {
        boolean duplicateName = locationId == null
                ? locationRepository.existsByBusinessIdAndNameIgnoreCase(businessId, request.name().trim())
                : locationRepository.existsByBusinessIdAndNameIgnoreCaseAndIdNot(
                        businessId,
                        request.name().trim(),
                        locationId
                );
        if (duplicateName) {
            throw new BadRequestException("Location name is already in use for this business");
        }

        try {
            ZoneId.of(request.timezone().trim());
        } catch (DateTimeException exception) {
            throw new BadRequestException("Location timezone must be a valid IANA timezone");
        }
    }

    private Business requireActiveBusiness(Long businessId) {
        return businessRepository.findById(businessId)
                .filter(Business::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));
    }

    private BusinessLocation requireLocation(Long businessId, Long locationId) {
        return locationRepository.findByIdAndBusinessId(locationId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found"));
    }
}
