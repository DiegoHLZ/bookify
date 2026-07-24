package com.bookify.backend.capacity.model;

import com.bookify.backend.business.model.Business;
import com.bookify.backend.business.model.ServiceOffering;
import com.bookify.backend.location.model.BusinessLocation;
import com.bookify.backend.resource.model.BookableResource;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "capacity_sessions")
public class CapacitySession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private BusinessLocation location;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceOffering service;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id", nullable = false)
    private BookableResource resource;
    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;
    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;
    @Column(name = "capacity_total", nullable = false)
    private Integer capacityTotal;
    @Column(name = "capacity_reserved", nullable = false)
    private Integer capacityReserved;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CapacitySessionStatus status;
    @Version
    @Column(nullable = false)
    private Long version;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CapacitySession() {}

    public CapacitySession(
            Business business, BusinessLocation location, ServiceOffering service,
            BookableResource resource, Instant startsAt, Instant endsAt, int capacityTotal
    ) {
        this.business = business;
        this.location = location;
        this.service = service;
        this.resource = resource;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.capacityTotal = capacityTotal;
        this.capacityReserved = 0;
        this.status = CapacitySessionStatus.OPEN;
    }

    public void reserve(int quantity) {
        if (status != CapacitySessionStatus.OPEN
                || capacityReserved + quantity > capacityTotal) {
            throw new IllegalStateException("Session does not have enough capacity");
        }
        capacityReserved += quantity;
    }

    public void release(int quantity) {
        capacityReserved = Math.max(0, capacityReserved - quantity);
    }

    public void cancel() {
        if (capacityReserved > 0) {
            throw new IllegalStateException("Cannot cancel a session with active reservations");
        }
        status = CapacitySessionStatus.CANCELLED;
    }

    @PrePersist void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Business getBusiness() { return business; }
    public BusinessLocation getLocation() { return location; }
    public ServiceOffering getService() { return service; }
    public BookableResource getResource() { return resource; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public Integer getCapacityTotal() { return capacityTotal; }
    public Integer getCapacityReserved() { return capacityReserved; }
    public int getRemainingCapacity() { return capacityTotal - capacityReserved; }
    public CapacitySessionStatus getStatus() { return status; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
