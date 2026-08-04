package com.bookify.backend.discovery.semantic.port;

public interface EmbeddingPort {
    double[] embed(String text);
    String providerName();
}
