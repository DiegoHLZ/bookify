package com.bookify.backend.tenancy.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeMemberStatusRequest(@NotNull Boolean active) {
}
