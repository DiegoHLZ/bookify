package com.bookify.backend.discovery.semantic.dto;

import com.bookify.backend.discovery.dto.DiscoverySearchItemResponse;

public record SemanticSearchItemResponse(
        DiscoverySearchItemResponse result,
        Double semanticScore
) {
}
