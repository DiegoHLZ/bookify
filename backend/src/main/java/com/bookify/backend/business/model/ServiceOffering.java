package com.bookify.backend.business.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "services")
public class ServiceOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private Integer durationMinutes;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_mode", nullable = false, length = 30)
    private BookingMode bookingMode = BookingMode.EXCLUSIVE_RESOURCE;

    @Column(name = "customer_cancellation_allowed", nullable = false)
    private boolean customerCancellationAllowed = true;

    @Column(name = "cancellation_notice_minutes", nullable = false)
    private Integer cancellationNoticeMinutes = 0;

    @Column(name = "customer_reschedule_allowed", nullable = false)
    private boolean customerRescheduleAllowed = true;

    @Column(name = "reschedule_notice_minutes", nullable = false)
    private Integer rescheduleNoticeMinutes = 0;

    @Column(name = "max_reschedules", nullable = false)
    private Integer maxReschedules = 1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public ServiceOffering() {
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

    public boolean isCustomerCancellationAllowed() { return customerCancellationAllowed; }
    public Integer getCancellationNoticeMinutes() { return cancellationNoticeMinutes; }
    public boolean isCustomerRescheduleAllowed() { return customerRescheduleAllowed; }
    public Integer getRescheduleNoticeMinutes() { return rescheduleNoticeMinutes; }
    public Integer getMaxReschedules() { return maxReschedules; }

    public Business getBusiness() {
        return business;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
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

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setBookingMode(BookingMode bookingMode) {
        this.bookingMode = bookingMode;
    }

    public void setCustomerCancellationAllowed(boolean value) {
        this.customerCancellationAllowed = value;
    }

    public void setCancellationNoticeMinutes(Integer value) {
        this.cancellationNoticeMinutes = value;
    }

    public void setCustomerRescheduleAllowed(boolean value) {
        this.customerRescheduleAllowed = value;
    }

    public void setRescheduleNoticeMinutes(Integer value) {
        this.rescheduleNoticeMinutes = value;
    }

    public void setMaxReschedules(Integer value) {
        this.maxReschedules = value;
    }

    public void setBusiness(Business business) {
        this.business = business;
    }
}
