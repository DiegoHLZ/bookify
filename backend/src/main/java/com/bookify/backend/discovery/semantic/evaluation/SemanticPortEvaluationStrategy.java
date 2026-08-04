package com.bookify.backend.discovery.semantic.evaluation;

import com.bookify.backend.discovery.semantic.model.EmbeddingInputType;
import com.bookify.backend.discovery.semantic.model.SemanticCandidate;
import com.bookify.backend.discovery.semantic.port.EmbeddingPort;
import com.bookify.backend.discovery.semantic.port.RerankingPort;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class SemanticPortEvaluationStrategy implements SearchRankingStrategy {
    private final EmbeddingPort embeddingPort;
    private final RerankingPort rerankingPort;

    public SemanticPortEvaluationStrategy(
            EmbeddingPort embeddingPort,
            RerankingPort rerankingPort
    ) {
        this.embeddingPort = embeddingPort;
        this.rerankingPort = rerankingPort;
    }

    @Override
    public String name() {
        return embeddingPort.providerName() + "+" + rerankingPort.providerName();
    }

    @Override
    public List<String> rank(
            String query,
            List<LabeledSearchDataset.EvaluationDocument> documents,
            int limit
    ) {
        double[] queryVector = embeddingPort.embed(
                List.of(query), EmbeddingInputType.QUERY
        ).get(0);
        List<double[]> passageVectors = embeddingPort.embed(
                documents.stream().map(LabeledSearchDataset.EvaluationDocument::text).toList(),
                EmbeddingInputType.PASSAGE
        );
        if (passageVectors.size() != documents.size()) {
            throw new IllegalStateException("Evaluation embedding batch is incomplete");
        }
        Map<Long, String> ids = new LinkedHashMap<>();
        List<SemanticCandidate> candidates = IntStream.range(0, documents.size())
                .mapToObj(index -> {
                    long locationId = index + 1L;
                    ids.put(locationId, documents.get(index).id());
                    return new SemanticCandidate(
                            locationId, documents.get(index).text(),
                            cosine(queryVector, passageVectors.get(index))
                    );
                })
                .toList();
        return rerankingPort.rerank(query, candidates, limit).stream()
                .map(result -> ids.get(result.locationId()))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Override
    public long providerRequestsPerQuery() {
        return 3;
    }

    private double cosine(double[] left, double[] right) {
        if (left.length != right.length) {
            throw new IllegalStateException("Evaluation embedding dimensions do not match");
        }
        double result = 0;
        for (int index = 0; index < left.length; index++) {
            result += left[index] * right[index];
        }
        return Math.max(0, Math.min(1, result));
    }
}
