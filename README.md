# 🏥 MediSureAI

> An agentic, self-reflective RAG platform for healthcare & insurance decision support.

MediSureAI helps **patients** and **doctors** make sense of insurance policies, medical
documents, billing, and drug information. It combines a Retrieval-Augmented Generation (RAG)
pipeline with a **Self-Reflective Language Model (SRLM)** reasoning loop that generates
multiple reasoning paths, critiques itself, scores candidates, and synthesises a single
grounded, citation-backed answer.

The system runs a **hybrid LLM setup**: a local **Ollama** model for fast, private foundation
tasks and embeddings, plus **Groq** (OpenAI-compatible) for high-quality cloud critique and
drug-formulary reasoning.

---

## ✨ Features

| Area | Capability |
|------|------------|
| 🔐 Auth | JWT access tokens + refresh-token rotation, role-based access (PATIENT / DOCTOR / ADMIN) |
| 📄 Documents | Upload PDF/DOCX → Apache Tika extraction → chunking → async processing via RabbitMQ |
| 🧠 RAG | OpenAI-compatible / Ollama embeddings stored in PostgreSQL **pgvector** (HNSW, cosine) |
| 🤖 Agentic query | Intent detection → task planning → tool orchestration → LLM reasoning (`/api/query`) |
| 🪞 SRLM pipeline | Multi-path reasoning → self-reflection → scoring → synthesis with citations (`/api/srlm`) |
| 🩺 Doctor portal | Manage profile, look up patients, create/update medical records, raise billing |
| 👤 Patient portal | Timeline, medical records, billing & payments, receipts, AI chat, drug formulary search |
| 💊 Drug formulary | Groq-backed drug information lookup |
| 🛡️ Safety | Confidence calibration, guardrails, retrieval feedback loop, session memory |

---

## 🧰 Tech Stack

**Backend**
- Java 21, Spring Boot 3.4.3, Spring AI 1.0.0-M4
- Spring Security (JWT, method-level RBAC), Spring Retry
- PostgreSQL + **pgvector** (embeddings), PostgreSQL (structured data), MongoDB (document text & chunks), RabbitMQ (async processing)
- Ollama (local LLM + embeddings) + Groq (cloud critique / formulary, OpenAI-compatible API)
- Apache Tika & Apache POI (text extraction), Maven

**Frontend**
- React 19, Vite 7, Tailwind CSS 4
- React Router 7, Framer Motion, Axios

---

## 🏗️ Architecture

```
                 React 19 + Vite + Tailwind (role-based UI)
                                │  HTTP / REST (Axios, JWT Bearer)
                                ▼
              Spring Boot REST API  ──  JWT filter + RBAC (@PreAuthorize)
                                │
   ┌───────────────┬───────────┴────────────┬─────────────────────┐
   ▼               ▼                        ▼                     ▼
 Document       Agentic Query           SRLM Pipeline         Domain APIs
 pipeline       /api/query              /api/srlm             /api/doctor
 (Tika →        intent → plan →         multi-path →          /api/patient
 chunk →        tools → LLM             reflect → score →     (records,
 RabbitMQ)                              synthesise            billing, chat)
   │               │                        │
   ▼               ▼                        ▼
 MongoDB      pgvector (HNSW)         Ollama (local) + Groq (cloud)
 (text +      embeddings              hybrid LLM reasoning
 chunks)
                                │
                                ▼
                 PostgreSQL (users, doctors, patients,
                 records, billing, receipts, drugs, refresh tokens)
```

### SRLM reasoning loop
```
query → retrieve (hybrid: embedding + keyword) → rerank
      → generate N reasoning paths (temperature for diversity)
      → self-reflect (validity, factual correctness, contradictions)
      → score (relevance + correctness + completeness)
      → synthesise top candidates → grounded answer + citations + confidence
```

---

## 📁 Repository Layout

```
MediSureAI/
├── Backend/                 # Spring Boot API (see Backend/README.md)
│   └── src/main/java/com/example/Backend/
│       ├── controller/      # REST endpoints
│       ├── service/         # agent, srlm, rag, document, embedding, medical, safety, …
│       ├── model/ dto/ repository/ config/ exception/
│       └── document/ vector/
├── Frontend/                # React app (see Frontend/README.md)
│   └── src/{pages,components,context,services}
├── test-data/               # Sample insurance / medical documents
├── docker-compose.yml       # pgvector, ollama, postgres, mongo, rabbitmq, backend, frontend
└── README.md
```

---

## 🚀 Quick Start

### Prerequisites
- Java 21, Maven 3.9+
- Node.js 20.19+ or 22.12+ (Vite 7 requirement)
- Docker & Docker Compose
- [Ollama](https://ollama.com) (if running the backend outside Docker)

### 1. Configure secrets

The backend reads secrets from environment variables (or a `Backend/.env` file). Required keys:

```properties
JWT_SECRET_KEY=<base64-256-bit-secret>
DB_USERNAME=postgres
DB_PASSWORD=<your-postgres-password>
OPENAI_API_KEY=<groq-api-key>          # used as the Groq (OpenAI-compatible) key
GROQ_FORMULARY_API_KEY=<groq-api-key>  # drug formulary lookups
```

> Never commit real secrets. `.env` files are git-ignored.

### 2. Start infrastructure

```bash
docker compose up -d pgvector medsureai-db mongodb rabbitmq ollama
```

Pull the local models used by Ollama:

```bash
docker exec -it ollama ollama pull llama3.2:1b
docker exec -it ollama ollama pull nomic-embed-text
```

### 3. Run the backend

```bash
cd Backend
./mvnw spring-boot:run        # Linux/macOS
mvnw.cmd spring-boot:run      # Windows
```
API: `http://localhost:8080`

### 4. Run the frontend

```bash
cd Frontend
npm install
npm run dev
```
App: `http://localhost:5173`

> Prefer containers? `docker compose up --build` brings up the full stack
> (backend, frontend, and all data services).

---

## 🔌 API Overview

Public: `POST /auth/register`, `/auth/login`, `/auth/refresh`. Everything else requires a
`Authorization: Bearer <token>` header.

| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | `/auth/register` `/login` `/refresh` `/logout` | Public / Bearer | Auth & token rotation |
| POST | `/api/documents/upload` | Bearer | Upload PDF/DOCX (multipart, async) |
| GET / DELETE | `/api/documents`, `/api/documents/{id}` | Bearer | List / fetch / delete documents |
| POST | `/api/query`, `/api/query/debug` | Bearer | Agentic RAG query (with optional trace) |
| POST | `/api/srlm`, `/api/srlm/debug` | Bearer | Self-reflective reasoning (with optional trace) |
| POST | `/api/chat/ask` | Bearer | Simple RAG chat |
| POST | `/api/search/embeddings/generate` | Bearer | Generate embeddings |
| GET / DELETE | `/api/search/index/stats`, `/index/clear` | ADMIN | Vector index admin |
| * | `/api/doctor/**` | DOCTOR | Profile, patient lookup, records, billing |
| * | `/api/patient/**` | PATIENT | Timeline, records, billing, payments, chat, formulary |

See [`Backend/README.md`](Backend/README.md) for the full endpoint reference.

---

## 🧪 Testing

```bash
cd Backend
./mvnw test
```
Unit tests cover the SRLM services (constraint reasoning, contradiction detection, financial
validation, clause classification, reranking) and the medical knowledge/safety services.

---

## 📚 More Documentation
- [Backend guide](Backend/README.md) — setup, configuration, endpoints, package layout
- [Frontend guide](Frontend/README.md) — pages, contexts, API layer
- [Frontend troubleshooting](Frontend/TROUBLESHOOTING.md) — common dev-server issues
- [Auth test guide](Backend/AUTHENTICATION_TEST_GUIDE.md) — exercising the JWT flow

---

## 📝 License
MIT
