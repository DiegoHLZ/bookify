package com.bookify.backend.capacity.controller;

import com.bookify.backend.capacity.dto.CapacitySessionResponse;
import com.bookify.backend.capacity.dto.CreateCapacitySessionRequest;
import com.bookify.backend.capacity.service.CapacitySessionService;
import com.bookify.backend.common.SecurityUtils;
import com.bookify.backend.tenancy.service.BusinessAccessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/locations/{locationId}/services/{serviceId}/sessions")
public class CapacitySessionController {
    private final CapacitySessionService sessionService;
    private final BusinessAccessService accessService;

    public CapacitySessionController(
            CapacitySessionService sessionService, BusinessAccessService accessService
    ) {
        this.sessionService = sessionService;
        this.accessService = accessService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CapacitySessionResponse create(
            @PathVariable Long businessId, @PathVariable Long locationId,
            @PathVariable Long serviceId,
            @Valid @RequestBody CreateCapacitySessionRequest request
    ) {
        accessService.requireManagementAccess(businessId, SecurityUtils.getCurrentUserEmail());
        return sessionService.create(businessId, locationId, serviceId, request);
    }

    @GetMapping
    public List<CapacitySessionResponse> find(
            @PathVariable Long businessId, @PathVariable Long locationId,
            @PathVariable Long serviceId
    ) {
        accessService.requireMembership(businessId, SecurityUtils.getCurrentUserEmail());
        return sessionService.find(businessId, locationId, serviceId);
    }

    @PostMapping("/{sessionId}/cancel")
    public CapacitySessionResponse cancel(
            @PathVariable Long businessId, @PathVariable Long sessionId
    ) {
        accessService.requireManagementAccess(businessId, SecurityUtils.getCurrentUserEmail());
        return sessionService.cancel(businessId, sessionId);
    }
}
