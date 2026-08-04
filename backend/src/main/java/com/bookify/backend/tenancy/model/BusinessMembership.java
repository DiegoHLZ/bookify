package com.bookify.backend.tenancy.model;

import com.bookify.backend.business.model.Business;
import com.bookify.backend.user.model.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "business_memberships",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_membership_business_user",
                columnNames = {"business_id", "user_id"}
        )
)
public class BusinessMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipRole role;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected BusinessMembership() {
    }

    public BusinessMembership(Business business, User user, MembershipRole role) {
        this.business = business;
        this.user = user;
        this.role = role;
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

    public User getUser() {
        return user;
    }

    public MembershipRole getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public void changeRole(MembershipRole role) {
        this.role = role;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
