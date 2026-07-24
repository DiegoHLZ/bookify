package com.bookify.backend.review.model;

import com.bookify.backend.booking.model.Booking;
import com.bookify.backend.business.model.Business;
import com.bookify.backend.user.model.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "reviews",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_review_booking", columnNames = "booking_id"
        )
)
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Column(nullable = false)
    private Integer score;

    @Column(length = 1000)
    private String comment;

    @Column(nullable = false)
    private boolean verified = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Review() {}

    public Review(Booking booking, String comment, int score) {
        this.business = booking.getBusiness();
        this.booking = booking;
        this.customer = booking.getCustomer();
        this.comment = comment;
        this.score = score;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public Business getBusiness() { return business; }
    public Booking getBooking() { return booking; }
    public User getCustomer() { return customer; }
    public Integer getScore() { return score; }
    public String getComment() { return comment; }
    public boolean isVerified() { return verified; }
    public Instant getCreatedAt() { return createdAt; }
}
