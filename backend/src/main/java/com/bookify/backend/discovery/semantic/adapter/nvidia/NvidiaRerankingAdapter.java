package com.bookify.backend.discovery.semantic.adapter.nvidia;

import com.fasterxml.jackson.databind.JsonNode;
import com.bookify.backend.discovery.semantic.model.RankedSemanticCandidate;
import com.bookify.backend.discovery.semantic.model.SemanticCandidate;
import com.bookify.backend.discovery.semantic.port.RerankingPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(
        prefix = "bookify.semantic",
        name = "provider",
        havingValue = "nvidia"
)
public class NvidiaRerankingAdapter implements RerankingPort {
    private final RestClient client;
    private final String endpoint;
    private final String model;

    public NvidiaRerankingAdapter(
            @Value("${bookify.semantic.nvidia.api-key:}") String apiKey,
            @Value("${bookify.semantic.nvidia.reranking-url}") String endpoint,
            @Value("${bookify.semantic.nvidia.reranking-model}") String model,
            @Value("${bookify.semantic.nvidia.connect-timeout-ms:1000}") long connectTimeoutMs,
            @Value("${bookify.semantic.nvidia.read-timeout-ms:5000}") long readTimeoutMs
    ) {
        this(NvidiaRestClientFactory.create(
                apiKey, connectTimeoutMs, readTimeoutMs
        ), endpoint, model);
    }

    NvidiaRerankingAdapter(RestClient client, String endpoint, String model) {
        this.client = client;
        this.endpoint = requireHttpUrl(endpoint, "reranking URL");
        this.model = requireText(model, "reranking model");
    }

    @Override
    public List<RankedSemanticCandidate> rerank(
            String query,
            List<SemanticCandidate> candidates,
            int limit
    ) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("NVIDIA reranking query cannot be blank");
        }
        if (candidates == null || candidates.isEmpty() || candidates.size() > 1000) {
            throw new IllegalArgumentException(
                    "NVIDIA reranking requires 1 to 1000 candidates"
            );
        }
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("query", Map.of("text", query));
        request.put("passages", candidates.stream()
                .map(candidate -> Map.of("text", candidate.document()))
                .toList());
        request.put("truncate", "END");
        JsonNode response = client.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(JsonNode.class);
        return parse(response, candidates).stream().limit(limit).toList();
    }

    @Override
    public String providerName() {
        return "nvidia:" + model;
    }

    private List<RankedSemanticCandidate> parse(
            JsonNode response,
            List<SemanticCandidate> candidates
    ) {
        JsonNode rankings = response == null ? null : response.path("rankings");
        if (rankings == null || !rankings.isArray()) {
            rankings = response == null ? null : response.path("results");
        }
        if (rankings == null || !rankings.isArray()) {
            throw new IllegalStateException("NVIDIA reranking response has no rankings array");
        }
        boolean[] seen = new boolean[candidates.size()];
        List<RankedSemanticCandidate> results = new ArrayList<>();
        for (JsonNode item : rankings) {
            int index = item.path("index").asInt(-1);
            if (index < 0 || index >= candidates.size() || seen[index]) {
                throw new IllegalStateException("NVIDIA reranking response has invalid indexes");
            }
            seen[index] = true;
            JsonNode scoreNode = item.has("score") ? item.path("score") : item.path("logit");
            double rawScore = scoreNode.asDouble(Double.NaN);
            if (!Double.isFinite(rawScore)) {
                throw new IllegalStateException("NVIDIA reranking response has an invalid score");
            }
            double score = item.has("score") && rawScore >= 0 && rawScore <= 1
                    ? rawScore : sigmoid(rawScore);
            results.add(new RankedSemanticCandidate(
                    candidates.get(index).locationId(), score
            ));
        }
        if (results.size() != candidates.size()) {
            throw new IllegalStateException("NVIDIA reranking response is incomplete");
        }
        return results;
    }

    private double sigmoid(double value) {
        if (value >= 0) {
            double exponential = Math.exp(-value);
            return 1 / (1 + exponential);
        }
        double exponential = Math.exp(value);
        return exponential / (1 + exponential);
    }

    private String requireHttpUrl(String value, String name) {
        String result = requireText(value, name);
        if (!result.startsWith("https://") && !result.startsWith("http://localhost")
                && !result.startsWith("http://127.0.0.1")) {
            throw new IllegalArgumentException("NVIDIA " + name + " must use HTTPS");
        }
        return result;
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("NVIDIA " + name + " is required");
        }
        return value.trim();
    }
}
