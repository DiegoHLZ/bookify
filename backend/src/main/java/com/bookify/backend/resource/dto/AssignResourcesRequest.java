package com.bookify.backend.resource.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record AssignResourcesRequest(@NotNull Set<@NotNull Long> resourceIds) {
}
