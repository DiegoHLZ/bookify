package com.bookify.backend.discovery.semantic.model;

public record SemanticCandidate(
        Long locationId,
        String document,
        double embeddingScore
) {
}
