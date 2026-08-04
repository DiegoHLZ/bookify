package com.bookify.backend.discovery.controller;

import com.bookify.backend.discovery.dto.DiscoverySearchPageResponse;
import com.bookify.backend.discovery.dto.NearbyBusinessResponse;
import com.bookify.backend.discovery.dto.PublicBusinessDetailResponse;
import com.bookify.backend.discovery.service.NearbyDiscoveryService;
import com.bookify.backend.discovery.semantic.dto.SemanticSearchPageResponse;
import com.bookify.backend.discovery.semantic.service.SemanticSearchService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/discovery")
public class DiscoveryController {
    private final NearbyDiscoveryService discoveryService;
    private final SemanticSearchService semanticSearchService;

    public DiscoveryController(
            NearbyDiscoveryService discoveryService,
            SemanticSearchService semanticSearchService
    ) {
        this.discoveryService = discoveryService;
        this.semanticSearchService = semanticSearchService;
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

    @GetMapping("/search")
    public DiscoverySearchPageResponse search(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "10") double radiusKm,
            @RequestParam(required = false) String text,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(defaultValue = "0") BigDecimal minRating,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime availableAt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return discoveryService.search(
                latitude, longitude, radiusKm, text, categoryCode,
                minRating, availableAt, page, size
        );
    }

    @GetMapping("/semantic-search")
    public SemanticSearchPageResponse semanticSearch(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "10") double radiusKm,
            @RequestParam(required = false) String text,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(defaultValue = "0") BigDecimal minRating,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime availableAt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return semanticSearchService.search(
                latitude, longitude, radiusKm, text, categoryCode,
                minRating, availableAt, page, size
        );
    }

    @GetMapping("/businesses/{slug}")
    public PublicBusinessDetailResponse business(@PathVariable String slug) {
        return discoveryService.findPublicBusiness(slug);
    }
}
