package com.bookify.backend.discovery.controller;

import com.bookify.backend.discovery.dto.NearbyBusinessResponse;
import com.bookify.backend.discovery.service.NearbyDiscoveryService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/discovery")
public class DiscoveryController {
    private final NearbyDiscoveryService discoveryService;

    public DiscoveryController(NearbyDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @GetMapping("/nearby")
    public List<NearbyBusinessResponse> nearby(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "10") double radiusKm,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(defaultValue = "0") BigDecimal minRating,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return discoveryService.findNearby(
                latitude, longitude, radiusKm, categoryCode, minRating, limit
        );
    }
}
