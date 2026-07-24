package com.bookify.backend.discovery;

import com.bookify.backend.availability.dto.AvailabilitySlotResponse;
import com.bookify.backend.availability.dto.ServiceAvailabilityResponse;
import com.bookify.backend.availability.service.AvailabilitySlotService;
import com.bookify.backend.business.model.Business;
import com.bookify.backend.business.model.ServiceOffering;
import com.bookify.backend.business.repository.BusinessRepository;
import com.bookify.backend.business.repository.OfferingLocationRepository;
import com.bookify.backend.business.repository.ServiceOfferingRepository;
import com.bookify.backend.common.exception.BadRequestException;
import com.bookify.backend.discovery.dto.NearbyLocationProjection;
import com.bookify.backend.discovery.service.NearbyDiscoveryService;
import com.bookify.backend.location.model.BusinessLocation;
import com.bookify.backend.location.repository.BusinessLocationRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NearbyDiscoveryServiceTest {
    private final BusinessLocationRepository locationRepository =
            mock(BusinessLocationRepository.class);
    private final BusinessRepository businessRepository = mock(BusinessRepository.class);
    private final ServiceOfferingRepository serviceRepository =
            mock(ServiceOfferingRepository.class);
    private final OfferingLocationRepository offeringLocationRepository =
            mock(OfferingLocationRepository.class);
    private final AvailabilitySlotService availabilityService =
            mock(AvailabilitySlotService.class);
    private final NearbyDiscoveryService service = new NearbyDiscoveryService(
            locationRepository, businessRepository, serviceRepository,
            offeringLocationRepository, availabilityService
    );

    @Test
    void normalizesTextAndReturnsStablePage() {
        NearbyLocationProjection first = row(1L, 11L, 100.0);
        NearbyLocationProjection second = row(2L, 12L, 200.0);
        when(locationRepository.searchVerifiedNearby(
                anyDouble(), anyDouble(), anyDouble(), any(), any(), any(),
                anyInt(), anyInt()
        )).thenReturn(List.of(first, second));

        var result = service.search(
                -12.1, -77.0, 5, "  corte   clásico ", " professional_services ",
                new BigDecimal("4"), null, 0, 1
        );

        assertEquals(1, result.items().size());
        assertTrue(result.hasNext());
        verify(locationRepository).searchVerifiedNearby(
                -12.1, -77.0, 5000, "PROFESSIONAL_SERVICES",
                new BigDecimal("4"), "corte clásico", 0, 2
        );
    }

    @Test
    void filtersByExactLocalAvailabilityAndReturnsServiceIds() {
        LocalDateTime requestedAt = LocalDateTime.of(2026, 8, 3, 10, 0);
        NearbyLocationProjection row = row(1L, 11L, 100.0);
        when(locationRepository.searchVerifiedNearby(
                anyDouble(), anyDouble(), anyDouble(), any(), any(), any(),
                anyInt(), anyInt()
        )).thenReturn(List.of(row));
        when(offeringLocationRepository.findActiveServiceIdsAtLocation(1L, 11L))
                .thenReturn(List.of(21L, 22L));
        when(availabilityService.findAvailability(
                eq(1L), eq(11L), eq(21L), any(), any(), eq(5)
        )).thenReturn(availability(21L, List.of()));
        when(availabilityService.findAvailability(
                eq(1L), eq(11L), eq(22L), any(), any(), eq(5)
        )).thenReturn(availability(22L, List.of(new AvailabilitySlotResponse(
                31L, "Ana", null, null, null, requestedAt,
                requestedAt.plusMinutes(30), null, null
        ))));

        var result = service.search(
                -12.1, -77.0, 5, null, null,
                BigDecimal.ZERO, requestedAt, 0, 10
        );

        assertEquals(List.of(22L), result.items().get(0).availableServiceIds());
        assertEquals(requestedAt, result.items().get(0).requestedAt());
    }

    @Test
    void publicDetailExcludesUnverifiedLocationsAndUnassignedServices() {
        Business business = mock(Business.class);
        when(business.getId()).thenReturn(1L);
        when(business.getSlug()).thenReturn("studio-norte");
        when(business.getName()).thenReturn("Studio Norte");
        when(business.getRatingAverage()).thenReturn(new BigDecimal("4.50"));
        when(business.getRatingCount()).thenReturn(10);
        BusinessLocation location = mock(BusinessLocation.class);
        when(location.getId()).thenReturn(11L);
        ServiceOffering visible = mock(ServiceOffering.class);
        when(visible.getId()).thenReturn(21L);
        ServiceOffering hidden = mock(ServiceOffering.class);
        when(hidden.getId()).thenReturn(22L);

        when(businessRepository.findBySlugAndActiveTrue("studio-norte"))
                .thenReturn(Optional.of(business));
        when(locationRepository
                .findByBusinessIdAndActiveTrueAndCoordinatesVerifiedTrueOrderByIdAsc(1L))
                .thenReturn(List.of(location));
        when(serviceRepository.findByBusinessIdAndActiveTrueOrderByNameAscIdAsc(1L))
                .thenReturn(List.of(visible, hidden));
        when(offeringLocationRepository.findLocationIdsByServiceId(21L))
                .thenReturn(List.of(11L));
        when(offeringLocationRepository.findLocationIdsByServiceId(22L))
                .thenReturn(List.of(99L));

        var detail = service.findPublicBusiness(" STUDIO-NORTE ");

        assertEquals(1, detail.locations().size());
        assertEquals(1, detail.services().size());
        assertEquals(21L, detail.services().get(0).id());
    }

    @Test
    void rejectsUnsafeBoundsBeforeQueryingDatabase() {
        assertThrows(BadRequestException.class, () ->
                service.search(91, 0, 10, null, null, BigDecimal.ZERO, null, 0, 20));
        assertThrows(BadRequestException.class, () ->
                service.search(0, 0, 10, "x".repeat(101), null,
                        BigDecimal.ZERO, null, 0, 20));
        assertThrows(BadRequestException.class, () ->
                service.search(0, 0, 10, null, null, BigDecimal.ZERO, null, -1, 20));
        assertThrows(BadRequestException.class, () ->
                service.search(0, 0, 10, null, null, BigDecimal.ZERO, null, 0, 101));
        verifyNoInteractions(locationRepository);
    }

    private NearbyLocationProjection row(Long businessId, Long locationId, double distance) {
        NearbyLocationProjection row = mock(NearbyLocationProjection.class);
        when(row.getBusinessId()).thenReturn(businessId);
        when(row.getBusinessName()).thenReturn("Centro " + businessId);
        when(row.getCategoryCode()).thenReturn("PROFESSIONAL_SERVICES");
        when(row.getRatingAverage()).thenReturn(new BigDecimal("4.75"));
        when(row.getRatingCount()).thenReturn(20);
        when(row.getLocationId()).thenReturn(locationId);
        when(row.getLocationName()).thenReturn("Miraflores");
        when(row.getTimezone()).thenReturn("America/Lima");
        when(row.getLatitude()).thenReturn(new BigDecimal("-12.12"));
        when(row.getLongitude()).thenReturn(new BigDecimal("-77.03"));
        when(row.getDistanceMeters()).thenReturn(distance);
        return row;
    }

    private ServiceAvailabilityResponse availability(
            Long serviceId,
            List<AvailabilitySlotResponse> slots
    ) {
        LocalDate date = LocalDate.of(2026, 8, 3);
        return new ServiceAvailabilityResponse(
                1L, 11L, serviceId, 30, 15, "America/Lima", date, date, slots
        );
    }
}
