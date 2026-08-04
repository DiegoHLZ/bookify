package com.bookify.backend.discovery.semantic.evaluation;

import java.util.List;

public interface SearchRankingStrategy {
    String name();

    List<String> rank(
            String query,
            List<LabeledSearchDataset.EvaluationDocument> documents,
            int limit
    );

    default long providerRequestsPerQuery() {
        return 0;
    }
}
