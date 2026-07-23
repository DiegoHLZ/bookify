package com.bookify.backend.resource.controller;

import com.bookify.backend.common.SecurityUtils;
import com.bookify.backend.resource.dto.BookableResourceResponse;
import com.bookify.backend.resource.dto.ResourceStatusRequest;
import com.bookify.backend.resource.dto.UpsertResourceRequest;
import com.bookify.backend.resource.service.BookableResourceService;
import com.bookify.backend.tenancy.service.BusinessAccessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/locations/{locationId}/resources")
public class BookableResourceController {

    private final BookableResourceService resourceService;
    private final BusinessAccessService accessService;

    public BookableResourceController(
            BookableResourceService resourceService,
            BusinessAccessService accessService
    ) {
        this.resourceService = resourceService;
        this.accessService = accessService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookableResourceResponse create(
            @PathVariable Long businessId,
            @PathVariable Long locationId,
            @Valid @RequestBody UpsertResourceRequest request
    ) {
        requireManagement(businessId);
        return resourceService.create(businessId, locationId, request);
    }

    @GetMapping
    public List<BookableResourceResponse> findAll(
            @PathVariable Long businessId,
            @PathVariable Long locationId
    ) {
        requireMembership(businessId);
        return resourceService.findAll(businessId, locationId);
    }

    @GetMapping("/{resourceId}")
    public BookableResourceResponse findById(
            @PathVariable Long businessId,
            @PathVariable Long locationId,
            @PathVariable Long resourceId
    ) {
        requireMembership(businessId);
        return resourceService.findById(businessId, locationId, resourceId);
    }

    @PutMapping("/{resourceId}")
    public BookableResourceResponse update(
            @PathVariable Long businessId,
            @PathVariable Long locationId,
            @PathVariable Long resourceId,
            @Valid @RequestBody UpsertResourceRequest request
    ) {
        requireManagement(businessId);
        return resourceService.update(businessId, locationId, resourceId, request);
    }

    @PatchMapping("/{resourceId}/status")
    public BookableResourceResponse changeStatus(
            @PathVariable Long businessId,
            @PathVariable Long locationId,
            @PathVariable Long resourceId,
            @Valid @RequestBody ResourceStatusRequest request
    ) {
        requireManagement(businessId);
        return resourceService.changeStatus(
                businessId,
                locationId,
                resourceId,
                request.active()
        );
    }

    private void requireMembership(Long businessId) {
        accessService.requireMembership(businessId, SecurityUtils.getCurrentUserEmail());
    }

    private void requireManagement(Long businessId) {
        accessService.requireManagementAccess(businessId, SecurityUtils.getCurrentUserEmail());
    }
}
