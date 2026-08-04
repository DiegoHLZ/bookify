# Semantic search foundation

Bookify exposes an experimental provider-neutral semantic search pipeline at
`GET /api/v1/discovery/semantic-search`. The deterministic discovery engine remains the
eligibility authority and the safe fallback.

## Pipeline

1. Apply canonical active status, verified coordinates, radius, category, rating and optional
   exact availability filters without AI.
2. Assemble an on-read search projection from canonical business, location and active service
   text.
3. Use `EmbeddingPort` to score semantic similarity.
4. Send only a bounded candidate set through `RerankingPort`.
5. Return canonical response objects with a semantic score and stable pagination.

Provider adapters cannot add candidate identifiers. Unknown identifiers returned by an adapter
are discarded. Availability and booking writes never depend on semantic output.

## Mock provider

The first adapter is deterministic and local. It uses a small Spanish concept map plus hashed
token vectors to exercise synonyms, ranking, provider boundaries and failure behavior without
calling an external service. It is test infrastructure, not a production relevance model.

Configuration:

```text
BOOKIFY_SEMANTIC_ENABLED=false
BOOKIFY_SEMANTIC_PROVIDER=mock
BOOKIFY_SEMANTIC_CANDIDATE_LIMIT=100
BOOKIFY_SEMANTIC_RERANK_LIMIT=50
BOOKIFY_SEMANTIC_TIMEOUT_MS=500
```

Semantic search is disabled by default. Invalid configuration fails at startup. Provider work
runs on a bounded executor and is limited by a 10–10,000 ms configurable timeout.

## Response behavior

- `mode=SEMANTIC`: embedding and reranking completed.
- `mode=DETERMINISTIC`, `fallbackReason=SEMANTIC_DISABLED`: feature flag is off.
- `mode=DETERMINISTIC`, `fallbackReason=QUERY_REQUIRED`: no semantic query was supplied.
- `mode=DETERMINISTIC`, `fallbackReason=PROVIDER_FAILURE`: timeout, provider error or malformed
  provider output occurred.

`semanticScore` is present only for semantically ranked items. `provider` exposes the active
embedding and reranking adapter names, and `semanticLatencyMs` measures only provider-side
embedding/reranking work.

## NVIDIA acceptance gate

The real NVIDIA adapter must not replace this mock until a labeled multilingual query set shows:

- higher recall@k and NDCG@k than deterministic text search;
- zero ineligible or fabricated results;
- acceptable p95 provider latency and cost per query;
- successful timeout, malformed-response and provider-unavailable contract tests;
- identical booking correctness with semantic search enabled or disabled.
