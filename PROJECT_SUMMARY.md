# MediSureAI — Project Summary

## Overview
Agentic SRLM-RAG Healthcare Insurance AI system built with Spring Boot 3.4.3 / Java 21.
Helps patients, doctors, and admins query insurance policies, understand coverage, and
interpret medical documents through a multi-stage AI reasoning pipeline.

---

## Architecture

```
Client
  │
  ▼
Spring Boot REST API  (JWT-authenticated)
  │
  ├─ Phase 1: Auth Layer
  │     JWT (15 min access) + Refresh Token Rotation (7 days) + RBAC
  │     Roles: PATIENT | DOCTOR | ADMIN
  │
  ├─ Phase 2: Document Pipeline
  │     Upload → Apache Tika extraction → Chunking → MongoDB → RabbitMQ async
  │
  ├─ Phase 3: Vector Store
  │     OpenAI text-embedding-3-small → FAISS-style in-memory index (cosine)
  │     File persistence via Java serialisation
  │
  ├─ Phase 4: Agentic Core  (POST /api/query)
  │     IntentDetection → TaskPlanning → VectorSearch → LLM Reasoning
  │
  ├─ Phase 5: SRLM Pipeline  (POST /api/srlm)
  │     IntentDetection → VectorSearch → MultiReasoning (3 paths, temp=0.7)
  │     → SelfReflection → Scoring → Synthesis → Final Answer
  │
  └─ Phase 6: Production Enhancements
        ToolOrchestration + ConfidenceCalibration + RetrievalFeedbackLoop
        + SafetyGuardrails + SessionMemory
```

---

## Phase Details

### Phase 1 — JWT Authentication
| Item | Detail |
|------|--------|
| Access token TTL | 15 minutes |
| Refresh token TTL | 7 days |
| Rotation | Old refresh token deleted on each use |
| Roles | PATIENT, DOCTOR, ADMIN (`@PreAuthorize`) |
| Endpoints | `POST /auth/register`, `/login`, `/refresh`, `/logout` |

### Phase 2 — Document Processing
| Item | Detail |
|------|--------|
| Accepted formats | PDF, DOCX |
| Extraction | Apache Tika 2.9.1 |
| Chunking | Sentence-boundary-aware, 500 chars, 50-char overlap |
| Storage | Full text → MongoDB (`DocumentContent`); Chunks → MongoDB (`DocumentChunk`) |
| Metadata | PostgreSQL (`Document` entity) |
| Async queue | RabbitMQ — `document.processing.queue` |
| Endpoints | `POST /api/documents/upload`, `GET /`, `GET /{id}`, `DELETE /{id}` |

### Phase 3 — Vector Database
| Item | Detail |
|------|--------|
| Embedding model | `text-embedding-3-small` (1536 dims) |
| Index type | In-memory `ConcurrentHashMap<String, float[]>` (cosine similarity) |
| Persistence | `~/medisure-ai/faiss-index/vector_index.dat` — saved on shutdown |
| Top-K | 5 results, similarity threshold 0.7 |
| Endpoints | `POST /api/search`, `/embeddings/generate`, `/index/stats`, `/index/clear` |

### Phase 4 — Agentic Core
| Item | Detail |
|------|--------|
| Intents | COVERAGE_QUESTION, CLAIM_STATUS, POLICY_DETAILS, MEDICAL_INTERPRETATION, BILLING_INQUIRY, GENERAL_INFO, UNKNOWN |
| Pipeline | Intent → Plan → VectorSearch → LLM (gpt-4o-mini) |
| Trace | `POST /api/query/debug` includes full execution trace |
| Retry | `@Retryable` — 3 attempts, 2 s exponential back-off |

### Phase 5 — SRLM Reasoning Pipeline
| Item | Detail |
|------|--------|
| Reasoning paths | 3 (configurable), temperature=0.7 for diversity |
| Path types | POLICY_FOCUSED, COVERAGE_FOCUSED, BALANCED, CLAIM_ORIENTED, PATIENT_BENEFIT |
| Reflection | LLM critically evaluates each candidate (isValid, factsCorrect, hasContradictions) |
| Scoring | Relevance + Correctness + Completeness (1-10 each); penalty for flagged answers |
| Synthesis | Top-2 candidates merged into one coherent final answer |
| Endpoints | `POST /api/srlm`, `POST /api/srlm/debug` |

---

## Key Configuration (`application.properties`)

```properties
# JWT
jwt.access.expiration=900000       # 15 min
jwt.refresh.expiration=604800000   # 7 days

# OpenAI
openai.model.completion=gpt-4o-mini
openai.model.embedding=text-embedding-3-small
openai.model.temperature=0.3

# SRLM
srlm.reasoning.paths=3
srlm.reasoning.temperature=0.7
srlm.reflection.enabled=true
srlm.scoring.min-confidence=6.0
srlm.synthesis.enabled=true

# Phase 6
srlm.feedback.enabled=true
srlm.feedback.confidence-threshold=6.0
safety.min-confidence=4.0
session.memory.max-queries=10
```

---

## Tech Stack
- **Java 21** / **Spring Boot 3.4.3**
- **PostgreSQL** — users, refresh tokens, document metadata
- **MongoDB** — document full-text and chunks
- **RabbitMQ** — async document processing
- **OpenAI API** (`com.theokanning.openai-gpt3-java:service:0.18.2`)
- **Apache Tika 2.9.1** — text extraction
- **Spring Security** — JWT filter chain + method-level RBAC
- **Spring Retry** — resilient LLM calls
- **Lombok** — boilerplate reduction
- **Docker Compose** — PostgreSQL + RabbitMQ + MongoDB

---

## API Quick Reference

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/auth/register` | Public | Register new user |
| POST | `/auth/login` | Public | Login, get tokens |
| POST | `/auth/refresh` | Public | Rotate refresh token |
| POST | `/auth/logout` | Bearer | Invalidate refresh token |
| POST | `/api/documents/upload` | Bearer | Upload PDF/DOCX |
| GET | `/api/documents` | Bearer | List user documents |
| GET | `/api/documents/{id}` | Bearer | Document metadata |
| DELETE | `/api/documents/{id}` | Bearer | Delete document |
| POST | `/api/search` | Bearer | Semantic search |
| POST | `/api/search/embeddings/generate` | Bearer | Generate embeddings |
| GET | `/api/search/index/stats` | ADMIN | Vector index stats |
| DELETE | `/api/search/index/clear` | ADMIN | Clear vector index |
| POST | `/api/query` | Bearer | Phase 4 agent query |
| POST | `/api/query/debug` | Bearer | Phase 4 with trace |
| POST | `/api/srlm` | Bearer | Phase 5 SRLM query |
| POST | `/api/srlm/debug` | Bearer | Phase 5 with full trace |

### Phase 6 — Enhanced response fields (`/api/query` and `/api/srlm`)
Both endpoints now return:
- `overallConfidence` (0-10) — calibrated from intent, retrieval, and context quality
- `confidenceLevel` — `HIGH` / `MEDIUM` / `LOW`
- `confidenceFactors` — per-factor breakdown
- `safetyWarnings` — list of guardrail flags (null when clean)
- `sessionId` — pass back on follow-up queries for conversation memory