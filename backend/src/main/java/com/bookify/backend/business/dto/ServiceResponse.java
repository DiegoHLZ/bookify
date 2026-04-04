package com.bookify.backend.business.dto;

import java.time.LocalDateTime;

public class ServiceResponse {

    private Long id;
    private String name;
    private String description;
    private Integer durationMinutes;
    private Double price;
    private String currency;
    private boolean active;
    private Long businessId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ServiceResponse() {
    }

    public ServiceResponse(Long id, String name, String description, Integer durationMinutes,
                           Double price, String currency, boolean active, Long businessId,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.price = price;
        this.currency = currency;
        this.active = active;
        this.businessId = businessId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
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

    public boolean isActive() {
        return active;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
