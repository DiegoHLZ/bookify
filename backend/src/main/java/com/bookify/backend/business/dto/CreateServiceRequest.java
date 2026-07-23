package com.bookify.backend.business.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Set;

public class CreateServiceRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Max(value = 1440, message = "Duration must not exceed 1440 minutes")
    private Integer durationMinutes;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be greater than or equal to 0")
    @Digits(integer = 17, fraction = 2, message = "Price must have at most 2 decimal places")
    private BigDecimal price;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "PEN|USD|EUR", message = "Currency must be PEN, USD or EUR")
    private String currency;

    @NotEmpty(message = "At least one location is required")
    private Set<@NotNull Long> locationIds;

    public CreateServiceRequest() {
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public Set<Long> getLocationIds() {
        return locationIds;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setLocationIds(Set<Long> locationIds) {
        this.locationIds = locationIds;
    }
}
