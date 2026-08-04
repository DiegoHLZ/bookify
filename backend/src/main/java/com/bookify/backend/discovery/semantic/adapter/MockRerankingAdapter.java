package com.bookify.backend.discovery.semantic.adapter;

import com.bookify.backend.discovery.semantic.model.RankedSemanticCandidate;
import com.bookify.backend.discovery.semantic.model.SemanticCandidate;
import com.bookify.backend.discovery.semantic.port.RerankingPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@ConditionalOnProperty(
        prefix = "bookify.semantic",
        name = "provider",
        havingValue = "mock",
        matchIfMissing = true
)
public class MockRerankingAdapter implements RerankingPort {
    @Override
    public List<RankedSemanticCandidate> rerank(
            String query,
            List<SemanticCandidate> candidates,
            int limit
    ) {
        Set<String> queryTokens = tokens(query);
        return candidates.stream()
                .map(candidate -> new RankedSemanticCandidate(
                        candidate.locationId(),
                        bounded(0.85 * candidate.embeddingScore()
                                + 0.15 * overlap(queryTokens, tokens(candidate.document())))
                ))
                .sorted((left, right) -> {
                    int score = Double.compare(right.score(), left.score());
                    return score != 0 ? score : left.locationId().compareTo(right.locationId());
                })
                .limit(limit)
                .toList();
    }

    @Override
    public String providerName() {
        return "mock";
    }

    private Set<String> tokens(String value) {
        return new HashSet<>(Arrays.asList(MockEmbeddingAdapter.normalize(value).split(" ")));
    }

    private double overlap(Set<String> query, Set<String> document) {
        if (query.isEmpty()) {
            return 0;
        }
        long matches = query.stream().filter(document::contains).count();
        return (double) matches / query.size();
    }

    private double bounded(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
