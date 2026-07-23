package com.bookify.backend.resource.dto;

import java.util.List;

public record ServiceResourceAssignmentResponse(Long serviceId, List<Long> resourceIds) {
}
