package com.bookify.backend.discovery.semantic.port;

import com.bookify.backend.discovery.semantic.model.RankedSemanticCandidate;
import com.bookify.backend.discovery.semantic.model.SemanticCandidate;

import java.util.List;

public interface RerankingPort {
    List<RankedSemanticCandidate> rerank(
            String query,
            List<SemanticCandidate> candidates,
            int limit
    );

    String providerName();
}
