package com.bookify.backend.availability.model;

import com.bookify.backend.business.model.Business;
import com.bookify.backend.location.model.BusinessLocation;
import com.bookify.backend.resource.model.BookableResource;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
        name = "resource_schedule_exceptions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_resource_exception_date",
                columnNames = {"resource_id", "exception_date"}
        )
)
public class ResourceScheduleException {

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
    @JoinColumn(name = "resource_id", nullable = false)
    private BookableResource resource;

    @Column(name = "exception_date", nullable = false)
    private LocalDate exceptionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "exception_type", nullable = false, length = 20)
    private ScheduleExceptionType exceptionType;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(length = 250)
    private String reason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected ResourceScheduleException() {
    }

    public ResourceScheduleException(
            Business business,
            BookableResource resource,
            LocalDate exceptionDate
    ) {
        this.business = business;
        this.location = resource.getLocation();
        this.resource = resource;
        this.exceptionDate = exceptionDate;
    }

    public void update(
            ScheduleExceptionType type,
            LocalTime startTime,
            LocalTime endTime,
            String reason
    ) {
        this.exceptionType = type;
        this.startTime = startTime;
        this.endTime = endTime;
        this.reason = reason;
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
    public BookableResource getResource() { return resource; }
    public LocalDate getExceptionDate() { return exceptionDate; }
    public ScheduleExceptionType getExceptionType() { return exceptionType; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getReason() { return reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
