package com.bookify.backend.resource.dto;

import jakarta.validation.constraints.NotNull;

public record ResourceStatusRequest(@NotNull Boolean active) {
}
