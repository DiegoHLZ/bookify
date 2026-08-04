package com.bookify.backend.discovery.semantic.adapter.nvidia;

import com.bookify.backend.discovery.semantic.model.EmbeddingInputType;
import com.bookify.backend.discovery.semantic.model.SemanticCandidate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

class NvidiaAdaptersContractTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<JsonNode> requests = new CopyOnWriteArrayList<>();
    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/embeddings", exchange -> respond(exchange, """
                {"data":[
                  {"index":1,"embedding":[0.0,1.0]},
                  {"index":0,"embedding":[1.0,0.0]}
                ]}
                """));
        server.createContext("/ranking", exchange -> respond(exchange, """
                {"rankings":[
                  {"index":1,"logit":2.0},
                  {"index":0,"logit":-2.0}
                ]}
                """));
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void embeddingAdapterSendsPassageTypeAndRestoresIndexOrder() {
        NvidiaEmbeddingAdapter adapter = new NvidiaEmbeddingAdapter(
                NvidiaRestClientFactory.create("test-key", 1000, 1000),
                baseUrl + "/embeddings", "nvidia/test-embed"
        );

        List<double[]> vectors = adapter.embed(
                List.of("first", "second"), EmbeddingInputType.PASSAGE
        );

        assertArrayEquals(new double[]{1, 0}, vectors.get(0));
        assertArrayEquals(new double[]{0, 1}, vectors.get(1));
        assertEquals("passage", requests.get(0).path("input_type").asText());
        assertEquals("nvidia/test-embed", requests.get(0).path("model").asText());
    }

    @Test
    void rerankingAdapterMapsIndexesAndNormalizesLogits() {
        NvidiaRerankingAdapter adapter = new NvidiaRerankingAdapter(
                NvidiaRestClientFactory.create("test-key", 1000, 1000),
                baseUrl + "/ranking", "nvidia/test-rerank"
        );

        var result = adapter.rerank("query", List.of(
                new SemanticCandidate(10L, "first", 0.1),
                new SemanticCandidate(20L, "second", 0.2)
        ), 2);

        assertEquals(List.of(20L, 10L), result.stream()
                .map(value -> value.locationId()).toList());
        assertTrue(result.get(0).score() > result.get(1).score());
        assertEquals("query", requests.get(0).path("query").path("text").asText());
        assertEquals(2, requests.get(0).path("passages").size());
    }

    @Test
    void adaptersRejectInsecureRemoteUrlsAndMissingKeys() {
        assertThrows(IllegalArgumentException.class, () ->
                NvidiaRestClientFactory.create("", 1000, 1000));
        var client = NvidiaRestClientFactory.create("test", 1000, 1000);
        assertThrows(IllegalArgumentException.class, () ->
                new NvidiaEmbeddingAdapter(client, "http://example.com/embed", "model"));
        assertThrows(IllegalArgumentException.class, () ->
                new NvidiaRerankingAdapter(client, "http://example.com/rank", "model"));
    }

    private void respond(HttpExchange exchange, String response) throws IOException {
        assertEquals("Bearer test-key", exchange.getRequestHeaders()
                .getFirst("Authorization"));
        requests.add(objectMapper.readTree(exchange.getRequestBody()));
        byte[] body = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
