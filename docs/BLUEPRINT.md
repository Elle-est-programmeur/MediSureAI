# 🏥 MediSureAI — Complete Software Engineering Blueprint

> AI-Powered Healthcare & Insurance Decision Support Platform  
> Agentic RAG · Explainable AI · Clinical Decision Support · Insurance Claim Validation

---

## 📁 1. Monorepo Folder Structure

```
medisure-ai/
├── frontend/                        # React + JavaScript (JSX) + Vite
│   ├── public/
│   ├── src/
│   │   ├── api/                     # Axios API service layer
│   │   │   ├── claimApi.js
│   │   │   ├── treatmentApi.js
│   │   │   ├── policyApi.js
│   │   │   ├── decisionApi.js
│   │   │   ├── authApi.js
│   │   │   └── adminApi.js
│   │   ├── components/
│   │   │   ├── common/              # Shared UI components
│   │   │   │   ├── Navbar.jsx
│   │   │   │   ├── Sidebar.jsx
│   │   │   │   ├── LoadingSpinner.jsx
│   │   │   │   └── ExplanationCard.jsx
│   │   │   ├── claim/
│   │   │   │   ├── ClaimForm.jsx
│   │   │   │   ├── ClaimStatus.jsx
│   │   │   │   └── ClaimHistory.jsx
│   │   │   ├── treatment/
│   │   │   │   ├── TreatmentForm.jsx
│   │   │   │   └── GuidelineViewer.jsx
│   │   │   ├── decision/
│   │   │   │   ├── DecisionResult.jsx
│   │   │   │   ├── ConfidenceScore.jsx
│   │   │   │   └── ReasoningTrace.jsx
│   │   │   └── policy/
│   │   │       ├── PolicyUploader.jsx
│   │   │       └── CoverageBreakdown.jsx
│   │   ├── pages/
│   │   │   ├── auth/
│   │   │   │   ├── Login.jsx
│   │   │   │   └── Register.jsx
│   │   │   ├── doctor/
│   │   │   │   ├── DoctorDashboard.jsx
│   │   │   │   ├── TreatmentValidation.jsx
│   │   │   │   └── DrugSafetyChecker.jsx
│   │   │   ├── patient/
│   │   │   │   ├── PatientDashboard.jsx
│   │   │   │   ├── PolicyAnalyzer.jsx
│   │   │   │   ├── ClaimSubmission.jsx
│   │   │   │   └── BillExplainer.jsx
│   │   │   └── admin/
│   │   │       ├── AdminDashboard.jsx
│   │   │       ├── ClaimReview.jsx
│   │   │       └── AuditTrail.jsx
│   │   ├── hooks/
│   │   │   ├── useAuth.js
│   │   │   ├── useClaim.js
│   │   │   ├── useDecision.js
│   │   │   └── usePolicy.js
│   │   ├── store/                   # Zustand state management
│   │   │   ├── authStore.js
│   │   │   ├── claimStore.js
│   │   │   └── decisionStore.js
│   │   ├── utils/
│   │   │   ├── formatters.js
│   │   │   ├── validators.js
│   │   │   └── constants.js
│   │   ├── config/
│   │   │   └── axiosConfig.js
│   │   ├── App.jsx
│   │   └── main.jsx
│   ├── index.html
│   ├── tailwind.config.js
│   ├── vite.config.js
│   └── package.json
│
├── backend/                         # Java Spring Boot
│   ├── src/main/java/com/medisure/
│   │   ├── MediSureApplication.java
│   │   ├── config/
│   │   │   ├── SecurityConfig.java
│   │   │   ├── JwtConfig.java
│   │   │   ├── CorsConfig.java
│   │   │   └── AIConfig.java
│   │   ├── modules/
│   │   │   ├── auth/
│   │   │   │   ├── controller/AuthController.java
│   │   │   │   ├── service/AuthService.java
│   │   │   │   ├── repository/UserRepository.java
│   │   │   │   ├── model/User.java
│   │   │   │   └── dto/
│   │   │   │       ├── LoginRequest.java
│   │   │   │       ├── RegisterRequest.java
│   │   │   │       └── AuthResponse.java
│   │   │   ├── claim/
│   │   │   │   ├── controller/ClaimController.java
│   │   │   │   ├── service/ClaimService.java
│   │   │   │   ├── repository/ClaimRepository.java
│   │   │   │   ├── model/Claim.java
│   │   │   │   └── dto/
│   │   │   │       ├── ClaimRequest.java
│   │   │   │       └── ClaimResponse.java
│   │   │   ├── treatment/
│   │   │   │   ├── controller/TreatmentController.java
│   │   │   │   ├── service/TreatmentService.java
│   │   │   │   ├── repository/TreatmentRepository.java
│   │   │   │   ├── model/Treatment.java
│   │   │   │   └── dto/
│   │   │   │       ├── TreatmentRequest.java
│   │   │   │       └── TreatmentResponse.java
│   │   │   ├── policy/
│   │   │   │   ├── controller/PolicyController.java
│   │   │   │   ├── service/PolicyService.java
│   │   │   │   ├── repository/PolicyRepository.java
│   │   │   │   ├── model/Policy.java
│   │   │   │   └── dto/
│   │   │   │       ├── PolicyRequest.java
│   │   │   │       └── PolicyResponse.java
│   │   │   └── decision/
│   │   │       ├── controller/DecisionController.java
│   │   │       ├── service/DecisionService.java
│   │   │       ├── repository/DecisionRepository.java
│   │   │       ├── model/Decision.java
│   │   │       └── dto/
│   │   │           ├── DecisionRequest.java
│   │   │           └── DecisionResponse.java
│   │   ├── agent/
│   │   │   ├── orchestrator/
│   │   │   │   ├── AgentOrchestrator.java
│   │   │   │   └── AgentContext.java
│   │   │   ├── tools/
│   │   │   │   ├── ClaimValidationTool.java
│   │   │   │   ├── DrugInteractionTool.java
│   │   │   │   ├── CoverageCalculatorTool.java
│   │   │   │   └── GuidelineValidatorTool.java
│   │   │   └── memory/
│   │   │       └── AgentMemoryStore.java
│   │   ├── rag/
│   │   │   ├── ingestion/
│   │   │   │   ├── DocumentIngestionService.java
│   │   │   │   └── EmbeddingService.java
│   │   │   ├── retrieval/
│   │   │   │   ├── VectorRetriever.java
│   │   │   │   └── HybridRetriever.java
│   │   │   └── pipeline/
│   │   │       └── RAGPipeline.java
│   │   ├── explainability/
│   │   │   ├── ExplanationEngine.java
│   │   │   ├── ReasoningTracer.java
│   │   │   └── ConfidenceScorer.java
│   │   └── common/
│   │       ├── exception/GlobalExceptionHandler.java
│   │       ├── response/ApiResponse.java
│   │       └── util/DateUtils.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-dev.yml
│   │   └── application-prod.yml
│   └── pom.xml
│
├── ai-engine/                       # Python (optional AI microservice)
│   ├── ingestion/
│   │   ├── pdf_loader.py
│   │   └── text_chunker.py
│   ├── embeddings/
│   │   └── embed_documents.py
│   ├── retrieval/
│   │   └── vector_search.py
│   └── requirements.txt
│
├── database/
│   ├── migrations/
│   │   ├── V1__create_users.sql
│   │   ├── V2__create_policies.sql
│   │   ├── V3__create_claims.sql
│   │   ├── V4__create_treatments.sql
│   │   └── V5__create_decisions.sql
│   └── seed/
│       ├── seed_policies.sql
│       └── seed_guidelines.sql
│
├── prompts/
│   ├── agent_system_prompt.txt
│   ├── claim_validation_prompt.txt
│   ├── treatment_guideline_prompt.txt
│   ├── drug_interaction_prompt.txt
│   ├── bill_explanation_prompt.txt
│   └── explanation_generation_prompt.txt
│
├── docs/
│   ├── architecture/
│   │   ├── system-architecture.md
│   │   ├── agent-flow.md
│   │   └── rag-pipeline.md
│   ├── api/
│   │   └── api-reference.md
│   └── diagrams/
│       └── er-diagram.md
│
├── infra/
│   ├── docker-compose.yml
│   ├── Dockerfile.backend
│   ├── Dockerfile.frontend
│   └── nginx.conf
│
├── tests/
│   ├── backend/
│   │   ├── unit/
│   │   └── integration/
│   └── frontend/
│       └── component/
│
└── README.md
```

---

## 🏗️ 2. High-Level System Architecture

```
┌──────────────────────────────────────────────────────────┐
│                        USERS                             │
│          Patient | Doctor | Insurance Admin              │
└──────────────────────┬───────────────────────────────────┘
                       ↓
┌──────────────────────────────────────────────────────────┐
│              React Frontend (Vite + Tailwind)            │
│  Patient Portal | Doctor Dashboard | Admin Panel         │
└──────────────────────┬───────────────────────────────────┘
                       ↓ REST / HTTPS
┌──────────────────────────────────────────────────────────┐
│              Spring Boot API Gateway                     │
│     Auth | Claim | Treatment | Policy | Decision         │
└──────────────────────┬───────────────────────────────────┘
                       ↓
┌──────────────────────────────────────────────────────────┐
│              Agent Orchestrator (Spring AI)              │
│   Intent Detection → Tool Selection → Reasoning Loop     │
└────┬──────────────┬──────────────┬────────────┬──────────┘
     ↓              ↓              ↓            ↓
┌─────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐
│ RAG     │  │  Claim   │  │  Drug    │  │ Guideline    │
│Pipeline │  │Validator │  │ Safety   │  │ Validator    │
└────┬────┘  └────┬─────┘  └────┬─────┘  └──────┬───────┘
     ↓            ↓             ↓               ↓
┌─────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐
│ Vector  │  │ Postgres │  │ Drug DB  │  │ Rules Engine │
│   DB    │  │   (SQL)  │  │          │  │              │
└────┬────┘  └────┬─────┘  └────┬─────┘  └──────┬───────┘
     └────────────┴─────────────┴────────────────┘
                       ↓
              Aggregated Knowledge
                       ↓
┌──────────────────────────────────────────────────────────┐
│                  LLM (OpenAI / Ollama)                   │
│          Reason → Justify → Score Confidence             │
└──────────────────────┬───────────────────────────────────┘
                       ↓
┌──────────────────────────────────────────────────────────┐
│             Explanation Engine                           │
│   Decision + Reasoning Trace + Confidence + Citations    │
└──────────────────────────────────────────────────────────┘
```

### Agent Reasoning Loop
```
User Request
    ↓
Intent Detection
    ↓
Tool Selection (which tools are needed?)
    ↓
Tool Execution (parallel where possible)
    ↓
Result Observation
    ↓
Re-query if insufficient? → YES → loop back
                           → NO  → continue
    ↓
LLM Synthesis
    ↓
Explainability Layer
    ↓
Structured Response
```

---

## 🧩 3. Backend Module Design (Java)

### Auth Module
Handles JWT-based authentication and role management (PATIENT, DOCTOR, ADMIN).

```java
// AuthController.java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    @PostMapping("/register") // Register user
    @PostMapping("/login")    // Return JWT token
    @PostMapping("/refresh")  // Refresh token
}
```

### Claim Module
Accepts treatment + patient + policy data. Triggers eligibility evaluation.

```java
@RestController
@RequestMapping("/api/v1/claims")
public class ClaimController {
    @PostMapping("/submit")         // Submit new claim
    @GetMapping("/{claimId}")       // Get claim by ID
    @GetMapping("/patient/{id}")    // Claims by patient
    @PutMapping("/{claimId}/status")// Update status
}
```

### Agent Orchestrator
The brain. Receives a DecisionRequest, selects tools, runs reasoning loop.

```java
@Service
public class AgentOrchestrator {
    
    // Core reasoning method
    public DecisionResponse orchestrate(DecisionRequest request) {
        AgentContext ctx = buildContext(request);
        
        // Step 1: Detect intent
        String intent = detectIntent(ctx);
        
        // Step 2: Select tools
        List<AgentTool> tools = selectTools(intent, ctx);
        
        // Step 3: Execute tools
        List<ToolResult> results = executeTools(tools, ctx);
        
        // Step 4: Synthesize with LLM
        String reasoning = llmSynthesize(results, ctx);
        
        // Step 5: Generate explanation
        return explanationEngine.build(reasoning, results);
    }
}
```

### RAG Pipeline

```java
// RAGPipeline.java
@Service
public class RAGPipeline {

    // Ingest document into vector DB
    public void ingest(byte[] pdfBytes, String category) {
        List<String> chunks = chunker.chunk(pdfBytes);
        List<float[]> embeddings = embeddingService.embed(chunks);
        vectorStore.upsert(chunks, embeddings, category);
    }

    // Retrieve relevant context
    public List<String> retrieve(String query, String category, int topK) {
        float[] queryEmbedding = embeddingService.embed(query);
        return vectorStore.similaritySearch(queryEmbedding, category, topK);
    }
}
```

### Explanation Engine

```java
@Service
public class ExplanationEngine {

    public DecisionResponse build(String llmReasoning, List<ToolResult> results) {
        return DecisionResponse.builder()
            .decision(extractDecision(llmReasoning))
            .reasoning(llmReasoning)
            .confidenceScore(scorer.score(results))
            .citations(extractCitations(results))
            .warnings(extractWarnings(results))
            .suggestedActions(extractActions(llmReasoning))
            .build();
    }
}
```

---

## 🌐 4. Frontend Module Design (React)

### Role-Based Routing

```jsx
// App.jsx
<Routes>
  <Route path="/login" element={<Login />} />
  <Route path="/patient/*" element={<ProtectedRoute role="PATIENT"><PatientRoutes /></ProtectedRoute>} />
  <Route path="/doctor/*" element={<ProtectedRoute role="DOCTOR"><DoctorRoutes /></ProtectedRoute>} />
  <Route path="/admin/*" element={<ProtectedRoute role="ADMIN"><AdminRoutes /></ProtectedRoute>} />
</Routes>
```

### Patient Dashboard Features
- Upload insurance policy PDF
- Submit treatment details
- View claim status
- Read AI-generated bill explanation
- View coverage breakdown with citations

### Doctor Dashboard Features
- Submit treatment plan for validation
- View drug interaction warnings
- Check guideline compliance
- Generate discharge instructions

### Decision Explanation UI

```jsx
// DecisionResult.jsx
const DecisionResult = ({ decision }) => (
  <div className="space-y-4">
    <StatusBadge status={decision.status} />         {/* Approved / Rejected / Partial */}
    <ConfidenceScore score={decision.confidence} />  {/* Visual percentage */}
    <ReasoningTrace steps={decision.reasoningSteps} /> {/* Agent thought process */}
    <CitationsList citations={decision.citations} />   {/* Policy clause references */}
    <WarningsList warnings={decision.warnings} />      {/* Drug/coverage warnings */}
  </div>
);
```

### State Management (Zustand)

```js
// claimStore.js
// ClaimStore structure (using JSDoc for types):
/**
 * @typedef {Object} ClaimStore
 * @property {Claim[]} claims
 * @property {Claim|null} activeClaim
 * @property {boolean} isLoading
 * @property {Function} submitClaim
 * @property {Function} fetchClaims
 */
const useClaimStore = create((set) => ({
  claims: Claim[];
  activeClaim: Claim | null;
  isLoading: boolean;
  submitClaim: (data: ClaimRequest) => Promise<void>;
  fetchClaims: () => Promise<void>;
}
```

---

## 🗄️ 5. Database Design

### Tables

```sql
-- Users
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,  -- PATIENT | DOCTOR | ADMIN
    created_at TIMESTAMP DEFAULT NOW()
);

-- Policies
CREATE TABLE policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    policy_number VARCHAR(100) UNIQUE,
    provider_name VARCHAR(255),
    coverage_amount DECIMAL(12,2),
    premium DECIMAL(10,2),
    start_date DATE,
    end_date DATE,
    status VARCHAR(50),           -- ACTIVE | EXPIRED | SUSPENDED
    raw_document_path TEXT,       -- Path to uploaded PDF
    created_at TIMESTAMP DEFAULT NOW()
);

-- Treatments
CREATE TABLE treatments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID REFERENCES users(id),
    doctor_id UUID REFERENCES users(id),
    diagnosis TEXT NOT NULL,
    treatment_plan TEXT,
    medications JSONB,            -- [{name, dosage, frequency}]
    hospital_name VARCHAR(255),
    estimated_cost DECIMAL(12,2),
    treatment_date DATE,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Claims
CREATE TABLE claims (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID REFERENCES users(id),
    policy_id UUID REFERENCES policies(id),
    treatment_id UUID REFERENCES treatments(id),
    claim_amount DECIMAL(12,2),
    status VARCHAR(50),           -- PENDING | APPROVED | REJECTED | PARTIAL
    submitted_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP
);

-- Decisions
CREATE TABLE decisions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id UUID REFERENCES claims(id),
    decision_type VARCHAR(50),    -- CLAIM_ELIGIBILITY | TREATMENT_VALIDATION | DRUG_SAFETY
    outcome VARCHAR(50),          -- APPROVED | REJECTED | WARNING | PARTIAL
    payable_amount DECIMAL(12,2),
    reasoning TEXT,
    confidence_score DECIMAL(5,2),
    citations JSONB,              -- [{source, clause, text}]
    warnings JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Audit Logs
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(100),
    entity_id UUID,
    action VARCHAR(100),
    performed_by UUID REFERENCES users(id),
    details JSONB,
    timestamp TIMESTAMP DEFAULT NOW()
);
```

### Relationships
- `User (1) → (N) Policy`
- `User (1) → (N) Treatment` (as patient)
- `User (1) → (N) Treatment` (as doctor)
- `Policy (1) → (N) Claim`
- `Treatment (1) → (1) Claim`
- `Claim (1) → (N) Decision`

---

## 🔗 6. API Design

### Auth
```
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
```

### Claims
```
POST   /api/v1/claims/submit                 # Submit new claim
GET    /api/v1/claims/{claimId}              # Get claim details
GET    /api/v1/claims/patient/{patientId}    # Patient's claims
PUT    /api/v1/claims/{claimId}/status       # Update claim status
```

### Treatments
```
POST   /api/v1/treatments                     # Create treatment record (REST convention)
POST   /api/v1/treatments/validate           # Validate treatment plan
POST   /api/v1/treatments/drug-check         # Drug interaction check
GET    /api/v1/treatments/{treatmentId}
```

### Policies
```
POST   /api/v1/policies/upload               # Upload policy PDF
POST   /api/v1/policies/analyze              # Analyze coverage
GET    /api/v1/policies/{policyId}/coverage  # Coverage breakdown
```

### Decisions
```
POST   /api/v1/decisions/generate                   # Trigger agent decision
GET    /api/v1/decisions/{decisionId}               # Full decision + explanation
GET    /api/v1/decisions/claim/{claimId}            # Decision for a claim
POST   /api/v1/decisions/{decisionId}/feedback      # Submit feedback on decision
```

### Billing
```
POST   /api/v1/billing/analyze                      # Analyze and explain hospital bill
```

### Sample Request/Response

**POST /api/v1/decisions/generate**
```json
// Request
{
  "claimId": "uuid",
  "decisionType": "CLAIM_ELIGIBILITY",
  "context": {
    "patientAge": 45,
    "diagnosis": "Type 2 Diabetes",
    "treatmentPlan": "Insulin therapy + dietary management",
    "hospitalType": "NETWORK",
    "estimatedCost": 85000
  }
}

// Response
{
  "decisionId": "uuid",
  "outcome": "PARTIAL",
  "payableAmount": 62500,
  "confidenceScore": 87.4,
  "reasoning": "The policy covers diabetes treatment up to ₹75,000 annually. A co-pay of 25% applies for insulin therapy per Clause 4.2.1. Network hospital discount applied.",
  "citations": [
    { "clause": "4.2.1", "text": "Insulin therapy co-pay: 25%", "source": "policy_doc.pdf" }
  ],
  "warnings": ["Annual limit 73% utilized after this claim"],
  "suggestedActions": ["Consider day-care admission to reduce room rent costs"]
}
```

---

## 🤖 7. Agentic RAG Workflow

### Full Agent Flow

```
1. RECEIVE REQUEST
   └─ Claim ID + Decision Type + Context

2. INTENT ANALYSIS
   └─ What kind of decision is needed?
      CLAIM_ELIGIBILITY | DRUG_SAFETY | TREATMENT_VALIDATION | BILL_EXPLANATION

3. TOOL SELECTION (Agent decides)
   ├─ Always: PolicyRetriever (vector search on policy docs)
   ├─ If medications present: DrugInteractionTool
   ├─ If treatment plan present: GuidelineValidatorTool
   ├─ Always: CoverageCalculatorTool
   └─ If bill present: BillingAnalyzerTool

4. PARALLEL TOOL EXECUTION
   ├─ Vector DB Query → Retrieve policy clauses
   ├─ SQL Query → Fetch patient + policy metadata
   ├─ Drug DB → Check interactions
   └─ Rule Engine → Calculate coverage amounts

5. OBSERVATION + VALIDATION
   └─ Is the retrieved context sufficient?
      NO → Re-query with refined terms
      YES → proceed

6. LLM SYNTHESIS
   └─ Prompt: System prompt + Context + Retrieved docs + Tool results
   └─ LLM generates: Decision + Reasoning + Warnings

7. EXPLANATION GENERATION
   ├─ Extract decision outcome
   ├─ Map reasoning to source citations
   ├─ Score confidence based on retrieval quality
   └─ Generate patient-friendly summary

8. RETURN STRUCTURED RESPONSE
```

### Sample Prompt (prompts/claim_validation_prompt.txt)
```
You are a healthcare insurance decision specialist.

Given the following:
- Patient Context: {patient_context}
- Policy Clauses Retrieved: {policy_clauses}
- Treatment Details: {treatment_details}
- Coverage Rules: {coverage_rules}
- Drug Interaction Results: {drug_results}

Your task:
1. Determine if the claim is APPROVED, REJECTED, or PARTIAL
2. Calculate the payable amount with step-by-step reasoning
3. Cite the specific policy clauses that apply
4. List any warnings or risk factors
5. Suggest actions the patient can take

Respond in structured JSON format only.
Format: { "outcome", "payableAmount", "reasoning", "citations", "warnings", "suggestedActions" }
```

---

## ⚙️ 8. Development Setup Guide

### Prerequisites
```bash
Java 17+, Maven 3.9+, Node 18+, PostgreSQL 15+, Docker (optional)
```

### 1. Clone and Setup
```bash
git clone https://github.com/yourteam/medisure-ai.git
cd medisure-ai
```

### 2. Database Setup
```bash
# Create database
psql -U postgres
CREATE DATABASE medisure_db;

# Run migrations
cd database/migrations
psql -U postgres -d medisure_db -f V1__create_users.sql
psql -U postgres -d medisure_db -f V2__create_policies.sql
# ... run all migration files in order
```

### 3. Backend Setup
```bash
cd backend

# Configure application-dev.yml
# Set: DB url, password, OpenAI/Ollama key, vector DB URL

mvn clean install
mvn spring-boot:run -Dspring.profiles.active=dev
# Backend runs at http://localhost:8080
```

### 4. Vector DB Setup (ChromaDB)
```bash
pip install chromadb
chroma run --path ./chroma-data --port 8000
# OR use Docker:
docker run -p 8000:8000 chromadb/chroma
```

### 5. Document Ingestion
```bash
# Place policy PDFs in /data/policies/
# Place medical guidelines in /data/guidelines/

# Trigger ingestion via API:
curl -X POST http://localhost:8080/api/v1/rag/ingest \
  -F "file=@policy.pdf" \
  -F "category=INSURANCE_POLICY"
```

### 6. Frontend Setup
```bash
cd frontend
npm install
cp .env.example .env.local
# Set VITE_API_BASE_URL=http://localhost:8080
npm run dev
# Frontend runs at http://localhost:5173
```

### 7. Docker Compose (Full Stack)
```yaml
# infra/docker-compose.yml
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: medisure_db
      POSTGRES_USER: medisure
      POSTGRES_PASSWORD: medisure123
    ports: ["5432:5432"]

  chroma:
    image: chromadb/chroma
    ports: ["8000:8000"]

  backend:
    build:
      context: ../backend
      dockerfile: ../infra/Dockerfile.backend
    ports: ["8080:8080"]
    depends_on: [postgres, chroma]
    environment:
      SPRING_PROFILES_ACTIVE: dev
      DB_URL: jdbc:postgresql://postgres:5432/medisure_db

  frontend:
    build:
      context: ../frontend
      dockerfile: ../infra/Dockerfile.frontend
    ports: ["5173:80"]
    depends_on: [backend]
```

```bash
cd infra
docker-compose up --build
```

---

## 🧪 9. Testing Strategy

### Unit Tests (JUnit 5 + Mockito)
```java
// ClaimServiceTest.java
@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    @Mock ClaimRepository claimRepository;
    @Mock AgentOrchestrator orchestrator;
    @InjectMocks ClaimService claimService;

    @Test
    void shouldSubmitClaimAndTriggerDecision() {
        // Arrange
        ClaimRequest request = buildMockRequest();
        when(orchestrator.orchestrate(any())).thenReturn(buildMockDecision());
        
        // Act
        ClaimResponse response = claimService.submit(request);
        
        // Assert
        assertNotNull(response.getClaimId());
        assertEquals("PENDING", response.getStatus());
        verify(orchestrator, times(1)).orchestrate(any());
    }
}
```

### API Tests (Spring Boot Test)
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ClaimControllerIntegrationTest {

    @Test
    void submitClaim_shouldReturn201() throws Exception {
        mockMvc.perform(post("/api/v1/claims/submit")
            .contentType(MediaType.APPLICATION_JSON)
            .content(claimRequestJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.claimId").exists());
    }
}
```

### RAG Pipeline Tests
- Test retrieval accuracy: verify top-K results contain expected clause
- Test embedding quality: cosine similarity threshold > 0.75
- Test end-to-end decision: mock LLM, verify correct tool calls made

---

## 📄 10. README.md

```markdown
# 🏥 MediSureAI

> AI-Powered Healthcare & Insurance Decision Support Platform

MediSureAI is an explainable multi-agent AI system that bridges the gap between clinical
decision-making and insurance claim validation. It helps patients understand their coverage,
supports doctors in treatment validation, and assists hospitals in claim readiness.

---

## 🚨 Problem Statement

Healthcare systems today operate in silos. Insurance claims are rejected without clear
reasoning. AI models act as black boxes. Patients, doctors, and hospitals lack a unified
intelligent layer that connects treatment decisions with insurance eligibility and
provides transparent, explainable outcomes.

---

## ✨ Features

| Feature | Description |
|---|---|
| 🤖 Agentic AI | Multi-step reasoning agent using tool orchestration |
| 📚 RAG Pipeline | Policy and guideline retrieval from vector knowledge base |
| 🔍 Claim Eligibility | Automated multi-step insurance claim validation |
| 💊 Drug Safety | Real-time drug interaction checking |
| 🧾 Bill Explainer | Human-readable hospital bill breakdown |
| 🩺 Treatment Validator | Clinical guideline compliance check |
| 💡 Explainable AI | Full reasoning trace with policy citations |
| 🔒 Role-Based Access | Patient, Doctor, Admin dashboards |

---

## 🧰 Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React, JavaScript (JSX), Vite, Tailwind CSS |
| Backend | Java 17, Spring Boot 3, Spring AI |
| AI | OpenAI / Ollama (LLM), ChromaDB (Vector DB, port 8000) |
| Database | PostgreSQL |
| DevOps | Docker, Docker Compose |

---

## 📁 Project Structure

See full folder structure in `/docs/architecture/`

---

## 🚀 Quick Start

### 1. Clone Repository
```bash
git clone https://github.com/yourteam/medisure-ai.git
cd medisure-ai
```

### 2. Start with Docker
```bash
cd infra
docker-compose up --build
```

### 3. Access Application
- Frontend: http://localhost:5173
- Backend API: http://localhost:8080
- API Docs (Swagger): http://localhost:8080/swagger-ui.html

---

## 🏗️ Architecture

See `/docs/architecture/system-architecture.md`

Key components:
- Central Agent Orchestrator (Spring AI)
- RAG Pipeline (Chroma Vector DB + Embeddings)
- Rule-based Coverage Calculator
- Explainability Engine with citation generation

---

## 👥 Team Workflow

```
main branch         → stable, production-ready code
develop branch      → integration branch
feature/xxx         → individual feature branches
```

**Commit convention:**
```
feat: add claim submission API
fix: correct coverage calculation bug
docs: update API reference
```

---

## 🔮 Future Scope

- Integration with real insurance provider APIs
- FHIR-compliant patient data exchange
- Fraud detection module using anomaly detection
- Mobile app (React Native)
- Multi-language support for regional hospitals
- Federated learning for privacy-preserving model training

---

## 🎓 Academic Note

This project was developed as a Final Year Major Project for the Bachelor of Engineering
in Computer Engineering program. The system demonstrates practical application of
Agentic AI, Retrieval-Augmented Generation, and Explainable AI in a real-world
healthcare domain.

---

## 📄 License

MIT License — free to use for academic and non-commercial purposes.
```

---

## 🎯 Implementation Priority Order

Build in this sequence to always have a working, demonstrable system:

| Phase | Focus | Deliverable |
|---|---|---|
| 1 | Auth + Basic Claim CRUD | Login + Submit a claim |
| 2 | Policy Upload + RAG Ingestion | Upload PDF + query it |
| 3 | Basic Agent + Decision Engine | Simple eligibility output |
| 4 | Coverage Calculation Tool | Payable amount logic |
| 5 | Drug Safety Module | Interaction warnings |
| 6 | Explainability Layer | Reasoning trace + citations |
| 7 | Full Frontend Dashboards | Polished demo-ready UI |

At every phase the system is demoable. This is how you protect yourself against time pressure.
```