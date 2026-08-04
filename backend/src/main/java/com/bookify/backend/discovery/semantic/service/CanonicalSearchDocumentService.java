package com.bookify.backend.discovery.semantic.service;

import com.bookify.backend.business.repository.CanonicalSearchTextProjection;
import com.bookify.backend.business.repository.OfferingLocationRepository;
import com.bookify.backend.discovery.dto.DiscoverySearchItemResponse;
import com.bookify.backend.discovery.semantic.model.SemanticDocument;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CanonicalSearchDocumentService {
    private final OfferingLocationRepository offeringLocationRepository;

    public CanonicalSearchDocumentService(
            OfferingLocationRepository offeringLocationRepository
    ) {
        this.offeringLocationRepository = offeringLocationRepository;
    }

    public Map<Long, SemanticDocument> build(
            List<DiscoverySearchItemResponse> candidates
    ) {
        if (candidates.isEmpty()) {
            return Map.of();
        }
        Collection<Long> locationIds = candidates.stream()
                .map(DiscoverySearchItemResponse::locationId)
                .toList();
        Map<Long, List<CanonicalSearchTextProjection>> canonicalText =
                offeringLocationRepository.findCanonicalSearchText(locationIds).stream()
                        .collect(Collectors.groupingBy(
                                CanonicalSearchTextProjection::getLocationId,
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));
        return candidates.stream().map(candidate -> {
            List<String> parts = new ArrayList<>(List.of(
                    safe(candidate.businessName()), safe(candidate.categoryCode()),
                    safe(candidate.locationName()), safe(candidate.address()),
                    safe(candidate.city()), safe(candidate.countryCode())
            ));
            for (CanonicalSearchTextProjection row : canonicalText.getOrDefault(
                    candidate.locationId(), List.of()
            )) {
                parts.add(safe(row.getBusinessDescription()));
                parts.add(safe(row.getServiceName()));
                parts.add(safe(row.getServiceDescription()));
            }
            String document = parts.stream()
                    .filter(value -> !value.isBlank())
                    .distinct()
                    .collect(Collectors.joining(" "));
            return new SemanticDocument(candidate.locationId(), document);
        }).collect(Collectors.toMap(
                SemanticDocument::locationId,
                Function.identity(),
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
