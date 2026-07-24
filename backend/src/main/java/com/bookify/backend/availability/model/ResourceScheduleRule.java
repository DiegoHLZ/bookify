package com.bookify.backend.availability.model;

import com.bookify.backend.business.model.Business;
import com.bookify.backend.location.model.BusinessLocation;
import com.bookify.backend.resource.model.BookableResource;
import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "resource_schedule_rules")
public class ResourceScheduleRule {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 20)
    private ScheduleRuleType ruleType;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ResourceScheduleRule() {
    }

    public ResourceScheduleRule(
            Business business,
            BookableResource resource,
            DayOfWeek dayOfWeek,
            ScheduleRuleType ruleType,
            LocalTime startTime,
            LocalTime endTime
    ) {
        this.business = business;
        this.location = resource.getLocation();
        this.resource = resource;
        this.dayOfWeek = dayOfWeek;
        this.ruleType = ruleType;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Business getBusiness() { return business; }
    public BusinessLocation getLocation() { return location; }
    public BookableResource getResource() { return resource; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public ScheduleRuleType getRuleType() { return ruleType; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
}
