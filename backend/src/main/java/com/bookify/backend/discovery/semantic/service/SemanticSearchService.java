package com.bookify.backend.discovery.semantic.service;

import com.bookify.backend.common.exception.BadRequestException;
import com.bookify.backend.discovery.dto.DiscoverySearchItemResponse;
import com.bookify.backend.discovery.dto.DiscoverySearchPageResponse;
import com.bookify.backend.discovery.semantic.dto.SemanticSearchItemResponse;
import com.bookify.backend.discovery.semantic.dto.SemanticSearchPageResponse;
import com.bookify.backend.discovery.semantic.model.RankedSemanticCandidate;
import com.bookify.backend.discovery.semantic.model.EmbeddingInputType;
import com.bookify.backend.discovery.semantic.model.SemanticCandidate;
import com.bookify.backend.discovery.semantic.model.SemanticDocument;
import com.bookify.backend.discovery.semantic.port.EmbeddingPort;
import com.bookify.backend.discovery.semantic.port.RerankingPort;
import com.bookify.backend.discovery.service.NearbyDiscoveryService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Service
public class SemanticSearchService {
    private static final int MAX_PAGE = 100;
    private static final int MAX_PAGE_SIZE = 100;

    private final NearbyDiscoveryService deterministicSearch;
    private final CanonicalSearchDocumentService documentService;
    private final EmbeddingPort embeddingPort;
    private final RerankingPort rerankingPort;
    private final Executor executor;
    private final boolean enabled;
    private final int candidateLimit;
    private final int rerankLimit;
    private final long timeoutMs;

    public SemanticSearchService(
            NearbyDiscoveryService deterministicSearch,
            CanonicalSearchDocumentService documentService,
            EmbeddingPort embeddingPort,
            RerankingPort rerankingPort,
            @Qualifier("semanticSearchExecutor") Executor executor,
            @Value("${bookify.semantic.enabled:false}") boolean enabled,
            @Value("${bookify.semantic.candidate-limit:100}") int candidateLimit,
            @Value("${bookify.semantic.rerank-limit:50}") int rerankLimit,
            @Value("${bookify.semantic.timeout-ms:500}") long timeoutMs
    ) {
        this.deterministicSearch = deterministicSearch;
        this.documentService = documentService;
        this.embeddingPort = embeddingPort;
        this.rerankingPort = rerankingPort;
        this.executor = executor;
        this.enabled = enabled;
        this.candidateLimit = bounded(candidateLimit, 1, 100, "candidate limit");
        this.rerankLimit = bounded(rerankLimit, 1, this.candidateLimit, "rerank limit");
        this.timeoutMs = bounded(timeoutMs, 10, 10_000, "timeout");
    }

    public SemanticSearchPageResponse search(
            double latitude,
            double longitude,
            double radiusKm,
            String text,
            String categoryCode,
            BigDecimal minRating,
            LocalDateTime availableAt,
            int page,
            int size
    ) {
        validateRequest(text, page, size);
        if (!enabled) {
            return fallback(latitude, longitude, radiusKm, text, categoryCode, minRating,
                    availableAt, page, size, "SEMANTIC_DISABLED");
        }
        if (text == null || text.isBlank()) {
            return fallback(latitude, longitude, radiusKm, null, categoryCode, minRating,
                    availableAt, page, size, "QUERY_REQUIRED");
        }
        if ((long) page * size >= rerankLimit) {
            throw new BadRequestException(
                    "Requested semantic page exceeds the configured rerank window"
            );
        }

        DiscoverySearchPageResponse eligible = deterministicSearch.search(
                latitude, longitude, radiusKm, null, categoryCode, minRating,
                availableAt, 0, candidateLimit
        );
        Map<Long, DiscoverySearchItemResponse> itemsByLocation = eligible.items().stream()
                .collect(java.util.stream.Collectors.toMap(
                        DiscoverySearchItemResponse::locationId,
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        Map<Long, SemanticDocument> documents = documentService.build(eligible.items());
        Instant startedAt = Instant.now();
        try {
            List<RankedSemanticCandidate> ranked = CompletableFuture.supplyAsync(
                            () -> rank(text, documents), executor
                    )
                    .orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    .join();
            long latency = Duration.between(startedAt, Instant.now()).toMillis();
            List<SemanticSearchItemResponse> ordered = sanitize(
                    ranked, itemsByLocation.keySet()
            ).stream()
                    .map(candidate -> new SemanticSearchItemResponse(
                            itemsByLocation.get(candidate.locationId()), candidate.score()
                    ))
                    .toList();
            int from = page * size;
            int to = Math.min(from + size, ordered.size());
            List<SemanticSearchItemResponse> pageItems = from >= ordered.size()
                    ? List.of() : ordered.subList(from, to);
            return new SemanticSearchPageResponse(
                    page, size, to < ordered.size(), "SEMANTIC",
                    providerName(), null, latency, List.copyOf(pageItems)
            );
        } catch (RuntimeException exception) {
            return fallback(latitude, longitude, radiusKm, text, categoryCode, minRating,
                    availableAt, page, size, "PROVIDER_FAILURE");
        }
    }

    private List<RankedSemanticCandidate> rank(
            String query,
            Map<Long, SemanticDocument> documents
    ) {
        double[] queryVector = requireSingleEmbedding(embeddingPort.embed(
                List.of(query), EmbeddingInputType.QUERY
        ));
        List<SemanticDocument> orderedDocuments = List.copyOf(documents.values());
        List<double[]> passageVectors = embeddingPort.embed(
                orderedDocuments.stream().map(SemanticDocument::text).toList(),
                EmbeddingInputType.PASSAGE
        );
        if (passageVectors.size() != orderedDocuments.size()) {
            throw new IllegalStateException("Embedding provider returned an invalid batch");
        }
        List<SemanticCandidate> candidates = java.util.stream.IntStream
                .range(0, orderedDocuments.size())
                .mapToObj(index -> new SemanticCandidate(
                        orderedDocuments.get(index).locationId(),
                        orderedDocuments.get(index).text(),
                        cosine(queryVector, passageVectors.get(index))
                ))
                .sorted(Comparator.comparingDouble(SemanticCandidate::embeddingScore)
                        .reversed()
                        .thenComparing(SemanticCandidate::locationId))
                .limit(rerankLimit)
                .toList();
        return rerankingPort.rerank(query, candidates, rerankLimit);
    }

    private double[] requireSingleEmbedding(List<double[]> embeddings) {
        if (embeddings == null || embeddings.size() != 1 || embeddings.get(0) == null) {
            throw new IllegalStateException("Embedding provider returned an invalid query vector");
        }
        return embeddings.get(0);
    }

    private SemanticSearchPageResponse fallback(
            double latitude, double longitude, double radiusKm, String text,
            String categoryCode, BigDecimal minRating, LocalDateTime availableAt,
            int page, int size, String reason
    ) {
        DiscoverySearchPageResponse result = deterministicSearch.search(
                latitude, longitude, radiusKm, text, categoryCode, minRating,
                availableAt, page, size
        );
        return new SemanticSearchPageResponse(
                result.page(), result.size(), result.hasNext(), "DETERMINISTIC",
                providerName(), reason, 0,
                result.items().stream()
                        .map(item -> new SemanticSearchItemResponse(item, null))
                        .toList()
        );
    }

    private double cosine(double[] left, double[] right) {
        if (left.length != right.length) {
            throw new IllegalStateException("Embedding dimensions do not match");
        }
        double score = 0;
        for (int index = 0; index < left.length; index++) {
            score += left[index] * right[index];
        }
        return Math.max(0, Math.min(1, score));
    }

    private List<RankedSemanticCandidate> sanitize(
            List<RankedSemanticCandidate> ranked,
            Set<Long> eligibleLocationIds
    ) {
        if (ranked == null) {
            throw new IllegalStateException("Reranking provider returned no result");
        }
        Set<Long> seen = new HashSet<>();
        return ranked.stream()
                .filter(candidate -> candidate != null
                        && eligibleLocationIds.contains(candidate.locationId()))
                .peek(candidate -> {
                    if (!Double.isFinite(candidate.score())
                            || candidate.score() < 0 || candidate.score() > 1) {
                        throw new IllegalStateException(
                                "Reranking provider returned an invalid score"
                        );
                    }
                    if (!seen.add(candidate.locationId())) {
                        throw new IllegalStateException(
                                "Reranking provider returned duplicate candidates"
                        );
                    }
                })
                .toList();
    }

    private String providerName() {
        return embeddingPort.providerName() + "+" + rerankingPort.providerName();
    }

    private void validateRequest(String text, int page, int size) {
        if (text != null && text.trim().replaceAll("\\s+", " ").length() > 100) {
            throw new BadRequestException("Search text cannot exceed 100 characters");
        }
        if (page < 0 || page > MAX_PAGE) {
            throw new BadRequestException("Page must be between 0 and 100");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Page size must be between 1 and 100");
        }
    }

    private int bounded(int value, int minimum, int maximum, String name) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(
                    "Semantic " + name + " must be between " + minimum + " and " + maximum
            );
        }
        return value;
    }

    private long bounded(long value, long minimum, long maximum, String name) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(
                    "Semantic " + name + " must be between " + minimum + " and " + maximum
            );
        }
        return value;
    }
}
