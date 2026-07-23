package com.bookify.backend.resource.dto;

import com.bookify.backend.resource.model.ResourceType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpsertResourceRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        @NotNull ResourceType type,
        @NotNull @Min(1) @Max(10000) Integer capacity
) {
}
