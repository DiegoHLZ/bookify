package com.bookify.backend.discovery.semantic.evaluation;

import java.util.List;
import java.util.Map;

public record LabeledSearchDataset(
        String version,
        List<EvaluationDocument> documents,
        List<EvaluationQuery> queries
) {
    public record EvaluationDocument(String id, String text, boolean eligible) {
    }

    public record EvaluationQuery(
            String id,
            String language,
            String text,
            Map<String, Integer> relevance
    ) {
    }
}
