package com.bookify.backend.location.controller;

import com.bookify.backend.common.SecurityUtils;
import com.bookify.backend.location.dto.BusinessLocationResponse;
import com.bookify.backend.location.dto.LocationStatusRequest;
import com.bookify.backend.location.dto.UpsertLocationRequest;
import com.bookify.backend.location.dto.VerifyCoordinatesRequest;
import com.bookify.backend.location.service.BusinessLocationService;
import com.bookify.backend.tenancy.service.BusinessAccessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import org.springframework.security.access.AccessDeniedException;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/locations")
public class BusinessLocationController {

    private final BusinessLocationService locationService;
    private final BusinessAccessService accessService;

    public BusinessLocationController(
            BusinessLocationService locationService,
            BusinessAccessService accessService
    ) {
        this.locationService = locationService;
        this.accessService = accessService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BusinessLocationResponse create(
            @PathVariable Long businessId,
            @Valid @RequestBody UpsertLocationRequest request
    ) {
        requireManagement(businessId);
        return locationService.create(businessId, request);
    }

    @GetMapping
    public List<BusinessLocationResponse> findAll(@PathVariable Long businessId) {
        requireMembership(businessId);
        return locationService.findAll(businessId);
    }

    @GetMapping("/{locationId}")
    public BusinessLocationResponse findById(
            @PathVariable Long businessId,
            @PathVariable Long locationId
    ) {
        requireMembership(businessId);
        return locationService.findById(businessId, locationId);
    }

    @PutMapping("/{locationId}")
    public BusinessLocationResponse update(
            @PathVariable Long businessId,
            @PathVariable Long locationId,
            @Valid @RequestBody UpsertLocationRequest request
    ) {
        requireManagement(businessId);
        return locationService.update(businessId, locationId, request);
    }

    @PatchMapping("/{locationId}/status")
    public BusinessLocationResponse changeStatus(
            @PathVariable Long businessId,
            @PathVariable Long locationId,
            @Valid @RequestBody LocationStatusRequest request
    ) {
        requireManagement(businessId);
        return locationService.changeStatus(businessId, locationId, request.active());
    }

    @PostMapping("/{locationId}/coordinates/verify")
    public BusinessLocationResponse verifyCoordinates(
            @PathVariable Long businessId,
            @PathVariable Long locationId,
            @Valid @RequestBody VerifyCoordinatesRequest request
    ) {
        if (!"ADMIN".equals(SecurityUtils.getCurrentRole())) {
            throw new AccessDeniedException("Platform administrator access required");
        }
        return locationService.verifyCoordinates(
                businessId, locationId, request.source()
        );
    }

    private void requireMembership(Long businessId) {
        accessService.requireMembership(businessId, SecurityUtils.getCurrentUserEmail());
    }

    private void requireManagement(Long businessId) {
        accessService.requireManagementAccess(businessId, SecurityUtils.getCurrentUserEmail());
    }
}
