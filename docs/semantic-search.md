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
BOOKIFY_SEMANTIC_TIMEOUT_MS=5000
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

## Labeled dataset and metrics

The versioned evaluation set lives at
`backend/src/test/resources/search-evaluation/bookify-labeled-queries-v1.json`. Version 1
contains 12 Spanish/English queries, graded relevance labels and one deliberately ineligible
document. Labels are product hypotheses for regression testing; they must be reviewed with real
users before being treated as a production relevance benchmark.

`SearchQualityEvaluator` reports:

- macro recall@k;
- graded NDCG@k;
- mean and p95 end-to-end strategy latency;
- count of ineligible returned identifiers;
- provider request count;
- estimated cost when an account-specific price per 1,000 requests is configured.

The reproducible local result for dataset v1 at `k=3` is:

| Strategy | Recall@3 | NDCG@3 | Ineligible |
| --- | ---: | ---: | ---: |
| Deterministic token baseline | 0.5000 | 0.5280 | 0 |
| Mock semantic adapters | 0.8750 | 0.8782 | 0 |

Mock latency is intentionally not recorded as a product claim because it does no network or
model inference. Its quality result validates the evaluation harness and concept fixtures only.

The first live NVIDIA hosted-API evaluation, executed on 2026-08-03 with the same dataset and
`k=3`, produced:

| Strategy | Recall@3 | NDCG@3 | Mean latency | p95 latency | Requests | Estimated cost |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Deterministic token baseline | 0.5000 | 0.5280 | — | — | 0 | USD 0 |
| NVIDIA embedding + reranking | 1.0000 | 1.0000 | 1,182.21 ms | 3,139.37 ms | 36 | Not configured |

This result validates connectivity and shows a relevance improvement on the synthetic v1 labels;
it is not yet a production claim. The dataset must grow with anonymized real-user queries, and
latency, quota usage and account-specific cost must be measured again before enabling NVIDIA by
default.

## NVIDIA adapters

Set `BOOKIFY_SEMANTIC_PROVIDER=nvidia` and provide `NVIDIA_API_KEY` at runtime. The implementation
uses the current multilingual text models:

- `nvidia/llama-nemotron-embed-1b-v2`, batching query and passage embeddings separately;
- `nvidia/llama-nemotron-rerank-1b-v2`, reranking the bounded embedded candidates.

The endpoints and model names are configurable for hosted NVIDIA APIs or an HTTPS NIM deployment.
Remote plain HTTP endpoints are rejected; localhost HTTP remains available for contract tests and
local NIM development. The API key is never logged or committed.

Run the live benchmark explicitly:

```bash
BOOKIFY_RUN_NVIDIA_LIVE=true \
NVIDIA_API_KEY='your-runtime-secret' \
./mvnw -Dtest=NvidiaLiveEvaluationTest test
```

Optionally set `BOOKIFY_NVIDIA_COST_PER_1000_REQUESTS_USD` using the price applicable to the
selected NVIDIA account/deployment. If it is absent, the report says `NOT_CONFIGURED`; Bookify
does not invent a cost. Each evaluated query currently performs two batched embedding requests
and one reranking request.

Without `BOOKIFY_RUN_NVIDIA_LIVE=true`, the live test is skipped so CI never consumes provider
credits. Contract tests exercise authentication, request shapes, query/passage modes, out-of-order
embedding indexes, reranking logits and insecure configuration against a local HTTP server.

Official references:

- [NVIDIA llama-nemotron-embed-1b-v2 API](https://docs.api.nvidia.com/nim/re/reference/nvidia-llama-nemotron-embed-1b-v2-infer)
- [NVIDIA llama-nemotron-rerank-1b-v2 API](https://docs.api.nvidia.com/nim/reference/nvidia-llama-nemotron-rerank-1b-v2-infer)
- [NVIDIA reranking model card](https://build.nvidia.com/nvidia/llama-nemotron-rerank-1b-v2/modelcard)
