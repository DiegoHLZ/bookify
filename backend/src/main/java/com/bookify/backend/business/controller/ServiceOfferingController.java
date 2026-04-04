package com.bookify.backend.business.controller;

import com.bookify.backend.business.dto.CreateServiceRequest;
import com.bookify.backend.business.dto.ServiceResponse;
import com.bookify.backend.business.dto.UpdateServiceRequest;
import com.bookify.backend.business.service.ServiceOfferingService;
import com.bookify.backend.common.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceOfferingController {

    private final ServiceOfferingService serviceOfferingService;

    public ServiceOfferingController(ServiceOfferingService serviceOfferingService) {
        this.serviceOfferingService = serviceOfferingService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ServiceResponse> create(@Valid @RequestBody CreateServiceRequest request) {
        Long businessId = SecurityUtils.getCurrentBusinessId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(serviceOfferingService.create(businessId, request));
    }

    @GetMapping
    public ResponseEntity<List<ServiceResponse>> getAll() {
        Long businessId = SecurityUtils.getCurrentBusinessId();
        return ResponseEntity.ok(serviceOfferingService.getAllByBusiness(businessId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceResponse> getById(@PathVariable Long id) {
        Long businessId = SecurityUtils.getCurrentBusinessId();
        return ResponseEntity.ok(serviceOfferingService.getById(businessId, id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ServiceResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody UpdateServiceRequest request) {
        Long businessId = SecurityUtils.getCurrentBusinessId();
        return ResponseEntity.ok(serviceOfferingService.update(businessId, id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Long businessId = SecurityUtils.getCurrentBusinessId();
        serviceOfferingService.delete(businessId, id);
        return ResponseEntity.noContent().build();
    }
}