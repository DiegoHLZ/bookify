package com.bookify.backend.location.model;

import com.bookify.backend.business.model.Business;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Instant;

@Entity
@Table(
        name = "business_locations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_business_location_name",
                columnNames = {"business_id", "name"}
        )
)
public class BusinessLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 250)
    private String address;

    @Column(nullable = false, length = 120)
    private String city;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(nullable = false, length = 60)
    private String timezone;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "coordinates_verified", nullable = false)
    private boolean coordinatesVerified = false;

    @Column(name = "coordinates_verified_at")
    private Instant coordinatesVerifiedAt;

    @Column(name = "coordinate_source", length = 100)
    private String coordinateSource;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected BusinessLocation() {
    }

    public BusinessLocation(
            Business business,
            String name,
            String address,
            String city,
            String countryCode,
            String timezone,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        this.business = business;
        this.name = name;
        this.address = address;
        this.city = city;
        this.countryCode = countryCode;
        this.timezone = timezone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.coordinatesVerified = false;
        this.coordinatesVerifiedAt = null;
        this.coordinateSource = null;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Business getBusiness() {
        return business;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getTimezone() {
        return timezone;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public boolean isActive() {
        return active;
    }

    public void updateDetails(
            String name,
            String address,
            String city,
            String countryCode,
            String timezone,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        this.name = name;
        this.address = address;
        this.city = city;
        this.countryCode = countryCode;
        this.timezone = timezone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.coordinatesVerified = false;
        this.coordinatesVerifiedAt = null;
        this.coordinateSource = null;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void verifyCoordinates(String source, Instant verifiedAt) {
        this.coordinatesVerified = true;
        this.coordinateSource = source;
        this.coordinatesVerifiedAt = verifiedAt;
    }

    public boolean isCoordinatesVerified() { return coordinatesVerified; }
    public Instant getCoordinatesVerifiedAt() { return coordinatesVerifiedAt; }
    public String getCoordinateSource() { return coordinateSource; }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
