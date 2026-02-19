# System Architecture

## Project Overview

MediSureAI is an AI-powered healthcare and insurance decision support platform designed to bridge the gap between clinical decision-making, treatment validation, and insurance claim processing. The system serves three primary user roles—Patients, Doctors, and Hospital Administrators—by providing intelligent, explainable, and automated assistance for insurance eligibility verification, treatment plan validation, drug safety checking, and medical bill explanation.

At its core, MediSureAI employs an **Agentic AI architecture** powered by **Retrieval-Augmented Generation (RAG)** to combine structured data from relational databases with unstructured knowledge from medical guidelines and insurance policy documents. The agent orchestrator intelligently selects and executes domain-specific tools, synthesizes results using a Large Language Model (LLM), and generates transparent, citation-backed explanations for every decision.

The platform addresses critical pain points in healthcare:
- **Claim rejection opacity**: Patients receive rejections without understanding why
- **Coverage uncertainty**: Lack of upfront clarity on what treatments are covered
- **Drug interaction risks**: Manual checking is error-prone and time-consuming
- **Treatment compliance**: Doctors need real-time guideline validation
- **Administrative burden**: Manual claim processing is slow and inconsistent

MediSureAI automates these workflows while maintaining full explainability, ensuring that every AI-generated decision can be traced back to specific policy clauses, medical guidelines, or clinical rules.

---

## High-Level Architecture

The system follows a layered architecture with clear separation of concerns:

```
┌───────────────────────────────────────────────────────────────────────┐
│                          USER LAYER                                   │
│   Patient Portal  |  Doctor Dashboard  |  Admin Control Panel        │
└──────────────────────────────┬────────────────────────────────────────┘
                               │ HTTPS / REST API
┌──────────────────────────────┴────────────────────────────────────────┐
│                      PRESENTATION LAYER                               │
│            React Frontend (Vite, TypeScript, Tailwind CSS)            │
│  ┌──────────────┬─────────────────┬────────────────┬───────────────┐ │
│  │ Auth Pages   │ Claim Submission│ Policy Analyzer│ Decision View │ │
│  └──────────────┴─────────────────┴────────────────┴───────────────┘ │
└──────────────────────────────┬────────────────────────────────────────┘
                               │ Axios HTTP Client
┌──────────────────────────────┴────────────────────────────────────────┐
│                       API GATEWAY LAYER                               │
│            Spring Boot REST Controllers (JWT Auth)                    │
│  ┌──────────┬─────────┬───────────┬─────────┬─────────────────────┐  │
│  │ Auth API │Claim API│Treatment  │Policy   │ Decision API        │  │
│  │          │         │API        │API      │                     │  │
│  └──────────┴─────────┴───────────┴─────────┴─────────────────────┘  │
└──────────────────────────────┬────────────────────────────────────────┘
                               │ Service Layer Invocation
┌──────────────────────────────┴────────────────────────────────────────┐
│                      BUSINESS LOGIC LAYER                             │
│                  Spring Services + Agent Orchestrator                 │
│  ┌──────────────────────────────────────────────────────────────────┐ │
│  │              AGENT ORCHESTRATOR (Core Brain)                     │ │
│  │  Intent Detection → Tool Selection → Execution → Synthesis      │ │
│  └──────────┬──────────────┬──────────────┬────────────┬───────────┘ │
│             ↓              ↓              ↓            ↓              │
│    ┌────────────┐  ┌────────────┐  ┌──────────┐  ┌─────────────┐    │
│    │ RAG        │  │ Claim      │  │ Drug     │  │ Guideline   │    │
│    │ Pipeline   │  │ Validation │  │ Safety   │  │ Validation  │    │
│    │ Service    │  │ Tool       │  │ Tool     │  │ Tool        │    │
│    └─────┬──────┘  └──────┬─────┘  └─────┬────┘  └──────┬──────┘    │
│          │                │               │              │            │
│  ┌───────┴────────────────┴───────────────┴──────────────┴────────┐  │
│  │            EXPLAINABILITY ENGINE                                │  │
│  │   Decision + Reasoning + Confidence Score + Citations          │  │
│  └─────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────┬────────────────────────────────────────┘
                               │ JPA / JDBC / HTTP
┌──────────────────────────────┴────────────────────────────────────────┐
│                         DATA LAYER                                    │
│  ┌───────────────┐  ┌──────────────┐  ┌────────────┐  ┌───────────┐ │
│  │ PostgreSQL    │  │ ChromaDB     │  │ Drug       │  │ Coverage  │ │
│  │ (Relational)  │  │ (Vector DB)  │  │ Database   │  │ Rules DB  │ │
│  │               │  │              │  │            │  │           │ │
│  │ Users         │  │ Policy Docs  │  │ Interaction│  │ Clause    │ │
│  │ Claims        │  │ Guidelines   │  │ Data       │  │ Mapping   │ │
│  │ Treatments    │  │ Embeddings   │  │            │  │           │ │
│  │ Policies      │  │              │  │            │  │           │ │
│  └───────────────┘  └──────────────┘  └────────────┘  └───────────┘ │
└──────────────────────────────┬────────────────────────────────────────┘
                               │ API Calls
┌──────────────────────────────┴────────────────────────────────────────┐
│                        AI/LLM LAYER                                   │
│  ┌──────────────────────────────────────────────────────────────────┐ │
│  │          OpenAI GPT-4 / Ollama (Local LLM)                      │ │
│  │                                                                  │ │
│  │  Input: System Prompt + Retrieved Context + Tool Results        │ │
│  │  Output: Structured Decision + Reasoning + Confidence           │ │
│  └──────────────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────────────┘
```

---

## Layer Descriptions

### 1. User Layer

**Responsibility**: Provide role-based interfaces for different user personas.

The user layer consists of three distinct portals:

- **Patient Portal**: Allows patients to upload insurance policies, submit claim requests with treatment details, view claim status, understand bill breakdowns, and receive AI-generated explanations of coverage decisions.

- **Doctor Dashboard**: Enables doctors to validate treatment plans against clinical guidelines, check drug interactions for prescribed medications, generate discharge instructions, and receive compliance warnings before finalizing treatment.

- **Admin Control Panel**: Used by hospital administrators and insurance claim reviewers to monitor claim processing queues, review flagged cases, audit agent decisions, view system analytics, and configure policy rules.

Each portal is isolated with role-based access control implemented at both frontend routing and backend API levels.

---

### 2. Presentation Layer (React Frontend)

**Responsibility**: Render UI components, manage client-side state, handle user interactions, and communicate with backend APIs.

Built with modern web technologies:
- **React 18** with functional components and hooks
- **TypeScript** for type safety
- **Vite** for fast development and optimized production builds
- **Tailwind CSS** for responsive, utility-first styling
- **Zustand** for lightweight, scalable state management
- **React Router** for client-side routing with role-based guards
- **Axios** for HTTP requests with request/response interceptors

Key features:
- Responsive design for desktop and tablet devices
- Real-time status updates using polling or WebSocket connections
- Form validation with client-side checks before submission
- Citation display with clickable links to source documents
- Confidence score visualization with color-coded indicators
- Reasoning trace display showing agent's step-by-step thought process

The frontend maintains a clean separation between presentational components (UI rendering) and container components (data fetching and state management).

---

### 3. API Gateway Layer (Spring Boot Controllers)

**Responsibility**: Expose RESTful endpoints, handle authentication/authorization, validate requests, and route to appropriate services.

Implemented using Spring Boot 3 with Spring Security for JWT-based authentication:

- **AuthController**: User registration, login, token refresh, and role validation
- **ClaimController**: Submit claims, retrieve claim details, update status, list patient claims
- **TreatmentController**: Validate treatment plans, check drug interactions, guideline compliance
- **PolicyController**: Upload policy documents, trigger RAG ingestion, analyze coverage
- **DecisionController**: Generate AI decisions, retrieve decision details with explanations

All endpoints follow RESTful conventions with proper HTTP methods, status codes, and error responses. CORS is configured to allow frontend access. Rate limiting prevents API abuse. Request/response payloads are validated using Bean Validation annotations.

---

### 4. Business Logic Layer (Spring Services + Agent Orchestrator)

**Responsibility**: Implement core business logic, orchestrate agent workflows, coordinate tool execution, and generate explainable decisions.

This is the brain of MediSureAI. The Agent Orchestrator is a Spring service that:

1. **Receives decision requests** from controllers with context (claim ID, patient data, policy data, treatment details)
2. **Detects intent** by analyzing the request type and available data
3. **Selects appropriate tools** based on intent (policy retrieval, drug checking, coverage calculation)
4. **Executes tools in parallel** where possible to minimize latency
5. **Observes results** and determines if additional retrieval is needed
6. **Calls the LLM** with system prompt, retrieved context, and tool results
7. **Parses LLM output** into structured format
8. **Generates explanations** with citations, confidence scores, and reasoning traces
9. **Returns decision response** to the controller

The orchestrator uses Spring AI's function calling capabilities to dynamically invoke tools based on LLM recommendations or predefined rules.

Domain-specific tools include:
- **RAG Pipeline Service**: Ingests documents, generates embeddings, performs similarity search
- **Claim Validation Tool**: Checks claim completeness, validates data integrity
- **Drug Safety Tool**: Queries drug interaction databases for contraindications
- **Guideline Validation Tool**: Compares treatment plans against clinical best practices
- **Coverage Calculator Tool**: Applies policy rules to compute payable amounts

The Explainability Engine post-processes LLM outputs to extract decision outcomes, map reasoning to source citations, calculate confidence scores, identify warnings, and generate patient-friendly summaries.

---

### 5. Data Layer

**Responsibility**: Persist and retrieve structured and unstructured data.

MediSureAI employs a polyglot persistence strategy:

**PostgreSQL (Relational Database)**
- Stores structured operational data
- Tables: users, policies, treatments, claims, decisions, audit_logs
- Enforces referential integrity, supports complex queries
- Used for transactional operations (ACID compliance)
- Indexed on frequently queried columns for performance

**ChromaDB (Vector Database, port 8000)**
- Stores document embeddings for semantic search
- Collections: insurance_policies, medical_guidelines, drug_information
- Supports cosine similarity search for RAG retrieval
- Stores metadata alongside vectors for filtering
- Enables hybrid search combining semantic and keyword matching

**Drug Data Source**
- Drug interaction data is stored in ChromaDB (vector DB, port 8000) as the single source of truth for semantic search and retrieval.
- A small PostgreSQL seed table may be used for common interactions, but all agent queries use ChromaDB.

All database connections use connection pooling for efficiency. Spring Data JPA is used for PostgreSQL access with repository pattern. ChromaDB is accessed via HTTP client or SDK.

---

### 6. AI/LLM Layer

**Responsibility**: Provide natural language understanding, reasoning, and generation capabilities.

MediSureAI integrates with Large Language Models for:
- Understanding natural language treatment descriptions
- Reasoning over retrieved policy clauses and medical guidelines
- Generating human-readable explanations
- Extracting structured data from unstructured documents
- Synthesizing information from multiple sources

Supported LLM providers:
- **OpenAI GPT-4** (cloud-based, high accuracy)
- **Ollama** (self-hosted, privacy-focused)

LLM interaction pattern:
1. Construct prompt with system role, task description, retrieved context, and output format specification
2. Send prompt to LLM API with temperature and max tokens configured
3. Receive response and parse as JSON or structured text
4. Validate output format and retry if malformed
5. Cache frequent queries to reduce API costs

The system uses structured prompts with few-shot examples to ensure consistent output format. Function calling is used where supported to enable the LLM to request tool execution.

---

## Component Responsibility Table

| Component | Primary Responsibility | Key Technologies |
|-----------|------------------------|------------------|
| **Patient Portal** | Enable patients to submit claims, view decisions, understand coverage | React, TypeScript, Tailwind |
| **Doctor Dashboard** | Validate treatments, check drug safety, ensure guideline compliance | React, TypeScript, Tailwind |
| **Admin Panel** | Monitor system, review claims, audit decisions, configure rules | React, TypeScript, Tailwind |
| **Auth Controller** | User authentication, JWT token management, role-based access control | Spring Security, JWT |
| **Claim Service** | Claim submission, status tracking, claim history management | Spring Boot, JPA |
| **Treatment Service** | Treatment validation, guideline checking, discharge instructions | Spring Boot |
| **Policy Service** | Policy document upload, parsing, RAG ingestion, coverage analysis | Spring Boot |
| **Decision Service** | Trigger agent orchestrator, store decisions, retrieve decision history | Spring Boot |
| **Agent Orchestrator** | Intent detection, tool selection, execution coordination, LLM synthesis | Spring AI |
| **RAG Pipeline** | Document ingestion, text chunking, embedding generation, similarity search | Spring AI, ChromaDB |
| **Claim Validation Tool** | Verify claim completeness, check data integrity, validate relationships | Java |
| **Drug Safety Tool** | Query drug interaction database, detect contraindications, assess risk | Java, External API |
| **Guideline Validator Tool** | Compare treatment against clinical guidelines, flag non-compliance | Java |
| **Coverage Calculator Tool** | Apply policy rules, compute co-pays, check limits, calculate payable amount | Java |
| **Explainability Engine** | Extract decision outcome, map citations, score confidence, generate summary | Java |
| **PostgreSQL Database** | Store users, claims, treatments, policies, decisions, audit logs | PostgreSQL 15+ |
| **ChromaDB** | Store and retrieve document embeddings for semantic search | ChromaDB |
| **LLM (OpenAI/Ollama)** | Natural language understanding, reasoning, explanation generation | GPT-4, Ollama |

---

## Key Design Decisions

### 1. Why Agentic AI Instead of Simple Prompt-Response?

**Decision**: Implement an agent orchestrator with tool-calling capabilities rather than a single LLM query.

**Rationale**:
- Healthcare decisions require multi-step reasoning across multiple data sources
- Different queries need different tools (not all claims need drug checking)
- Agents can retry failed retrievals or request additional information
- Enables parallel tool execution for faster response times
- Provides structured workflow for auditing and compliance

**Trade-off**: Increased complexity in implementation and debugging, but significantly better accuracy and explainability.

---

### 2. Why RAG Over Fine-Tuning?

**Decision**: Use Retrieval-Augmented Generation instead of fine-tuning LLMs on domain data.

**Rationale**:
- Insurance policies and medical guidelines change frequently
- RAG allows instant updates without retraining models
- Provides citation links to source documents for transparency
- Lower cost than maintaining fine-tuned models
- Better knowledge freshness and accuracy

**Trade-off**: Retrieval quality is critical; requires careful chunking and embedding strategies.

---

### 3. Why Polyglot Persistence (PostgreSQL + ChromaDB)?

**Decision**: Use separate databases for structured and vector data.

**Rationale**:
- PostgreSQL excels at relational data, ACID transactions, complex joins
- ChromaDB is optimized for vector similarity search
- Hybrid approach leverages strengths of each database
- Allows independent scaling of SQL and vector workloads

**Trade-off**: Additional operational complexity, but better performance and flexibility.

---

### 4. Why Spring Boot Instead of Microservices?

**Decision**: Build backend as a modular monolith rather than microservices architecture.

**Rationale**:
- Faster development and deployment for an MVP
- Easier debugging and testing with single deployment unit
- Sufficient performance for expected load
- Modular design allows future decomposition if needed
- Reduces operational overhead (no service mesh, API gateway complexity)

**Trade-off**: Scaling requires scaling entire application, but acceptable for initial phase.

---

### 5. Why JWT for Authentication?

**Decision**: Use JSON Web Tokens for stateless authentication.

**Rationale**:
- Stateless authentication scales horizontally without session store
- Tokens include role information for role-based access control
- Works seamlessly with RESTful API design
- Frontend can store tokens securely in memory or httpOnly cookies

**Trade-off**: Token revocation is harder than session invalidation, mitigated by short expiration times and refresh token rotation.

---

### 6. Why Explainability Engine as Separate Component?

**Decision**: Dedicated service for post-processing LLM outputs into structured explanations.

**Rationale**:
- LLM outputs can be inconsistent or incomplete
- Centralized logic for citation extraction, confidence scoring
- Enables A/B testing of explanation formats
- Provides consistent explanation structure across decision types

**Trade-off**: Additional processing step, but critical for user trust and regulatory compliance.

---

## Data Flow Example

**Scenario**: Patient submits a claim for diabetes treatment.

1. **User Action**: Patient fills ClaimForm in Patient Portal with treatment details, uploads policy PDF
2. **Frontend**: ClaimForm validates input, calls `POST /api/v1/claims/submit` via Axios
3. **API Gateway**: ClaimController receives request, validates JWT, checks role
4. **Service Layer**: ClaimService creates Claim record in PostgreSQL
5. **Agent Trigger**: ClaimService calls AgentOrchestrator with DecisionRequest
6. **Intent Detection**: Orchestrator identifies need for eligibility verification
7. **Tool Selection**: Orchestrator selects PolicyRetriever, DrugSafetyTool, CoverageCalculator
8. **RAG Retrieval**: PolicyRetriever queries ChromaDB for relevant policy clauses
9. **Drug Check**: DrugSafetyTool queries drug database for prescribed medications
10. **Coverage Calc**: CoverageCalculator applies policy rules to treatment cost
11. **LLM Synthesis**: Orchestrator sends retrieved context + tool results to GPT-4
12. **LLM Response**: GPT-4 returns structured decision with reasoning
13. **Explanation Generation**: ExplanationEngine extracts decision, maps citations, scores confidence
14. **Persistence**: Decision record saved to PostgreSQL
15. **Response**: DecisionResponse returned to ClaimService → ClaimController → Frontend
16. **UI Update**: DecisionResult component displays decision with reasoning trace, citations, confidence score

Total latency: 2-5 seconds depending on LLM response time and retrieval complexity.

---

## Security Considerations

- **Authentication**: JWT tokens with RS256 signing, short expiration (15 minutes)
- **Authorization**: Role-based access control enforced at API and service layers
- **Data Privacy**: Patient data encrypted at rest and in transit (TLS 1.3)
- **Audit Trail**: All decisions logged with user ID, timestamp, input data
- **Rate Limiting**: API endpoints protected against abuse
- **Input Validation**: All user inputs sanitized to prevent injection attacks
- **Sensitive Data Masking**: PII masked in logs and error messages

---

## Scalability Strategy

- **Horizontal Scaling**: Spring Boot backend can run multiple instances behind load balancer
- **Database Connection Pooling**: HikariCP for efficient PostgreSQL connections
- **Caching**: Redis cache for frequent policy rule lookups
- **Async Processing**: Long-running decisions handled asynchronously with job queue
- **CDN**: Static frontend assets served via CDN
- **Vector DB Scaling**: ChromaDB can be deployed in distributed mode

---

## Monitoring and Observability

- **Metrics**: Prometheus for application metrics (request rate, latency, error rate)
- **Logging**: Structured JSON logs with correlation IDs for request tracing
- **Distributed Tracing**: OpenTelemetry for end-to-end request tracking
- **Health Checks**: Spring Actuator endpoints for liveness and readiness probes
- **Alerting**: Alerts for high error rates, slow LLM responses, database connection failures

---

This architecture provides a solid foundation for MediSureAI to deliver intelligent, explainable, and trustworthy healthcare decision support while maintaining scalability, security, and maintainability.
