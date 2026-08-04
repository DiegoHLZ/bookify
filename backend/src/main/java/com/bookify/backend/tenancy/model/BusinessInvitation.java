package com.bookify.backend.tenancy.model;

import com.bookify.backend.business.model.Business;
import com.bookify.backend.user.model.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "business_invitations")
public class BusinessInvitation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(nullable = false, length = 320)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MembershipRole role;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_by_user_id", nullable = false)
    private User invitedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_by_user_id")
    private User acceptedBy;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BusinessInvitation() {
    }

    public BusinessInvitation(
            Business business,
            String email,
            MembershipRole role,
            String tokenHash,
            Instant expiresAt,
            User invitedBy
    ) {
        this.business = business;
        this.email = email;
        this.role = role;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.invitedBy = invitedBy;
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

    public void expire() {
        status = InvitationStatus.EXPIRED;
    }

    public void revoke() {
        status = InvitationStatus.REVOKED;
    }

    public void accept(User user, Instant acceptedAt) {
        status = InvitationStatus.ACCEPTED;
        acceptedBy = user;
        this.acceptedAt = acceptedAt;
    }

    public Long getId() { return id; }
    public Business getBusiness() { return business; }
    public String getEmail() { return email; }
    public MembershipRole getRole() { return role; }
    public String getTokenHash() { return tokenHash; }
    public InvitationStatus getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public User getInvitedBy() { return invitedBy; }
    public User getAcceptedBy() { return acceptedBy; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
