package com.bookify.backend.business.onboarding.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateBusinessRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank
        @Size(min = 3, max = 100)
        @Pattern(
                regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                message = "must use lowercase letters, numbers and hyphens"
        )
        String slug,
        @NotBlank
        @Size(max = 50)
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "must be a valid category code")
        String categoryCode,
        @Size(max = 1000) String description,
        @Size(max = 30) String phone,
        @Email @Size(max = 150) String email,
        @NotNull @Valid CreateLocationRequest location
) {
}
