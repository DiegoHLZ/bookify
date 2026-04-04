package com.bookify.backend.business.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UpdateServiceRequest {

    @NotBlank
    private String name;

    private String description;

    @NotNull
    @Min(1)
    private Integer durationMinutes;

    @NotNull
    @Min(0)
    private Double price;

    @NotBlank
    private String currency;

    @NotNull
    private Boolean active;

    public UpdateServiceRequest() {
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

    public Double getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public Boolean getActive() {
        return active;
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

    public void setPrice(Double price) {
        this.price = price;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
