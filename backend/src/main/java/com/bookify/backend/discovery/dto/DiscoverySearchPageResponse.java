package com.bookify.backend.discovery.dto;

import java.util.List;

public record DiscoverySearchPageResponse(
        int page,
        int size,
        boolean hasNext,
        List<DiscoverySearchItemResponse> items
) {
}
