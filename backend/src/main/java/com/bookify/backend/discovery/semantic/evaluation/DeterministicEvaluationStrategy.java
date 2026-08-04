package com.bookify.backend.discovery.semantic.evaluation;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DeterministicEvaluationStrategy implements SearchRankingStrategy {
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "al", "con", "de", "del", "donde", "el", "en", "la", "las",
            "lo", "los", "mi", "para", "por", "que", "quiero", "un", "una", "y"
    );

    @Override
    public String name() {
        return "deterministic-token-baseline";
    }

    @Override
    public List<String> rank(
            String query,
            List<LabeledSearchDataset.EvaluationDocument> documents,
            int limit
    ) {
        Set<String> queryTokens = tokens(query);
        return documents.stream()
                .map(document -> new ScoredDocument(
                        document.id(), overlap(queryTokens, tokens(document.text()))
                ))
                .filter(document -> document.score() > 0)
                .sorted((left, right) -> {
                    int score = Double.compare(right.score(), left.score());
                    return score != 0 ? score : left.id().compareTo(right.id());
                })
                .limit(limit)
                .map(ScoredDocument::id)
                .toList();
    }

    private Set<String> tokens(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        Set<String> tokens = new HashSet<>(Arrays.asList(normalized.split(" ")));
        tokens.removeAll(STOP_WORDS);
        tokens.remove("");
        return tokens;
    }

    private double overlap(Set<String> query, Set<String> document) {
        if (query.isEmpty()) {
            return 0;
        }
        return (double) query.stream().filter(document::contains).count() / query.size();
    }

    private record ScoredDocument(String id, double score) {
    }
}
