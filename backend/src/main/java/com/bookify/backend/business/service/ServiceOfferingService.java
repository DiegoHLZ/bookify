package com.bookify.backend.business.service;

import com.bookify.backend.business.dto.CreateServiceRequest;
import com.bookify.backend.business.dto.ServiceResponse;
import com.bookify.backend.business.dto.UpdateServiceRequest;
import com.bookify.backend.business.model.Business;
import com.bookify.backend.business.model.ServiceOffering;
import com.bookify.backend.business.repository.BusinessRepository;
import com.bookify.backend.business.repository.ServiceOfferingRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServiceOfferingService {

    private final ServiceOfferingRepository serviceOfferingRepository;
    private final BusinessRepository businessRepository;

    public ServiceOfferingService(ServiceOfferingRepository serviceOfferingRepository,
                                  BusinessRepository businessRepository) {
        this.serviceOfferingRepository = serviceOfferingRepository;
        this.businessRepository = businessRepository;
    }

    public ServiceResponse create(Long businessId, CreateServiceRequest request) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Business not found"));

        ServiceOffering service = new ServiceOffering();
        service.setName(request.getName());
        service.setDescription(request.getDescription());
        service.setDurationMinutes(request.getDurationMinutes());
        service.setPrice(request.getPrice());
        service.setCurrency(request.getCurrency());
        service.setBusiness(business);
        service.setActive(true);

        return toResponse(serviceOfferingRepository.save(service));
    }

    public List<ServiceResponse> getAllByBusiness(Long businessId) {
        return serviceOfferingRepository.findByBusinessId(businessId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ServiceResponse getById(Long businessId, Long serviceId) {
        ServiceOffering service = serviceOfferingRepository.findByIdAndBusinessId(serviceId, businessId)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        return toResponse(service);
    }

    public ServiceResponse update(Long businessId, Long serviceId, UpdateServiceRequest request) {
        ServiceOffering service = serviceOfferingRepository.findByIdAndBusinessId(serviceId, businessId)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        service.setName(request.getName());
        service.setDescription(request.getDescription());
        service.setDurationMinutes(request.getDurationMinutes());
        service.setPrice(request.getPrice());
        service.setCurrency(request.getCurrency());
        service.setActive(request.getActive());

        return toResponse(serviceOfferingRepository.save(service));
    }

    public void delete(Long businessId, Long serviceId) {
        ServiceOffering service = serviceOfferingRepository.findByIdAndBusinessId(serviceId, businessId)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        serviceOfferingRepository.delete(service);
    }

    private ServiceResponse toResponse(ServiceOffering service) {
        return new ServiceResponse(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.getDurationMinutes(),
                service.getPrice(),
                service.getCurrency(),
                service.isActive(),
                service.getBusiness().getId(),
                service.getCreatedAt(),
                service.getUpdatedAt()
        );
    }
}
