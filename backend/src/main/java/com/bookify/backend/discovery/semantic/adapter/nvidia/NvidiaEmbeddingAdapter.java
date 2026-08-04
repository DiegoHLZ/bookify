package com.bookify.backend.discovery.semantic.adapter.nvidia;

import com.fasterxml.jackson.databind.JsonNode;
import com.bookify.backend.discovery.semantic.model.EmbeddingInputType;
import com.bookify.backend.discovery.semantic.port.EmbeddingPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(
        prefix = "bookify.semantic",
        name = "provider",
        havingValue = "nvidia"
)
public class NvidiaEmbeddingAdapter implements EmbeddingPort {
    private final RestClient client;
    private final String endpoint;
    private final String model;

    public NvidiaEmbeddingAdapter(
            @Value("${bookify.semantic.nvidia.api-key:}") String apiKey,
            @Value("${bookify.semantic.nvidia.embedding-url}") String endpoint,
            @Value("${bookify.semantic.nvidia.embedding-model}") String model,
            @Value("${bookify.semantic.nvidia.connect-timeout-ms:1000}") long connectTimeoutMs,
            @Value("${bookify.semantic.nvidia.read-timeout-ms:5000}") long readTimeoutMs
    ) {
        this(NvidiaRestClientFactory.create(
                apiKey, connectTimeoutMs, readTimeoutMs
        ), endpoint, model);
    }

    NvidiaEmbeddingAdapter(RestClient client, String endpoint, String model) {
        this.client = client;
        this.endpoint = requireHttpUrl(endpoint, "embedding URL");
        this.model = requireText(model, "embedding model");
    }

    @Override
    public List<double[]> embed(List<String> texts, EmbeddingInputType inputType) {
        if (texts == null || texts.isEmpty() || texts.size() > 100) {
            throw new IllegalArgumentException("NVIDIA embedding batch must contain 1 to 100 texts");
        }
        if (texts.stream().anyMatch(text -> text == null || text.isBlank())) {
            throw new IllegalArgumentException("NVIDIA embedding text cannot be blank");
        }
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("input", texts);
        request.put("model", model);
        request.put("input_type", inputType.name().toLowerCase(java.util.Locale.ROOT));
        request.put("encoding_format", "float");
        request.put("truncate", "END");
        JsonNode response = client.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(JsonNode.class);
        return parse(response, texts.size());
    }

    @Override
    public String providerName() {
        return "nvidia:" + model;
    }

    private List<double[]> parse(JsonNode response, int expectedSize) {
        if (response == null || !response.path("data").isArray()) {
            throw new IllegalStateException("NVIDIA embedding response has no data array");
        }
        List<IndexedVector> vectors = new ArrayList<>();
        for (JsonNode item : response.path("data")) {
            int index = item.path("index").asInt(-1);
            JsonNode values = item.path("embedding");
            if (index < 0 || !values.isArray() || values.isEmpty()) {
                throw new IllegalStateException("NVIDIA embedding response is malformed");
            }
            double[] vector = new double[values.size()];
            for (int valueIndex = 0; valueIndex < values.size(); valueIndex++) {
                vector[valueIndex] = values.get(valueIndex).asDouble(Double.NaN);
                if (!Double.isFinite(vector[valueIndex])) {
                    throw new IllegalStateException("NVIDIA embedding contains a non-finite value");
                }
            }
            vectors.add(new IndexedVector(index, vector));
        }
        vectors.sort(Comparator.comparingInt(IndexedVector::index));
        if (vectors.size() != expectedSize) {
            throw new IllegalStateException("NVIDIA embedding batch size does not match request");
        }
        for (int index = 0; index < vectors.size(); index++) {
            if (vectors.get(index).index() != index) {
                throw new IllegalStateException("NVIDIA embedding indexes are incomplete");
            }
        }
        int dimensions = vectors.get(0).vector().length;
        if (vectors.stream().anyMatch(value -> value.vector().length != dimensions)) {
            throw new IllegalStateException("NVIDIA embedding dimensions do not match");
        }
        return vectors.stream().map(IndexedVector::vector).toList();
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

    private record IndexedVector(int index, double[] vector) {
    }
}
