package com.bookify.backend.discovery.semantic.adapter;

import com.bookify.backend.discovery.semantic.model.SemanticCandidate;
import org.junit.jupiter.api.Test;

import java.util.List;

import com.bookify.backend.discovery.semantic.model.EmbeddingInputType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockSemanticAdaptersTest {
    private final MockEmbeddingAdapter embeddings = new MockEmbeddingAdapter();
    private final MockRerankingAdapter reranker = new MockRerankingAdapter();

    @Test
    void recognizesConfiguredSpanishConceptsDeterministically() {
        double coworking = cosine(
                embed("quiero trabajar con mi laptop", EmbeddingInputType.QUERY),
                embed("coworking con escritorios y oficina compartida", EmbeddingInputType.PASSAGE)
        );
        double haircut = cosine(
                embed("quiero trabajar con mi laptop", EmbeddingInputType.QUERY),
                embed("barberia para corte de cabello", EmbeddingInputType.PASSAGE)
        );

        assertTrue(coworking > haircut);
        assertEquals(indexOfMaximum(embed("laptop", EmbeddingInputType.QUERY)),
                indexOfMaximum(embed("escritorio", EmbeddingInputType.PASSAGE)));
    }

    @Test
    void rerankingUsesStableLocationIdTieBreak() {
        var results = reranker.rerank("consulta", List.of(
                new SemanticCandidate(20L, "consulta", 1),
                new SemanticCandidate(10L, "consulta", 1)
        ), 10);

        assertEquals(List.of(10L, 20L), results.stream()
                .map(result -> result.locationId()).toList());
    }

    private double cosine(double[] left, double[] right) {
        double score = 0;
        for (int index = 0; index < left.length; index++) {
            score += left[index] * right[index];
        }
        return score;
    }

    private double[] embed(String value, EmbeddingInputType type) {
        return embeddings.embed(List.of(value), type).get(0);
    }

    private int indexOfMaximum(double[] vector) {
        int maximum = 0;
        for (int index = 1; index < vector.length; index++) {
            if (vector[index] > vector[maximum]) {
                maximum = index;
            }
        }
        return maximum;
    }
}
