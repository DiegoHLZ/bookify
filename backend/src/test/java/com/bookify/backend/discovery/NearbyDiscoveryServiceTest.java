package com.bookify.backend.discovery;

import com.bookify.backend.common.exception.BadRequestException;
import com.bookify.backend.discovery.dto.NearbyLocationProjection;
import com.bookify.backend.discovery.service.NearbyDiscoveryService;
import com.bookify.backend.location.repository.BusinessLocationRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NearbyDiscoveryServiceTest {
    private final BusinessLocationRepository repository =
            mock(BusinessLocationRepository.class);
    private final NearbyDiscoveryService service = new NearbyDiscoveryService(repository);

    @Test
    void normalizesFiltersAndMapsRankedResults() {
        NearbyLocationProjection row = mock(NearbyLocationProjection.class);
        when(row.getBusinessId()).thenReturn(1L);
        when(row.getBusinessName()).thenReturn("Centro");
        when(row.getCategoryCode()).thenReturn("PROFESSIONAL_SERVICES");
        when(row.getRatingAverage()).thenReturn(new BigDecimal("4.75"));
        when(row.getRatingCount()).thenReturn(20);
        when(row.getLocationId()).thenReturn(2L);
        when(row.getLocationName()).thenReturn("Miraflores");
        when(row.getLatitude()).thenReturn(new BigDecimal("-12.12"));
        when(row.getLongitude()).thenReturn(new BigDecimal("-77.03"));
        when(row.getDistanceMeters()).thenReturn(850.5);
        when(repository.findVerifiedNearby(
                anyDouble(), anyDouble(), anyDouble(), any(), any(), anyInt()
        )).thenReturn(List.of(row));

        var results = service.findNearby(
                -12.1, -77.0, 5, " professional_services ",
                new BigDecimal("4"), 10
        );

        assertEquals(1, results.size());
        assertEquals(850.5, results.get(0).distanceMeters());
        verify(repository).findVerifiedNearby(
                -12.1, -77.0, 5000, "PROFESSIONAL_SERVICES",
                new BigDecimal("4"), 10
        );
    }

    @Test
    void rejectsUnsafeBoundsBeforeQueryingDatabase() {
        assertThrows(BadRequestException.class, () ->
                service.findNearby(91, 0, 10, null, BigDecimal.ZERO, 20));
        assertThrows(BadRequestException.class, () ->
                service.findNearby(Double.NaN, 0, 10, null, BigDecimal.ZERO, 20));
        assertThrows(BadRequestException.class, () ->
                service.findNearby(0, 0, Double.POSITIVE_INFINITY,
                        null, BigDecimal.ZERO, 20));
        assertThrows(BadRequestException.class, () ->
                service.findNearby(0, 0, 101, null, BigDecimal.ZERO, 20));
        assertThrows(BadRequestException.class, () ->
                service.findNearby(0, 0, 10, null, new BigDecimal("5.1"), 20));
        assertThrows(BadRequestException.class, () ->
                service.findNearby(0, 0, 10, null, BigDecimal.ZERO, 101));
        verifyNoInteractions(repository);
    }
}
