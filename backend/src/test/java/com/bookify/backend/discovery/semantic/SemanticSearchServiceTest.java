package com.bookify.backend.discovery.semantic;

import com.bookify.backend.discovery.dto.DiscoverySearchItemResponse;
import com.bookify.backend.discovery.dto.DiscoverySearchPageResponse;
import com.bookify.backend.discovery.semantic.adapter.MockEmbeddingAdapter;
import com.bookify.backend.discovery.semantic.adapter.MockRerankingAdapter;
import com.bookify.backend.discovery.semantic.model.SemanticDocument;
import com.bookify.backend.discovery.semantic.port.EmbeddingPort;
import com.bookify.backend.discovery.semantic.port.RerankingPort;
import com.bookify.backend.discovery.semantic.model.RankedSemanticCandidate;
import com.bookify.backend.discovery.semantic.model.EmbeddingInputType;
import com.bookify.backend.discovery.semantic.service.CanonicalSearchDocumentService;
import com.bookify.backend.discovery.semantic.service.SemanticSearchService;
import com.bookify.backend.discovery.service.NearbyDiscoveryService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SemanticSearchServiceTest {
    private final NearbyDiscoveryService deterministic = mock(NearbyDiscoveryService.class);
    private final CanonicalSearchDocumentService documents =
            mock(CanonicalSearchDocumentService.class);

    @Test
    void semanticSearchImprovesRecallWithoutChangingEligibility() {
        DiscoverySearchItemResponse barber = item(1L, "Barbería Central", 100);
        DiscoverySearchItemResponse coworking = item(2L, "Espacio Norte", 200);
        when(deterministic.search(
                anyDouble(), anyDouble(), anyDouble(), isNull(), any(), any(),
                any(), eq(0), eq(100)
        )).thenReturn(new DiscoverySearchPageResponse(
                0, 100, false, List.of(barber, coworking)
        ));
        LinkedHashMap<Long, SemanticDocument> projection = new LinkedHashMap<>();
        projection.put(1L, new SemanticDocument(1L, "barberia corte cabello"));
        projection.put(2L, new SemanticDocument(
                2L, "coworking escritorios oficina compartida"
        ));
        when(documents.build(List.of(barber, coworking))).thenReturn(projection);
        SemanticSearchService service = service(
                new MockEmbeddingAdapter(), new MockRerankingAdapter(), true
        );

        var result = service.search(
                -12.1, -77, 5, "lugar para trabajar con laptop", null,
                BigDecimal.ZERO, null, 0, 10
        );

        assertEquals("SEMANTIC", result.mode());
        assertEquals(2L, result.items().get(0).result().locationId());
        assertNotNull(result.items().get(0).semanticScore());
        assertEquals(List.of(1L, 2L), result.items().stream()
                .map(item -> item.result().locationId()).sorted().toList());
        verify(deterministic).search(
                -12.1, -77, 5, null, null, BigDecimal.ZERO, null, 0, 100
        );
    }

    @Test
    void disabledSemanticSearchUsesDeterministicContract() {
        DiscoverySearchItemResponse resultItem = item(1L, "Barbería", 100);
        when(deterministic.search(anyDouble(), anyDouble(), anyDouble(), eq("corte"),
                any(), any(), any(), eq(0), eq(10)))
                .thenReturn(new DiscoverySearchPageResponse(
                        0, 10, false, List.of(resultItem)
                ));
        SemanticSearchService service = service(
                new MockEmbeddingAdapter(), new MockRerankingAdapter(), false
        );

        var result = service.search(
                -12.1, -77, 5, "corte", null, BigDecimal.ZERO, null, 0, 10
        );

        assertEquals("DETERMINISTIC", result.mode());
        assertEquals("SEMANTIC_DISABLED", result.fallbackReason());
        assertNull(result.items().get(0).semanticScore());
        verifyNoInteractions(documents);
    }

    @Test
    void providerFailureFallsBackWithoutLeakingCandidates() {
        DiscoverySearchItemResponse eligible = item(1L, "Centro", 100);
        when(deterministic.search(anyDouble(), anyDouble(), anyDouble(), isNull(),
                any(), any(), any(), eq(0), eq(100)))
                .thenReturn(new DiscoverySearchPageResponse(
                        0, 100, false, List.of(eligible)
                ));
        when(deterministic.search(anyDouble(), anyDouble(), anyDouble(), eq("consulta"),
                any(), any(), any(), eq(0), eq(10)))
                .thenReturn(new DiscoverySearchPageResponse(
                        0, 10, false, List.of(eligible)
                ));
        when(documents.build(List.of(eligible))).thenReturn(MapBuilder.of(
                1L, new SemanticDocument(1L, "consulta profesional")
        ));
        EmbeddingPort failing = new EmbeddingPort() {
            public List<double[]> embed(List<String> texts, EmbeddingInputType inputType) {
                throw new IllegalStateException("offline");
            }
            public String providerName() { return "failing"; }
        };
        SemanticSearchService service = service(
                failing, new MockRerankingAdapter(), true
        );

        var result = service.search(
                -12.1, -77, 5, "consulta", null, BigDecimal.ZERO, null, 0, 10
        );

        assertEquals("DETERMINISTIC", result.mode());
        assertEquals("PROVIDER_FAILURE", result.fallbackReason());
        assertEquals(List.of(eligible), result.items().stream()
                .map(item -> item.result()).toList());
    }

    @Test
    void malformedProviderScoresTriggerSafeFallback() {
        DiscoverySearchItemResponse eligible = item(1L, "Centro", 100);
        when(deterministic.search(anyDouble(), anyDouble(), anyDouble(), isNull(),
                any(), any(), any(), eq(0), eq(100)))
                .thenReturn(new DiscoverySearchPageResponse(
                        0, 100, false, List.of(eligible)
                ));
        when(deterministic.search(anyDouble(), anyDouble(), anyDouble(), eq("consulta"),
                any(), any(), any(), eq(0), eq(10)))
                .thenReturn(new DiscoverySearchPageResponse(
                        0, 10, false, List.of(eligible)
                ));
        when(documents.build(List.of(eligible))).thenReturn(MapBuilder.of(
                1L, new SemanticDocument(1L, "consulta")
        ));
        RerankingPort malformed = new RerankingPort() {
            public List<RankedSemanticCandidate> rerank(
                    String query,
                    List<com.bookify.backend.discovery.semantic.model.SemanticCandidate> candidates,
                    int limit
            ) {
                return List.of(new RankedSemanticCandidate(1L, Double.NaN));
            }
            public String providerName() { return "malformed"; }
        };
        SemanticSearchService service = service(
                new MockEmbeddingAdapter(), malformed, true
        );

        var result = service.search(
                -12.1, -77, 5, "consulta", null, BigDecimal.ZERO, null, 0, 10
        );

        assertEquals("DETERMINISTIC", result.mode());
        assertEquals("PROVIDER_FAILURE", result.fallbackReason());
    }

    private SemanticSearchService service(
            EmbeddingPort embeddings,
            RerankingPort reranker,
            boolean enabled
    ) {
        return new SemanticSearchService(
                deterministic, documents, embeddings, reranker, Runnable::run,
                enabled, 100, 50, 500
        );
    }

    private DiscoverySearchItemResponse item(Long locationId, String name, double distance) {
        return new DiscoverySearchItemResponse(
                locationId, "business-" + locationId, name, "PROFESSIONAL_SERVICES", BigDecimal.valueOf(4.5),
                10, locationId, "Principal", "Dirección", "Lima", "PE",
                "America/Lima", BigDecimal.valueOf(-12.1), BigDecimal.valueOf(-77),
                distance, null, List.of()
        );
    }

    private static final class MapBuilder {
        private static LinkedHashMap<Long, SemanticDocument> of(
                Long key, SemanticDocument value
        ) {
            LinkedHashMap<Long, SemanticDocument> result = new LinkedHashMap<>();
            result.put(key, value);
            return result;
        }
    }
}
