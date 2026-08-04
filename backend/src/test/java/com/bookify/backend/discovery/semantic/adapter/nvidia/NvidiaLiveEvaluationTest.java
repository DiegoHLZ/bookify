package com.bookify.backend.discovery.semantic.adapter.nvidia;

import com.bookify.backend.discovery.semantic.evaluation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class NvidiaLiveEvaluationTest {
    private static final String EMBEDDING_URL =
            "https://integrate.api.nvidia.com/v1/embeddings";
    private static final String EMBEDDING_MODEL =
            "nvidia/llama-nemotron-embed-1b-v2";
    private static final String RERANKING_URL =
            "https://ai.api.nvidia.com/v1/retrieval/nvidia/llama-nemotron-rerank-1b-v2/reranking";
    private static final String RERANKING_MODEL =
            "nvidia/llama-nemotron-rerank-1b-v2";

    @Test
    @EnabledIfEnvironmentVariable(
            named = "BOOKIFY_RUN_NVIDIA_LIVE",
            matches = "(?i)true"
    )
    void evaluatesLiveNvidiaAgainstDeterministicBaseline() throws Exception {
        String apiKey = System.getenv("NVIDIA_API_KEY");
        assertNotNull(apiKey, "NVIDIA_API_KEY is required for the live benchmark");
        var client = NvidiaRestClientFactory.create(apiKey, 2_000, 30_000);
        var embeddings = new NvidiaEmbeddingAdapter(
                client, EMBEDDING_URL, EMBEDDING_MODEL
        );
        var reranker = new NvidiaRerankingAdapter(
                client, RERANKING_URL, RERANKING_MODEL
        );
        LabeledSearchDataset dataset = dataset();
        SearchQualityEvaluator evaluator = new SearchQualityEvaluator();
        SearchEvaluationMetrics deterministic = evaluator.evaluate(
                dataset, new DeterministicEvaluationStrategy(), 3, null
        );
        SearchEvaluationMetrics nvidia = evaluator.evaluate(
                dataset, new SemanticPortEvaluationStrategy(embeddings, reranker),
                3, configuredRequestCost()
        );

        assertEquals(0, nvidia.ineligibleResults());
        assertTrue(Double.isFinite(nvidia.recallAtK()));
        assertTrue(Double.isFinite(nvidia.ndcgAtK()));
        System.out.printf(
                "NVIDIA_LIVE_EVALUATION baselineRecall@3=%.4f baselineNdcg@3=%.4f "
                        + "nvidiaRecall@3=%.4f nvidiaNdcg@3=%.4f meanMs=%.2f p95Ms=%.2f "
                        + "requests=%d estimatedCostUsd=%s%n",
                deterministic.recallAtK(), deterministic.ndcgAtK(),
                nvidia.recallAtK(), nvidia.ndcgAtK(), nvidia.meanLatencyMs(),
                nvidia.p95LatencyMs(), nvidia.providerRequests(),
                nvidia.estimatedCostUsd() == null
                        ? "NOT_CONFIGURED" : String.format("%.6f", nvidia.estimatedCostUsd())
        );
    }

    private Double configuredRequestCost() {
        String value = System.getenv("BOOKIFY_NVIDIA_COST_PER_1000_REQUESTS_USD");
        return value == null || value.isBlank() ? null : Double.valueOf(value);
    }

    private LabeledSearchDataset dataset() throws Exception {
        try (InputStream input = getClass().getResourceAsStream(
                "/search-evaluation/bookify-labeled-queries-v1.json"
        )) {
            assertNotNull(input);
            return new ObjectMapper().readValue(input, LabeledSearchDataset.class);
        }
    }
}
