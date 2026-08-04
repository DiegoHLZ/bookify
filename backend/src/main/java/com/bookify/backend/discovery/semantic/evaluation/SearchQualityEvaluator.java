package com.bookify.backend.discovery.semantic.evaluation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SearchQualityEvaluator {
    public SearchEvaluationMetrics evaluate(
            LabeledSearchDataset dataset,
            SearchRankingStrategy strategy,
            int k,
            Double costPerThousandRequestsUsd
    ) {
        validate(dataset, k, costPerThousandRequestsUsd);
        List<LabeledSearchDataset.EvaluationDocument> eligible = dataset.documents().stream()
                .filter(LabeledSearchDataset.EvaluationDocument::eligible)
                .toList();
        Set<String> eligibleIds = eligible.stream()
                .map(LabeledSearchDataset.EvaluationDocument::id)
                .collect(java.util.stream.Collectors.toSet());
        double recall = 0;
        double ndcg = 0;
        int ineligible = 0;
        List<Long> latencies = new ArrayList<>();
        for (LabeledSearchDataset.EvaluationQuery query : dataset.queries()) {
            long startedAt = System.nanoTime();
            List<String> ranked = strategy.rank(query.text(), eligible, k);
            latencies.add(System.nanoTime() - startedAt);
            ineligible += (int) ranked.stream().filter(id -> !eligibleIds.contains(id)).count();
            recall += recallAtK(query.relevance(), ranked, k);
            ndcg += ndcgAtK(query.relevance(), ranked, k);
        }
        int queryCount = dataset.queries().size();
        long providerRequests = strategy.providerRequestsPerQuery() * queryCount;
        Double cost = costPerThousandRequestsUsd == null ? null
                : providerRequests * costPerThousandRequestsUsd / 1000.0;
        return new SearchEvaluationMetrics(
                strategy.name(), queryCount, k,
                recall / queryCount, ndcg / queryCount,
                latencies.stream().mapToLong(Long::longValue).average().orElse(0) / 1_000_000.0,
                percentile95(latencies) / 1_000_000.0,
                ineligible, providerRequests, cost
        );
    }

    private double recallAtK(Map<String, Integer> relevance, List<String> ranked, int k) {
        Set<String> relevant = relevance.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
        if (relevant.isEmpty()) {
            return 1;
        }
        long found = ranked.stream().limit(k).filter(relevant::contains).distinct().count();
        return (double) found / relevant.size();
    }

    private double ndcgAtK(Map<String, Integer> relevance, List<String> ranked, int k) {
        double actual = 0;
        for (int index = 0; index < Math.min(k, ranked.size()); index++) {
            int grade = relevance.getOrDefault(ranked.get(index), 0);
            actual += gain(grade, index);
        }
        List<Integer> idealGrades = relevance.values().stream()
                .sorted(Comparator.reverseOrder())
                .limit(k)
                .toList();
        double ideal = 0;
        for (int index = 0; index < idealGrades.size(); index++) {
            ideal += gain(idealGrades.get(index), index);
        }
        return ideal == 0 ? 1 : actual / ideal;
    }

    private double gain(int grade, int index) {
        return (Math.pow(2, grade) - 1) / (Math.log(index + 2) / Math.log(2));
    }

    private long percentile95(List<Long> values) {
        List<Long> sorted = values.stream().sorted().toList();
        int index = Math.max(0, (int) Math.ceil(sorted.size() * 0.95) - 1);
        return sorted.get(index);
    }

    private void validate(
            LabeledSearchDataset dataset,
            int k,
            Double costPerThousandRequestsUsd
    ) {
        if (dataset == null || dataset.documents() == null || dataset.documents().isEmpty()
                || dataset.queries() == null || dataset.queries().isEmpty()) {
            throw new IllegalArgumentException("Evaluation dataset cannot be empty");
        }
        if (k < 1) {
            throw new IllegalArgumentException("Evaluation k must be positive");
        }
        if (costPerThousandRequestsUsd != null && costPerThousandRequestsUsd < 0) {
            throw new IllegalArgumentException("Evaluation request cost cannot be negative");
        }
        Set<String> documentIds = new HashSet<>();
        for (LabeledSearchDataset.EvaluationDocument document : dataset.documents()) {
            if (document.id() == null || document.id().isBlank()
                    || !documentIds.add(document.id())) {
                throw new IllegalArgumentException("Evaluation document IDs must be unique");
            }
        }
        Set<String> queryIds = new HashSet<>();
        for (LabeledSearchDataset.EvaluationQuery query : dataset.queries()) {
            if (query.id() == null || query.id().isBlank() || !queryIds.add(query.id())
                    || query.text() == null || query.text().isBlank()
                    || query.relevance() == null || query.relevance().isEmpty()
                    || !documentIds.containsAll(query.relevance().keySet())) {
                throw new IllegalArgumentException("Evaluation query labels are invalid");
            }
        }
    }
}
