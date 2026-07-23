package com.bookify.backend.resource.service;

import com.bookify.backend.business.model.Business;
import com.bookify.backend.business.repository.BusinessRepository;
import com.bookify.backend.common.exception.BadRequestException;
import com.bookify.backend.common.exception.ResourceNotFoundException;
import com.bookify.backend.location.model.BusinessLocation;
import com.bookify.backend.location.repository.BusinessLocationRepository;
import com.bookify.backend.resource.dto.BookableResourceResponse;
import com.bookify.backend.resource.dto.UpsertResourceRequest;
import com.bookify.backend.resource.model.BookableResource;
import com.bookify.backend.resource.repository.BookableResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookableResourceService {

    private final BookableResourceRepository resourceRepository;
    private final BusinessLocationRepository locationRepository;
    private final BusinessRepository businessRepository;

    public BookableResourceService(
            BookableResourceRepository resourceRepository,
            BusinessLocationRepository locationRepository,
            BusinessRepository businessRepository
    ) {
        this.resourceRepository = resourceRepository;
        this.locationRepository = locationRepository;
        this.businessRepository = businessRepository;
    }

    @Transactional
    public BookableResourceResponse create(
            Long businessId,
            Long locationId,
            UpsertResourceRequest request
    ) {
        Business business = requireActiveBusiness(businessId);
        BusinessLocation location = requireLocation(businessId, locationId);
        if (!location.isActive()) {
            throw new BadRequestException("Resource location must be active");
        }
        validateUniqueName(locationId, null, request.name());

        BookableResource resource = new BookableResource(
                business,
                location,
                request.name().trim(),
                trimToNull(request.description()),
                request.type(),
                request.capacity()
        );
        return BookableResourceResponse.from(resourceRepository.save(resource));
    }

    @Transactional(readOnly = true)
    public List<BookableResourceResponse> findAll(Long businessId, Long locationId) {
        requireLocation(businessId, locationId);
        return resourceRepository.findByBusinessIdAndLocationIdOrderByNameAsc(businessId, locationId)
                .stream()
                .map(BookableResourceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookableResourceResponse findById(Long businessId, Long locationId, Long resourceId) {
        return BookableResourceResponse.from(requireResource(businessId, locationId, resourceId));
    }

    @Transactional
    public BookableResourceResponse update(
            Long businessId,
            Long locationId,
            Long resourceId,
            UpsertResourceRequest request
    ) {
        BookableResource resource = requireResource(businessId, locationId, resourceId);
        validateUniqueName(locationId, resourceId, request.name());
        resource.updateDetails(
                request.name().trim(),
                trimToNull(request.description()),
                request.type(),
                request.capacity()
        );
        return BookableResourceResponse.from(resourceRepository.saveAndFlush(resource));
    }

    @Transactional
    public BookableResourceResponse changeStatus(
            Long businessId,
            Long locationId,
            Long resourceId,
            boolean active
    ) {
        BookableResource resource = requireResource(businessId, locationId, resourceId);
        if (active && !resource.getLocation().isActive()) {
            throw new BadRequestException("Cannot activate a resource in an inactive location");
        }
        resource.setActive(active);
        return BookableResourceResponse.from(resourceRepository.saveAndFlush(resource));
    }

    private void validateUniqueName(Long locationId, Long resourceId, String name) {
        boolean duplicate = resourceId == null
                ? resourceRepository.existsByLocationIdAndNameIgnoreCase(locationId, name.trim())
                : resourceRepository.existsByLocationIdAndNameIgnoreCaseAndIdNot(
                        locationId,
                        name.trim(),
                        resourceId
                );
        if (duplicate) {
            throw new BadRequestException("Resource name is already in use for this location");
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

    private BookableResource requireResource(Long businessId, Long locationId, Long resourceId) {
        return resourceRepository.findByIdAndBusinessIdAndLocationId(resourceId, businessId, locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
