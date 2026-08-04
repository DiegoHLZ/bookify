package com.bookify.backend.discovery.semantic.port;

import com.bookify.backend.discovery.semantic.model.EmbeddingInputType;

import java.util.List;

public interface EmbeddingPort {
    List<double[]> embed(List<String> texts, EmbeddingInputType inputType);
    String providerName();
}
