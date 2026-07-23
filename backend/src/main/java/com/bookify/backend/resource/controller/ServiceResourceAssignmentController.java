package com.bookify.backend.resource.controller;

import com.bookify.backend.common.SecurityUtils;
import com.bookify.backend.resource.dto.AssignResourcesRequest;
import com.bookify.backend.resource.dto.ServiceResourceAssignmentResponse;
import com.bookify.backend.resource.service.ServiceResourceAssignmentService;
import com.bookify.backend.tenancy.service.BusinessAccessService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/services/{serviceId}/resources")
public class ServiceResourceAssignmentController {

    private final ServiceResourceAssignmentService assignmentService;
    private final BusinessAccessService accessService;

    public ServiceResourceAssignmentController(
            ServiceResourceAssignmentService assignmentService,
            BusinessAccessService accessService
    ) {
        this.assignmentService = assignmentService;
        this.accessService = accessService;
    }

    @GetMapping
    public ServiceResourceAssignmentResponse find(
            @PathVariable Long businessId,
            @PathVariable Long serviceId
    ) {
        accessService.requireMembership(businessId, SecurityUtils.getCurrentUserEmail());
        return assignmentService.find(businessId, serviceId);
    }

    @PutMapping
    public ServiceResourceAssignmentResponse replace(
            @PathVariable Long businessId,
            @PathVariable Long serviceId,
            @Valid @RequestBody AssignResourcesRequest request
    ) {
        accessService.requireManagementAccess(businessId, SecurityUtils.getCurrentUserEmail());
        return assignmentService.replace(businessId, serviceId, request.resourceIds());
    }
}
