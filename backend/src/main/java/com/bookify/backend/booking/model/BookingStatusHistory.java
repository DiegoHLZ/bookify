package com.bookify.backend.booking.model;

import com.bookify.backend.business.model.Business;
import com.bookify.backend.user.model.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "booking_status_history")
public class BookingStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private BookingStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private BookingStatus toStatus;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BookingStatusHistory() {
    }

    public BookingStatusHistory(
            Booking booking,
            User actor,
            BookingStatus fromStatus,
            BookingStatus toStatus,
            String reason
    ) {
        this.business = booking.getBusiness();
        this.booking = booking;
        this.actor = actor;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Business getBusiness() { return business; }
    public Booking getBooking() { return booking; }
    public User getActor() { return actor; }
    public BookingStatus getFromStatus() { return fromStatus; }
    public BookingStatus getToStatus() { return toStatus; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}
