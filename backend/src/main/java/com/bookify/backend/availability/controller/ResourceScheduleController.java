package com.bookify.backend.availability.controller;

import com.bookify.backend.availability.dto.*;
import com.bookify.backend.availability.service.ResourceScheduleService;
import com.bookify.backend.common.SecurityUtils;
import com.bookify.backend.tenancy.service.BusinessAccessService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/locations/{locationId}/resources/{resourceId}")
public class ResourceScheduleController {

    private final ResourceScheduleService scheduleService;
    private final BusinessAccessService accessService;

    public ResourceScheduleController(
            ResourceScheduleService scheduleService,
            BusinessAccessService accessService
    ) {
        this.scheduleService = scheduleService;
        this.accessService = accessService;
    }

    @GetMapping("/schedule")
    public ResourceScheduleResponse findSchedule(
            @PathVariable Long businessId,
            @PathVariable Long locationId,
            @PathVariable Long resourceId
    ) {
        requireMembership(businessId);
        return scheduleService.findSchedule(businessId, locationId, resourceId);
    }

    @PutMapping("/schedule")
    public ResourceScheduleResponse replaceSchedule(
            @PathVariable Long businessId,
            @PathVariable Long locationId,
            @PathVariable Long resourceId,
            @Valid @RequestBody ReplaceScheduleRequest request
    ) {
        requireManagement(businessId);
        return scheduleService.replaceSchedule(
                businessId, locationId, resourceId, request.rules()
        );
    }

    @GetMapping("/exceptions")
    public List<ScheduleExceptionResponse> findExceptions(
            @PathVariable Long businessId,
            @PathVariable Long locationId,
            @PathVariable Long resourceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        requireMembership(businessId);
        return scheduleService.findExceptions(
                businessId, locationId, resourceId, from, to
        );
    }

    @PutMapping("/exceptions/{date}")
    public ScheduleExceptionResponse upsertException(
            @PathVariable Long businessId,
            @PathVariable Long locationId,
            @PathVariable Long resourceId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody UpsertScheduleExceptionRequest request
    ) {
        requireManagement(businessId);
        return scheduleService.upsertException(
                businessId, locationId, resourceId, date, request
        );
    }

    @DeleteMapping("/exceptions/{date}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteException(
            @PathVariable Long businessId,
            @PathVariable Long locationId,
            @PathVariable Long resourceId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        requireManagement(businessId);
        scheduleService.deleteException(businessId, locationId, resourceId, date);
    }

    private void requireMembership(Long businessId) {
        accessService.requireMembership(businessId, SecurityUtils.getCurrentUserEmail());
    }

    private void requireManagement(Long businessId) {
        accessService.requireManagementAccess(businessId, SecurityUtils.getCurrentUserEmail());
    }
}
