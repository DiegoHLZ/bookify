package com.bookify.backend.discovery.semantic.dto;

import java.util.List;

public record SemanticSearchPageResponse(
        int page,
        int size,
        boolean hasNext,
        String mode,
        String provider,
        String fallbackReason,
        long semanticLatencyMs,
        List<SemanticSearchItemResponse> items
) {
}
