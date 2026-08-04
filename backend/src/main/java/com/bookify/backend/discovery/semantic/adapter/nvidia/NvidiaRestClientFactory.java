package com.bookify.backend.discovery.semantic.adapter.nvidia;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

final class NvidiaRestClientFactory {
    private NvidiaRestClientFactory() {
    }

    static RestClient create(String apiKey, long connectTimeoutMs, long readTimeoutMs) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException(
                    "NVIDIA API key is required when the semantic provider is nvidia"
            );
        }
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
