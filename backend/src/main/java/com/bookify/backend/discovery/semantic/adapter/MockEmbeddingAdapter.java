package com.bookify.backend.discovery.semantic.adapter;

import com.bookify.backend.discovery.semantic.port.EmbeddingPort;
import com.bookify.backend.discovery.semantic.model.EmbeddingInputType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.List;

@Component
@ConditionalOnProperty(
        prefix = "bookify.semantic",
        name = "provider",
        havingValue = "mock",
        matchIfMissing = true
)
public class MockEmbeddingAdapter implements EmbeddingPort {
    private static final int DIMENSIONS = 128;
    private static final Map<String, String> CONCEPTS = Map.ofEntries(
            Map.entry("cabello", "corte"), Map.entry("pelo", "corte"),
            Map.entry("peluqueria", "corte"), Map.entry("barberia", "corte"),
            Map.entry("trabajar", "coworking"), Map.entry("laptop", "coworking"),
            Map.entry("escritorio", "coworking"), Map.entry("oficina", "coworking"),
            Map.entry("entrenar", "deporte"), Map.entry("futbol", "deporte"),
            Map.entry("cancha", "deporte"), Map.entry("masajes", "masaje"),
            Map.entry("relajar", "bienestar"), Map.entry("spa", "bienestar")
    );

    @Override
    public List<double[]> embed(List<String> texts, EmbeddingInputType inputType) {
        return texts.stream().map(this::embedOne).toList();
    }

    private double[] embedOne(String text) {
        double[] vector = new double[DIMENSIONS];
        for (String token : normalize(text).split(" ")) {
            if (token.isBlank()) {
                continue;
            }
            String concept = CONCEPTS.getOrDefault(token, token);
            int index = Math.floorMod(concept.hashCode(), DIMENSIONS);
            vector[index] += 1.0;
        }
        normalize(vector);
        return vector;
    }

    @Override
    public String providerName() {
        return "mock";
    }

    static String normalize(String value) {
        String withoutAccents = Normalizer.normalize(
                value == null ? "" : value, Normalizer.Form.NFD
        ).replaceAll("\\p{M}+", "");
        return withoutAccents.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private void normalize(double[] vector) {
        double magnitude = 0;
        for (double value : vector) {
            magnitude += value * value;
        }
        if (magnitude == 0) {
            return;
        }
        magnitude = Math.sqrt(magnitude);
        for (int index = 0; index < vector.length; index++) {
            vector[index] /= magnitude;
        }
    }
}
