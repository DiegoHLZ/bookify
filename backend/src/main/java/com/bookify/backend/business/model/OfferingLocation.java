package com.bookify.backend.business.model;

import com.bookify.backend.location.model.BusinessLocation;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "offering_locations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_offering_location",
                columnNames = {"service_id", "location_id"}
        )
)
public class OfferingLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceOffering service;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private BusinessLocation location;

    protected OfferingLocation() {
    }

    public OfferingLocation(
            Business business,
            ServiceOffering service,
            BusinessLocation location
    ) {
        this.business = business;
        this.service = service;
        this.location = location;
    }

    public Long getId() {
        return id;
    }

    public Business getBusiness() {
        return business;
    }

    public ServiceOffering getService() {
        return service;
    }

    public BusinessLocation getLocation() {
        return location;
    }
}
