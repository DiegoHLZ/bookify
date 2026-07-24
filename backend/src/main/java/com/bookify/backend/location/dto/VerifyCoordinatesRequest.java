package com.bookify.backend.location.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyCoordinatesRequest(
        @NotBlank @Size(max = 100) String source
) {
}
