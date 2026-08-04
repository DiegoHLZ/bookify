package com.bookify.backend.discovery.semantic.evaluation;

import com.bookify.backend.discovery.semantic.adapter.MockEmbeddingAdapter;
import com.bookify.backend.discovery.semantic.adapter.MockRerankingAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class SearchQualityEvaluatorTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SearchQualityEvaluator evaluator = new SearchQualityEvaluator();

    @Test
    void labeledDatasetIsValidAndMockSemanticBeatsTokenBaseline() throws Exception {
        LabeledSearchDataset dataset = dataset();

        SearchEvaluationMetrics deterministic = evaluator.evaluate(
                dataset, new DeterministicEvaluationStrategy(), 3, null
        );
        SearchEvaluationMetrics semantic = evaluator.evaluate(
                dataset,
                new SemanticPortEvaluationStrategy(
                        new MockEmbeddingAdapter(), new MockRerankingAdapter()
                ),
                3,
                0.0
        );

        assertEquals(12, deterministic.queryCount());
        assertEquals(0, deterministic.ineligibleResults());
        assertEquals(0, semantic.ineligibleResults());
        assertTrue(semantic.recallAtK() > deterministic.recallAtK());
        assertTrue(semantic.ndcgAtK() > deterministic.ndcgAtK());
        assertEquals(36, semantic.providerRequests());
        assertEquals(0.0, semantic.estimatedCostUsd());
        System.out.printf(
                "SEARCH_EVALUATION deterministic recall@3=%.4f ndcg@3=%.4f p95Ms=%.3f%n",
                deterministic.recallAtK(), deterministic.ndcgAtK(),
                deterministic.p95LatencyMs()
        );
        System.out.printf(
                "SEARCH_EVALUATION mock-semantic recall@3=%.4f ndcg@3=%.4f p95Ms=%.3f%n",
                semantic.recallAtK(), semantic.ndcgAtK(), semantic.p95LatencyMs()
        );
    }

    @Test
    void rejectsLabelsForUnknownDocuments() throws Exception {
        LabeledSearchDataset original = dataset();
        var invalidQuery = new LabeledSearchDataset.EvaluationQuery(
                "invalid", "es", "consulta", java.util.Map.of("missing", 3)
        );
        LabeledSearchDataset invalid = new LabeledSearchDataset(
                original.version(), original.documents(), java.util.List.of(invalidQuery)
        );

        assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(
                invalid, new DeterministicEvaluationStrategy(), 3, null
        ));
    }

    private LabeledSearchDataset dataset() throws Exception {
        try (InputStream input = getClass().getResourceAsStream(
                "/search-evaluation/bookify-labeled-queries-v1.json"
        )) {
            assertNotNull(input);
            return objectMapper.readValue(input, LabeledSearchDataset.class);
        }
    }
}
