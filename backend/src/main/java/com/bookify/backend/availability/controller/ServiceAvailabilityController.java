package com.bookify.backend.availability.controller;

import com.bookify.backend.availability.dto.ServiceAvailabilityResponse;
import com.bookify.backend.availability.service.AvailabilitySlotService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping(
        "/api/v1/businesses/{businessId}/locations/{locationId}/services/{serviceId}"
)
public class ServiceAvailabilityController {

    private final AvailabilitySlotService availabilityService;

    public ServiceAvailabilityController(AvailabilitySlotService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping("/availability")
    public ServiceAvailabilityResponse findAvailability(
            @PathVariable Long businessId,
            @PathVariable Long locationId,
            @PathVariable Long serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "15") Integer intervalMinutes
    ) {
        return availabilityService.findAvailability(
                businessId, locationId, serviceId, from, to, intervalMinutes
        );
    }
}
