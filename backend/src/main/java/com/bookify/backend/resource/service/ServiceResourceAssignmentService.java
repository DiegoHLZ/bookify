package com.bookify.backend.resource.service;

import com.bookify.backend.business.model.ServiceOffering;
import com.bookify.backend.business.repository.OfferingLocationRepository;
import com.bookify.backend.business.repository.ServiceOfferingRepository;
import com.bookify.backend.common.exception.BadRequestException;
import com.bookify.backend.common.exception.ResourceNotFoundException;
import com.bookify.backend.resource.dto.ServiceResourceAssignmentResponse;
import com.bookify.backend.resource.model.BookableResource;
import com.bookify.backend.resource.model.OfferingResource;
import com.bookify.backend.resource.repository.BookableResourceRepository;
import com.bookify.backend.resource.repository.OfferingResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class ServiceResourceAssignmentService {

    private final ServiceOfferingRepository serviceRepository;
    private final BookableResourceRepository resourceRepository;
    private final OfferingLocationRepository offeringLocationRepository;
    private final OfferingResourceRepository offeringResourceRepository;

    public ServiceResourceAssignmentService(
            ServiceOfferingRepository serviceRepository,
            BookableResourceRepository resourceRepository,
            OfferingLocationRepository offeringLocationRepository,
            OfferingResourceRepository offeringResourceRepository
    ) {
        this.serviceRepository = serviceRepository;
        this.resourceRepository = resourceRepository;
        this.offeringLocationRepository = offeringLocationRepository;
        this.offeringResourceRepository = offeringResourceRepository;
    }

    @Transactional
    public ServiceResourceAssignmentResponse replace(
            Long businessId,
            Long serviceId,
            Set<Long> resourceIds
    ) {
        ServiceOffering service = requireService(businessId, serviceId);
        List<BookableResource> resources =
                resourceRepository.findAllByIdInAndBusinessIdAndActiveTrueAndLocationActiveTrue(
                        resourceIds,
                        businessId
                );
        if (resources.size() != resourceIds.size()) {
            throw new BadRequestException("All resources must be active and belong to the business");
        }

        boolean invalidLocation = resources.stream().anyMatch(resource ->
                !offeringLocationRepository.existsByBusinessIdAndServiceIdAndLocationId(
                        businessId,
                        serviceId,
                        resource.getLocation().getId()
                )
        );
        if (invalidLocation) {
            throw new BadRequestException(
                    "Each resource must belong to a location where the service is offered"
            );
        }

        offeringResourceRepository.deleteByServiceId(serviceId);
        offeringResourceRepository.flush();
        offeringResourceRepository.saveAll(resources.stream()
                .map(resource -> new OfferingResource(service.getBusiness(), service, resource))
                .toList());

        return response(serviceId);
    }

    @Transactional(readOnly = true)
    public ServiceResourceAssignmentResponse find(Long businessId, Long serviceId) {
        requireService(businessId, serviceId);
        return response(serviceId);
    }

    private ServiceOffering requireService(Long businessId, Long serviceId) {
        return serviceRepository.findByIdAndBusinessId(serviceId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
    }

    private ServiceResourceAssignmentResponse response(Long serviceId) {
        return new ServiceResourceAssignmentResponse(
                serviceId,
                offeringResourceRepository.findResourceIdsByServiceId(serviceId)
        );
    }
}
