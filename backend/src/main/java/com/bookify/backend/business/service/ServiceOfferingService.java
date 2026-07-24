package com.bookify.backend.business.service;

import com.bookify.backend.business.dto.CreateServiceRequest;
import com.bookify.backend.business.dto.ServiceResponse;
import com.bookify.backend.business.dto.UpdateServiceRequest;
import com.bookify.backend.business.model.Business;
import com.bookify.backend.business.model.BookingMode;
import com.bookify.backend.business.model.OfferingLocation;
import com.bookify.backend.business.model.ServiceOffering;
import com.bookify.backend.business.repository.BusinessRepository;
import com.bookify.backend.business.repository.OfferingLocationRepository;
import com.bookify.backend.business.repository.ServiceOfferingRepository;
import com.bookify.backend.common.exception.BadRequestException;
import com.bookify.backend.common.exception.ResourceNotFoundException;
import com.bookify.backend.location.model.BusinessLocation;
import com.bookify.backend.location.repository.BusinessLocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class ServiceOfferingService {

    private final ServiceOfferingRepository serviceOfferingRepository;
    private final BusinessRepository businessRepository;
    private final BusinessLocationRepository locationRepository;
    private final OfferingLocationRepository offeringLocationRepository;

    public ServiceOfferingService(
            ServiceOfferingRepository serviceOfferingRepository,
            BusinessRepository businessRepository,
            BusinessLocationRepository locationRepository,
            OfferingLocationRepository offeringLocationRepository
    ) {
        this.serviceOfferingRepository = serviceOfferingRepository;
        this.businessRepository = businessRepository;
        this.locationRepository = locationRepository;
        this.offeringLocationRepository = offeringLocationRepository;
    }

    @Transactional
    public ServiceResponse create(Long businessId, CreateServiceRequest request) {
        Business business = businessRepository.findById(businessId)
                .filter(Business::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));
        List<BusinessLocation> locations = requireActiveLocations(businessId, request.getLocationIds());

        ServiceOffering service = new ServiceOffering();
        service.setName(request.getName().trim());
        service.setDescription(trimToNull(request.getDescription()));
        service.setDurationMinutes(request.getDurationMinutes());
        service.setPrice(request.getPrice());
        service.setCurrency(request.getCurrency());
        service.setBusiness(business);
        service.setActive(true);
        service.setBookingMode(defaultMode(request.getBookingMode()));
        applyPolicies(
                service,
                defaultBoolean(request.getCustomerCancellationAllowed(), true),
                defaultInteger(request.getCancellationNoticeMinutes(), 0),
                defaultBoolean(request.getCustomerRescheduleAllowed(), true),
                defaultInteger(request.getRescheduleNoticeMinutes(), 0),
                defaultInteger(request.getMaxReschedules(), 1)
        );

        serviceOfferingRepository.save(service);
        saveLocationLinks(business, service, locations);
        return toResponse(service);
    }

    @Transactional(readOnly = true)
    public List<ServiceResponse> getAllByBusiness(Long businessId) {
        return serviceOfferingRepository.findByBusinessIdAndActiveTrue(businessId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceResponse getById(Long businessId, Long serviceId) {
        ServiceOffering service = serviceOfferingRepository.findByIdAndBusinessId(serviceId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        return toResponse(service);
    }

    @Transactional
    public ServiceResponse update(Long businessId, Long serviceId, UpdateServiceRequest request) {
        ServiceOffering service = serviceOfferingRepository.findByIdAndBusinessId(serviceId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        List<BusinessLocation> locations = requireActiveLocations(businessId, request.getLocationIds());

        service.setName(request.getName().trim());
        service.setDescription(trimToNull(request.getDescription()));
        service.setDurationMinutes(request.getDurationMinutes());
        service.setPrice(request.getPrice());
        service.setCurrency(request.getCurrency());
        service.setActive(request.getActive());
        service.setBookingMode(defaultMode(request.getBookingMode()));
        applyPolicies(
                service,
                defaultBoolean(
                        request.getCustomerCancellationAllowed(),
                        service.isCustomerCancellationAllowed()
                ),
                defaultInteger(
                        request.getCancellationNoticeMinutes(),
                        service.getCancellationNoticeMinutes()
                ),
                defaultBoolean(
                        request.getCustomerRescheduleAllowed(),
                        service.isCustomerRescheduleAllowed()
                ),
                defaultInteger(
                        request.getRescheduleNoticeMinutes(),
                        service.getRescheduleNoticeMinutes()
                ),
                defaultInteger(request.getMaxReschedules(), service.getMaxReschedules())
        );

        offeringLocationRepository.deleteByServiceId(serviceId);
        offeringLocationRepository.flush();
        saveLocationLinks(service.getBusiness(), service, locations);
        serviceOfferingRepository.saveAndFlush(service);
        return toResponse(service);
    }

    @Transactional
    public void delete(Long businessId, Long serviceId) {
        ServiceOffering service = serviceOfferingRepository.findByIdAndBusinessId(serviceId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        service.setActive(false);
        serviceOfferingRepository.save(service);
    }

    private List<BusinessLocation> requireActiveLocations(Long businessId, Set<Long> locationIds) {
        List<BusinessLocation> locations =
                locationRepository.findAllByIdInAndBusinessIdAndActiveTrue(locationIds, businessId);
        if (locations.size() != locationIds.size()) {
            throw new BadRequestException(
                    "All service locations must be active and belong to the business"
            );
        }
        return locations;
    }

    private void saveLocationLinks(
            Business business,
            ServiceOffering service,
            List<BusinessLocation> locations
    ) {
        List<OfferingLocation> links = locations.stream()
                .map(location -> new OfferingLocation(business, service, location))
                .toList();
        offeringLocationRepository.saveAll(links);
    }

    private ServiceResponse toResponse(ServiceOffering service) {
        List<Long> locationIds = offeringLocationRepository
                .findLocationIdsByServiceId(service.getId())
                .stream()
                .sorted(Comparator.naturalOrder())
                .toList();
        return new ServiceResponse(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.getDurationMinutes(),
                service.getPrice(),
                service.getCurrency(),
                service.isActive(),
                service.getBookingMode(),
                service.isCustomerCancellationAllowed(),
                service.getCancellationNoticeMinutes(),
                service.isCustomerRescheduleAllowed(),
                service.getRescheduleNoticeMinutes(),
                service.getMaxReschedules(),
                service.getBusiness().getId(),
                locationIds,
                service.getCreatedAt(),
                service.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private BookingMode defaultMode(BookingMode mode) {
        return mode == null ? BookingMode.EXCLUSIVE_RESOURCE : mode;
    }

    private void applyPolicies(
            ServiceOffering service,
            boolean cancellationAllowed,
            int cancellationNotice,
            boolean rescheduleAllowed,
            int rescheduleNotice,
            int maxReschedules
    ) {
        service.setCustomerCancellationAllowed(cancellationAllowed);
        service.setCancellationNoticeMinutes(cancellationNotice);
        service.setCustomerRescheduleAllowed(rescheduleAllowed);
        service.setRescheduleNoticeMinutes(rescheduleNotice);
        service.setMaxReschedules(maxReschedules);
    }

    private boolean defaultBoolean(Boolean value, boolean fallback) {
        return value == null ? fallback : value;
    }

    private int defaultInteger(Integer value, int fallback) {
        return value == null ? fallback : value;
    }
}
