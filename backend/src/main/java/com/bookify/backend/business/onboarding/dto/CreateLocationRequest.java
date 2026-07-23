package com.bookify.backend.business.onboarding.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateLocationRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 250) String address,
        @NotBlank @Size(max = 120) String city,
        @NotBlank
        @Size(min = 2, max = 2)
        @Pattern(regexp = "^[A-Z]{2}$", message = "must be an uppercase ISO 3166-1 alpha-2 code")
        String countryCode,
        @NotBlank @Size(max = 60) String timezone,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") @Digits(integer = 2, fraction = 6)
        BigDecimal latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") @Digits(integer = 3, fraction = 6)
        BigDecimal longitude
) {
}
