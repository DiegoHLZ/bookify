package com.bookify.backend.resource.model;

import com.bookify.backend.business.model.Business;
import com.bookify.backend.business.model.ServiceOffering;
import com.bookify.backend.location.model.BusinessLocation;
import jakarta.persistence.*;

@Entity
@Table(
        name = "offering_resources",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_offering_resource",
                columnNames = {"service_id", "resource_id"}
        )
)
public class OfferingResource {

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id", nullable = false)
    private BookableResource resource;

    protected OfferingResource() {
    }

    public OfferingResource(
            Business business,
            ServiceOffering service,
            BookableResource resource
    ) {
        this.business = business;
        this.service = service;
        this.location = resource.getLocation();
        this.resource = resource;
    }

    public Long getId() { return id; }
    public Business getBusiness() { return business; }
    public ServiceOffering getService() { return service; }
    public BusinessLocation getLocation() { return location; }
    public BookableResource getResource() { return resource; }
}
