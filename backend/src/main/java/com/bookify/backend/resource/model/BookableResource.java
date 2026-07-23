package com.bookify.backend.resource.model;

import com.bookify.backend.business.model.Business;
import com.bookify.backend.location.model.BusinessLocation;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "resources",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_resource_location_name",
                columnNames = {"location_id", "name"}
        )
)
public class BookableResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private BusinessLocation location;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ResourceType type;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected BookableResource() {
    }

    public BookableResource(
            Business business,
            BusinessLocation location,
            String name,
            String description,
            ResourceType type,
            Integer capacity
    ) {
        this.business = business;
        this.location = location;
        updateDetails(name, description, type, capacity);
    }

    public void updateDetails(
            String name,
            String description,
            ResourceType type,
            Integer capacity
    ) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.capacity = capacity;
    }

    public void setActive(boolean active) {
        this.active = active;
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

    public Long getId() { return id; }
    public Business getBusiness() { return business; }
    public BusinessLocation getLocation() { return location; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public ResourceType getType() { return type; }
    public Integer getCapacity() { return capacity; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
