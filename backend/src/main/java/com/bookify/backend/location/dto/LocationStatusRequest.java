package com.bookify.backend.location.dto;

import jakarta.validation.constraints.NotNull;

public record LocationStatusRequest(@NotNull Boolean active) {
}
