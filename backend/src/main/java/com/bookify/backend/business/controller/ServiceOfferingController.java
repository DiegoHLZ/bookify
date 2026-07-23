package com.bookify.backend.business.controller;

import com.bookify.backend.business.dto.CreateServiceRequest;
import com.bookify.backend.business.dto.ServiceResponse;
import com.bookify.backend.business.dto.UpdateServiceRequest;
import com.bookify.backend.business.service.ServiceOfferingService;
import com.bookify.backend.common.SecurityUtils;
import com.bookify.backend.tenancy.service.BusinessAccessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/businesses/{businessId}/services")
public class ServiceOfferingController {

    private final ServiceOfferingService serviceOfferingService;
    private final BusinessAccessService businessAccessService;

    public ServiceOfferingController(
            ServiceOfferingService serviceOfferingService,
            BusinessAccessService businessAccessService
    ) {
        this.serviceOfferingService = serviceOfferingService;
        this.businessAccessService = businessAccessService;
    }

    @PostMapping
    public ResponseEntity<ServiceResponse> create(
            @PathVariable Long businessId,
            @Valid @RequestBody CreateServiceRequest request
    ) {
        businessAccessService.requireManagementAccess(
                businessId,
                SecurityUtils.getCurrentUserEmail()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(serviceOfferingService.create(businessId, request));
    }

    @GetMapping
    public ResponseEntity<List<ServiceResponse>> getAll(@PathVariable Long businessId) {
        businessAccessService.requireMembership(
                businessId,
                SecurityUtils.getCurrentUserEmail()
        );
        return ResponseEntity.ok(serviceOfferingService.getAllByBusiness(businessId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceResponse> getById(
            @PathVariable Long businessId,
            @PathVariable Long id
    ) {
        businessAccessService.requireMembership(
                businessId,
                SecurityUtils.getCurrentUserEmail()
        );
        return ResponseEntity.ok(serviceOfferingService.getById(businessId, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceResponse> update(
            @PathVariable Long businessId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateServiceRequest request
    ) {
        businessAccessService.requireManagementAccess(
                businessId,
                SecurityUtils.getCurrentUserEmail()
        );
        return ResponseEntity.ok(serviceOfferingService.update(businessId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long businessId,
            @PathVariable Long id
    ) {
        businessAccessService.requireManagementAccess(
                businessId,
                SecurityUtils.getCurrentUserEmail()
        );
        serviceOfferingService.delete(businessId, id);
        return ResponseEntity.noContent().build();
    }
}
