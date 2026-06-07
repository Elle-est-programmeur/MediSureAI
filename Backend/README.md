# 🏥 MediSureAI — Backend

Spring Boot backend for MediSureAI: an agentic, self-reflective RAG platform for healthcare &
insurance decision support. It exposes a JWT-secured REST API over a hybrid LLM stack
(local Ollama + cloud Groq) with a multi-stage SRLM reasoning pipeline.

## 🛠️ Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Language |
| Spring Boot | 3.4.3 | Application framework |
| Spring AI | 1.0.0-M4 | LLM / vector-store integration |
| Spring Security | 6.x | JWT auth + method-level RBAC |
| PostgreSQL + pgvector | pg16 | Vector embeddings (HNSW, cosine) |
| PostgreSQL | pg16 | Structured data (users, records, billing…) |
| MongoDB | latest | Document full text + chunks |
| RabbitMQ | 3 | Async document processing |
| Ollama | latest | Local LLM (`llama3.2:1b`) + embeddings (`nomic-embed-text`) |
| Groq | — | Cloud critique / drug formulary (`llama-3.3-70b-versatile`, OpenAI-compatible) |
| Apache Tika / POI | 2.9.1 / 5.2.5 | PDF & DOCX text extraction |
| Maven | 3.9+ | Build |

## ⚡ Quick Start

### Prerequisites
- Java 21, Maven 3.9+ (or use the bundled `./mvnw`)
- Docker & Docker Compose (for data services)
- Ollama with the required models pulled

### 1. Start data services

From the repository root:

```bash
docker compose up -d pgvector medsureai-db mongodb rabbitmq ollama
```

| Service | Host port | Notes |
|---------|-----------|-------|
| `pgvector` | 5432 | Embeddings store (`vectordb`) |
| `medsureai-db` | 5433 | Structured data (`medsureai`) |
| `mongodb` | 27017 | Document text & chunks |
| `rabbitmq` | 5672 / 15672 | Broker / management UI |
| `ollama` | 11434 | Local LLM runtime |

Pull the local models:

```bash
docker exec -it ollama ollama pull llama3.2:1b
docker exec -it ollama ollama pull nomic-embed-text
```

### 2. Configure secrets

`application.properties` reads secrets from environment variables (or an optional
`Backend/.env` properties file — git-ignored). Set:

```properties
JWT_SECRET_KEY=<base64-encoded 256-bit secret>
DB_USERNAME=postgres
DB_PASSWORD=<password for medsureai-db>
OPENAI_API_KEY=<groq-api-key>          # Spring AI OpenAI client points at Groq
GROQ_FORMULARY_API_KEY=<groq-api-key>  # drug formulary REST calls
```

> The OpenAI-compatible client is intentionally pointed at `https://api.groq.com/openai`.
> Local embeddings run on Ollama (`nomic-embed-text`, 768 dims), so no paid embedding calls
> are made by default.

### 3. Run

```bash
cd Backend
./mvnw spring-boot:run        # Linux/macOS
mvnw.cmd spring-boot:run      # Windows
```

The API starts on `http://localhost:8080`. Tables are auto-created by Hibernate
(`ddl-auto=update`).

## 📁 Project Structure

```
Backend/src/main/java/com/example/Backend/
├── BackendApplication.java
├── config/            # Security, JWT filter, CORS, Mongo, RabbitMQ, vector store, model, retry
├── controller/        # Auth, Chat, Document, Query, SRLMQuery, Search, Doctor, Patient
├── dto/               # Request/response payloads (auth, query, srlm, billing, records…)
├── model/             # JPA entities & enums (Users, Doctor, Patient, Billing, Drug, Role…)
├── repository/        # Spring Data JPA + Mongo repositories
├── document/          # Mongo documents (DocumentContent, DocumentChunk)
├── vector/            # Vector index & search result types
├── exception/         # Custom exceptions + GlobalExceptionHandler
├── security/jwt/      # JwtAuthenticationEntryPoint
└── service/
    ├── agent/         # AgentOrchestrator, IntentDetection, TaskPlanning, ToolExecutor
    ├── srlm/          # Multi-path reasoning, reflection, scoring, synthesis, retrieval, rerank
    ├── rag/           # ContextBuilderService
    ├── document/      # Chunking, processing, storage, text extraction
    ├── embedding/     # Embedding generation & service
    ├── vector/        # Semantic search & vector store
    ├── llm/           # LLMService, CritiqueLLMService, PromptTemplateService
    ├── medical/       # Drug knowledge & response safety
    ├── tools/         # Tool abstraction + VectorSearch/Metadata/DrugKnowledge/PatientData tools
    ├── confidence/    # Confidence calibration
    ├── safety/        # Guardrails
    ├── memory/        # Session memory
    └── messaging/     # RabbitMQ producer/consumer
```

## 🔌 API Reference

Base URL: `http://localhost:8080`. Public endpoints are `/auth/register`, `/auth/login`,
`/auth/refresh`. All other endpoints require `Authorization: Bearer <accessToken>`.

### Auth — `/auth`
| Method | Path | Description |
|--------|------|-------------|
| POST | `/register` | Register a user |
| POST | `/login` | Login, returns access + refresh tokens |
| POST | `/refresh` | Rotate refresh token, get new access token |
| POST | `/logout` | Invalidate refresh token |

### Documents — `/api/documents`
| Method | Path | Description |
|--------|------|-------------|
| POST | `/upload` | Upload PDF/DOCX (`multipart/form-data`: `files`, `documentType`); processed asynchronously |
| GET | `/` | List the caller's documents |
| GET | `/{id}` | Document metadata |
| DELETE | `/{id}` | Delete a document |
| DELETE | `/clear` | Clear the caller's documents |

### Reasoning
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/query` | Agentic RAG query (intent → plan → tools → LLM) |
| POST | `/api/query/debug` | Same, with full execution trace |
| POST | `/api/srlm` | Self-reflective multi-path reasoning |
| POST | `/api/srlm/debug` | Same, with full trace |
| POST | `/api/chat/ask` | Simple RAG chat |

### Search — `/api/search`
| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | `/embeddings/generate` | Bearer | Generate embeddings |
| GET | `/index/stats` | ADMIN | Vector index statistics |
| DELETE | `/index/clear` | ADMIN | Clear the vector index |

### Doctor — `/api/doctor` (`hasRole('DOCTOR')`)
`POST/GET /profile` · `GET /patients` · `GET /patients/lookup` ·
`GET /patients/{userId}/records` · `POST/GET /records` · `GET/PUT/DELETE /records/{id}` ·
`POST/GET /billing`

### Patient — `/api/patient` (`hasRole('PATIENT')`)
`GET /timeline` · `GET /formulary-search` · `GET/POST /profile` · `GET /records` ·
`GET /billing` · `POST /billing/{id}/pay` · `GET /billing/{id}/receipt` · `POST /chat`

## ⚙️ Key Configuration (`application.properties`)

```properties
# JWT
jwt.access.expiration=3600000        # access token (ms)
jwt.refresh.expiration=604800000     # refresh token (7 days)

# Hybrid LLM — Groq cloud critique (OpenAI-compatible)
spring.ai.openai.base-url=https://api.groq.com/openai
spring.ai.openai.chat.options.model=llama-3.3-70b-versatile
spring.ai.openai.embedding.enabled=false

# Hybrid LLM — local Ollama foundation + embeddings
spring.ai.ollama.chat.options.model=llama3.2:1b
spring.ai.ollama.embedding.options.model=nomic-embed-text

# Vector store (pgvector, 768 dims to match nomic-embed-text)
spring.ai.vectorstore.pgvector.dimensions=768
spring.ai.vectorstore.pgvector.distance-type=COSINE_DISTANCE
spring.ai.vectorstore.pgvector.index-type=HNSW

# SRLM pipeline
srlm.reasoning.paths=2
srlm.reasoning.temperature=0.7
srlm.reflection.enabled=true
srlm.scoring.min-confidence=6.0
srlm.synthesis.enabled=true

# Evidence-grounded retrieval
srlm.retrieval.embedding-weight=0.7
srlm.retrieval.keyword-weight=0.3
srlm.retrieval.default-topk=8
srlm.validation.min-final-score=0.45

# Safety & memory
srlm.feedback.enabled=true
safety.min-confidence=4.0
session.memory.max-queries=10
```

## 🧪 Testing

```bash
./mvnw test
```

Unit tests live under `src/test/java/com/example/Backend` and cover the SRLM services
(`ConstraintReasoning`, `ContradictionDetection`, `FinancialCalculationValidator`,
`PolicyClauseClassifier`, `RetrievalReranker`, `ContextValidation`, `InsuranceKeywords`)
and the medical services (`DrugKnowledgeService`, `MedicalResponseSafetyService`).

## 🐳 Docker

A `Dockerfile` is provided, and the root `docker-compose.yml` wires the backend together with
all data services. To build & run everything:

```bash
docker compose up --build
```

For the JWT flow walkthrough, see [`AUTHENTICATION_TEST_GUIDE.md`](AUTHENTICATION_TEST_GUIDE.md).
