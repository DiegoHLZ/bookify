package com.bookify.backend.capacity.service;

import com.bookify.backend.business.model.BookingMode;
import com.bookify.backend.business.model.ServiceOffering;
import com.bookify.backend.business.repository.OfferingLocationRepository;
import com.bookify.backend.business.repository.ServiceOfferingRepository;
import com.bookify.backend.capacity.dto.CapacitySessionResponse;
import com.bookify.backend.capacity.dto.CreateCapacitySessionRequest;
import com.bookify.backend.capacity.model.CapacitySession;
import com.bookify.backend.capacity.model.CapacitySessionStatus;
import com.bookify.backend.capacity.repository.CapacitySessionRepository;
import com.bookify.backend.common.exception.BadRequestException;
import com.bookify.backend.common.exception.ResourceNotFoundException;
import com.bookify.backend.location.model.BusinessLocation;
import com.bookify.backend.location.repository.BusinessLocationRepository;
import com.bookify.backend.resource.model.BookableResource;
import com.bookify.backend.resource.repository.BookableResourceRepository;
import com.bookify.backend.resource.repository.OfferingResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class CapacitySessionService {
    private final CapacitySessionRepository sessionRepository;
    private final ServiceOfferingRepository serviceRepository;
    private final BusinessLocationRepository locationRepository;
    private final BookableResourceRepository resourceRepository;
    private final OfferingLocationRepository offeringLocationRepository;
    private final OfferingResourceRepository offeringResourceRepository;

    public CapacitySessionService(
            CapacitySessionRepository sessionRepository,
            ServiceOfferingRepository serviceRepository,
            BusinessLocationRepository locationRepository,
            BookableResourceRepository resourceRepository,
            OfferingLocationRepository offeringLocationRepository,
            OfferingResourceRepository offeringResourceRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.serviceRepository = serviceRepository;
        this.locationRepository = locationRepository;
        this.resourceRepository = resourceRepository;
        this.offeringLocationRepository = offeringLocationRepository;
        this.offeringResourceRepository = offeringResourceRepository;
    }

    @Transactional
    public CapacitySessionResponse create(
            Long businessId, Long locationId, Long serviceId,
            CreateCapacitySessionRequest request
    ) {
        ServiceOffering service = requireCapacityService(businessId, serviceId);
        BusinessLocation location = locationRepository.findByIdAndBusinessId(locationId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business location not found"));
        BookableResource resource = resourceRepository
                .findByIdAndBusinessIdAndLocationId(request.resourceId(), businessId, locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Bookable resource not found"));
        if (!location.isActive() || !resource.isActive()
                || !offeringLocationRepository.existsByBusinessIdAndServiceIdAndLocationId(
                businessId, serviceId, locationId)
                || !offeringResourceRepository
                .existsByBusinessIdAndServiceIdAndLocationIdAndResourceId(
                        businessId, serviceId, locationId, resource.getId())) {
            throw new BadRequestException("Session relationships must be active and assigned");
        }
        CapacitySession session = new CapacitySession(
                service.getBusiness(), location, service, resource, request.startsAt(),
                request.startsAt().plus(service.getDurationMinutes(), ChronoUnit.MINUTES),
                request.capacityTotal()
        );
        return CapacitySessionResponse.from(sessionRepository.saveAndFlush(session));
    }

    @Transactional(readOnly = true)
    public List<CapacitySessionResponse> find(Long businessId, Long locationId, Long serviceId) {
        requireCapacityService(businessId, serviceId);
        return sessionRepository
                .findByBusinessIdAndLocationIdAndServiceIdAndStatusAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                        businessId, locationId, serviceId, CapacitySessionStatus.OPEN,
                        java.time.Instant.EPOCH, java.time.Instant.parse("9999-12-31T00:00:00Z"))
                .stream().map(CapacitySessionResponse::from).toList();
    }

    @Transactional
    public CapacitySessionResponse cancel(Long businessId, Long sessionId) {
        CapacitySession session = sessionRepository.findForUpdateByIdAndBusinessId(
                sessionId, businessId
        ).orElseThrow(() -> new ResourceNotFoundException("Capacity session not found"));
        try {
            session.cancel();
        } catch (IllegalStateException exception) {
            throw new BadRequestException(exception.getMessage());
        }
        return CapacitySessionResponse.from(sessionRepository.saveAndFlush(session));
    }

    private ServiceOffering requireCapacityService(Long businessId, Long serviceId) {
        ServiceOffering service = serviceRepository.findByIdAndBusinessId(serviceId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        if (!service.isActive() || service.getBookingMode() != BookingMode.CAPACITY_SESSION) {
            throw new BadRequestException("Service is not an active capacity-session service");
        }
        return service;
    }
}
