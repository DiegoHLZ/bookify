package com.bookify.backend.discovery.semantic.evaluation;

public record SearchEvaluationMetrics(
        String strategy,
        int queryCount,
        int k,
        double recallAtK,
        double ndcgAtK,
        double meanLatencyMs,
        double p95LatencyMs,
        int ineligibleResults,
        Long providerRequests,
        Double estimatedCostUsd
) {
}
