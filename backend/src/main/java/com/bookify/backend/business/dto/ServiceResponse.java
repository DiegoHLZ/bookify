package com.bookify.backend.business.dto;

import com.bookify.backend.business.model.BookingMode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ServiceResponse {

    private Long id;
    private String name;
    private String description;
    private Integer durationMinutes;
    private BigDecimal price;
    private String currency;
    private boolean active;
    private BookingMode bookingMode;
    private Long businessId;
    private List<Long> locationIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ServiceResponse() {
    }

    public ServiceResponse(Long id, String name, String description, Integer durationMinutes,
                           BigDecimal price, String currency, boolean active,
                           BookingMode bookingMode, Long businessId,
                           List<Long> locationIds,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.price = price;
        this.currency = currency;
        this.active = active;
        this.bookingMode = bookingMode;
        this.businessId = businessId;
        this.locationIds = locationIds;
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

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public boolean isActive() {
        return active;
    }

    public BookingMode getBookingMode() {
        return bookingMode;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public List<Long> getLocationIds() {
        return locationIds;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
