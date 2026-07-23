package com.bookify.backend.resource.dto;

import com.bookify.backend.resource.model.BookableResource;
import com.bookify.backend.resource.model.ResourceType;

import java.time.LocalDateTime;

public record BookableResourceResponse(
        Long id,
        Long businessId,
        Long locationId,
        String name,
        String description,
        ResourceType type,
        Integer capacity,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static BookableResourceResponse from(BookableResource resource) {
        return new BookableResourceResponse(
                resource.getId(),
                resource.getBusiness().getId(),
                resource.getLocation().getId(),
                resource.getName(),
                resource.getDescription(),
                resource.getType(),
                resource.getCapacity(),
                resource.isActive(),
                resource.getCreatedAt(),
                resource.getUpdatedAt()
        );
    }
}
