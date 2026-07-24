package com.bookify.backend.booking.model;

import com.bookify.backend.business.model.Business;
import com.bookify.backend.business.model.ServiceOffering;
import com.bookify.backend.location.model.BusinessLocation;
import com.bookify.backend.resource.model.BookableResource;
import com.bookify.backend.user.model.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "bookings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_booking_customer_idempotency",
                columnNames = {"customer_id", "idempotency_key"}
        )
)
public class Booking {

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 500)
    private String notes;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Booking() {
    }

    public Booking(
            Business business,
            BusinessLocation location,
            ServiceOffering service,
            BookableResource resource,
            User customer,
            Instant startsAt,
            Instant endsAt,
            String notes,
            String idempotencyKey
    ) {
        this.business = business;
        this.location = location;
        this.service = service;
        this.resource = resource;
        this.customer = customer;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = BookingStatus.CONFIRMED;
        this.quantity = 1;
        this.notes = notes;
        this.idempotencyKey = idempotencyKey;
    }

    public void cancel(Instant cancelledAt) {
        if (status == BookingStatus.CANCELLED) {
            return;
        }
        if (status != BookingStatus.PENDING && status != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Only active bookings can be cancelled");
        }
        status = BookingStatus.CANCELLED;
        this.cancelledAt = cancelledAt;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Business getBusiness() { return business; }
    public BusinessLocation getLocation() { return location; }
    public ServiceOffering getService() { return service; }
    public BookableResource getResource() { return resource; }
    public User getCustomer() { return customer; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public BookingStatus getStatus() { return status; }
    public Integer getQuantity() { return quantity; }
    public String getNotes() { return notes; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getCancelledAt() { return cancelledAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
