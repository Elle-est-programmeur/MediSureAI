# Backend Modules

This document provides a comprehensive overview of all backend modules in the MediSureAI Spring Boot application. Each module is designed with clear separation of concerns following a layered architecture: Controller (presentation), Service (business logic), Repository (data access), Model (domain entities), and DTO (data transfer objects).

---

## Module Architecture Pattern

Each module in MediSureAI follows a consistent structure:

```
module/
├── controller/          # REST API endpoints
│   └── XxxController.java
├── service/             # Business logic and orchestration
│   ├── XxxService.java
│   └── impl/
│       └── XxxServiceImpl.java
├── repository/          # Data access layer (Spring Data JPA)
│   └── XxxRepository.java
├── model/              # Domain entities (JPA entities)
│   └── Xxx.java
└── dto/                # Data transfer objects
    ├── XxxRequest.java
    ├── XxxResponse.java
    └── XxxDTO.java
```

**Benefits of this pattern**:
- Clear separation of concerns
- Easy to test (mock dependencies at each layer)
- Scalable (can move services to separate microservices if needed)
- Maintainable (changes isolated to specific layers)

---

## 1. Auth Module

### Purpose
Handle user authentication, authorization, and role-based access control for the MediSureAI platform.

### Responsibilities
- User registration with role assignment (PATIENT, DOCTOR, ADMIN)
- User login with JWT token generation
- Token validation and refresh
- Password encryption and security
- Role-based access control enforcement
- User profile management

### Key Components

#### AuthController
**Endpoints**:
- POST /api/v1/auth/register - Register new user
- POST /api/v1/auth/login - Authenticate user and return JWT
- POST /api/v1/auth/refresh - Refresh expired JWT token
- POST /api/v1/auth/logout - Invalidate token (optional, for token blacklisting)
- GET /api/v1/auth/me - Get current user profile

**Request/Response Flow**:
```
Client sends credentials → AuthController.login()
    → AuthService.authenticate()
    → UserRepository.findByEmail()
    → Verify password (BCrypt)
    → Generate JWT token (JwtUtils)
    → Return AuthResponse with token + user details
```

#### AuthService
**Core Methods**:
- registerUser(RegisterRequest request): Creates new user account, assigns role, encrypts password
- authenticateUser(LoginRequest request): Validates credentials, generates JWT
- refreshToken(String refreshToken): Issues new access token from valid refresh token
- validateToken(String token): Checks token signature and expiration
- getCurrentUser(): Retrieves authenticated user from security context

**Business Rules**:
- Email must be unique across all users
- Password minimum 8 characters, must include uppercase, lowercase, number
- PATIENT role assigned by default for self-registration
- DOCTOR and ADMIN roles require admin approval
- Passwords encrypted using BCrypt (strength 12)
- JWT access tokens expire after 15 minutes
- JWT refresh tokens expire after 7 days

#### UserRepository
**Interface**: Extends JpaRepository<User, UUID>

**Custom Queries**:
- findByEmail(String email): Look up user by email for login
- findByRole(UserRole role): Get all users with specific role
- existsByEmail(String email): Check if email already registered

#### User Model
**Entity**: JPA entity mapped to 'users' table

**Fields**:
- id (UUID, primary key, auto-generated)
- name (String, user full name)
- email (String, unique, indexed)
- passwordHash (String, BCrypt hash, never exposed in responses)
- role (Enum: PATIENT, DOCTOR, ADMIN)
- phoneNumber (String, optional)
- dateOfBirth (LocalDate, optional for PATIENT)
- specialization (String, required for DOCTOR)
- licenseNumber (String, required for DOCTOR)
- hospitalAffiliation (String, optional for DOCTOR)
- isActive (Boolean, account status)
- createdAt (Timestamp, auto-generated)
- updatedAt (Timestamp, auto-updated)

**Relationships**:
- One User (PATIENT) → Many Policies
- One User (PATIENT) → Many Claims
- One User (DOCTOR) → Many Treatments

#### DTOs
**RegisterRequest**: name, email, password, role, phoneNumber, specialization (for doctors)
**LoginRequest**: email, password
**AuthResponse**: token, refreshToken, user (UserDTO), expiresIn
**UserDTO**: id, name, email, role (excludes password)

### Interactions with Other Modules
- **Claim Module**: Validates user identity before claim submission
- **Treatment Module**: Verifies doctor credentials before treatment creation
- **Policy Module**: Links policy uploads to patient accounts
- **Decision Module**: Associates decisions with requesting user
- **Global Security**: Provides authentication filter for all protected endpoints

### Security Considerations
- Passwords never stored in plain text
- JWT tokens signed with RS256 (public/private key pair)
- Private key stored securely (environment variable or secret manager)
- Token expiration enforced
- Failed login attempts logged for audit
- Rate limiting on login endpoint to prevent brute force

---

## 2. Claim Module

### Purpose
Manage insurance claim submissions, status tracking, and claim lifecycle from submission to decision.

### Responsibilities
- Accept claim submissions from patients
- Validate claim data completeness and integrity
- Link claims to policies, treatments, and patients
- Track claim status (PENDING, APPROVED, REJECTED, PARTIAL)
- Update claim status based on agent decisions
- Provide claim history and search

### Key Components

#### ClaimController
**Endpoints**:
- POST /api/v1/claims/submit - Submit new claim
- GET /api/v1/claims/{claimId} - Get claim details by ID
- GET /api/v1/claims/patient/{patientId} - List all claims for a patient
- GET /api/v1/claims/policy/{policyId} - List claims for a specific policy
- PUT /api/v1/claims/{claimId}/status - Update claim status (admin only)
- GET /api/v1/claims/search - Search claims with filters

**Authorization**:
- PATIENT can submit claims and view their own claims
- DOCTOR can view claims related to their treatments
- ADMIN can view and update all claims

#### ClaimService
**Core Methods**:
- submitClaim(ClaimRequest request, String userId): Creates new claim, triggers agent decision
- getClaimById(UUID claimId, String userId): Retrieves claim with authorization check
- getClaimsByPatient(UUID patientId): Lists all claims for a patient
- updateClaimStatus(UUID claimId, ClaimStatus newStatus, String reason): Updates status with audit trail
- searchClaims(ClaimSearchCriteria criteria): Flexible search with filters

**Business Logic Flow**:
```
1. Receive claim submission
2. Validate claim data (all required fields present)
3. Verify patient owns the policy
4. Verify treatment exists and belongs to patient
5. Calculate claim amount from treatment cost
6. Create Claim entity with PENDING status
7. Persist to database
8. Trigger AgentOrchestrator to generate decision
9. Agent processes claim asynchronously
10. Decision stored and claim status updated
11. Notification sent to patient
12. Return ClaimResponse to client
```

**Business Rules**:
- Claim amount cannot exceed treatment estimated cost
- Policy must be active at time of treatment
- One treatment can have only one claim
- Claim cannot be resubmitted once APPROVED or REJECTED
- Only ADMIN can manually update claim status

#### ClaimRepository
**Interface**: Extends JpaRepository<Claim, UUID>

**Custom Queries**:
- findByPatientId(UUID patientId): All claims for a patient
- findByPolicyId(UUID policyId): All claims under a policy
- findByStatus(ClaimStatus status): Claims filtered by status
- findByPatientIdAndStatus(UUID patientId, ClaimStatus status): Combined filter
- findBySubmittedAtBetween(Timestamp start, Timestamp end): Claims in date range

#### Claim Model
**Entity**: JPA entity mapped to 'claims' table

**Fields**:
- id (UUID, primary key)
- claimNumber (String, unique, generated, format: CLM-YYYYMMDD-XXXX)
- patientId (UUID, foreign key to users)
- policyId (UUID, foreign key to policies)
- treatmentId (UUID, foreign key to treatments)
- claimAmount (BigDecimal, requested amount)
- approvedAmount (BigDecimal, amount approved by insurance, null if rejected)
- status (Enum: PENDING, APPROVED, REJECTED, PARTIAL, UNDER_REVIEW)
- rejectionReason (String, populated if REJECTED)
- submittedAt (Timestamp)
- processedAt (Timestamp, when decision was made)
- updatedAt (Timestamp)

**Relationships**:
- Many Claims → One Policy
- Many Claims → One Patient
- One Claim → One Treatment
- One Claim → Many Decisions (may have multiple reviews)

#### DTOs
**ClaimRequest**: policyId, treatmentId, claimAmount, supportingDocuments (optional)
**ClaimResponse**: claimId, claimNumber, status, claimAmount, approvedAmount, submittedAt, message
**ClaimDetailDTO**: Complete claim info + related policy summary + treatment summary + latest decision
**ClaimSearchCriteria**: patientId, policyId, status, dateFrom, dateTo, minAmount, maxAmount

### Interactions with Other Modules
- **Auth Module**: Validates user is patient and owns the claim
- **Policy Module**: Fetches policy details to validate coverage
- **Treatment Module**: Fetches treatment details for claim validation
- **Decision Module**: Triggers agent orchestrator for decision generation
- **Agent Orchestrator**: Receives decision updates and updates claim status

### Validation Rules
- Claim amount > 0 and ≤ treatment estimated cost
- Policy must exist and belong to patient
- Policy must be ACTIVE status
- Treatment must exist and belong to patient
- Treatment must not already have an approved claim
- Supporting documents (bills, prescriptions) recommended but not mandatory

---

## 3. Treatment Module

### Purpose
Manage medical treatment records, validate treatment plans against clinical guidelines, and support clinical decision-making.

### Responsibilities
- Record treatment details (diagnosis, procedures, medications)
- Validate treatment plans against medical guidelines
- Check drug interactions and safety
- Link treatments to patients and doctors
- Provide treatment history
- Generate treatment summaries for claim submission

### Key Components

#### TreatmentController
**Endpoints**:
- POST /api/v1/treatments/create - Create treatment record (doctor only)
- GET /api/v1/treatments/{treatmentId} - Get treatment details
- GET /api/v1/treatments/patient/{patientId} - Patient's treatment history
- POST /api/v1/treatments/validate - Validate treatment plan against guidelines
- POST /api/v1/treatments/drug-check - Check drug interactions
- PUT /api/v1/treatments/{treatmentId} - Update treatment details (doctor only)

**Authorization**:
- DOCTOR can create and update treatments
- PATIENT can view their own treatments
- ADMIN can view all treatments

#### TreatmentService
**Core Methods**:
- createTreatment(TreatmentRequest request, String doctorId): Records new treatment
- validateTreatmentPlan(TreatmentValidationRequest request): Validates against guidelines using agent
- checkDrugInteractions(List<String> medications, PatientContext context): Checks for drug interactions
- getTreatmentById(UUID treatmentId): Retrieves treatment with authorization
- getTreatmentsByPatient(UUID patientId): Lists patient's treatment history
- updateTreatment(UUID treatmentId, TreatmentRequest request): Updates existing treatment

**Business Logic Flow for Treatment Validation**:
```
1. Receive treatment plan (diagnosis, medications, procedures)
2. Call GuidelineValidatorTool via agent orchestrator
3. Retrieve relevant clinical guidelines from RAG
4. Compare treatment plan against guideline recommendations
5. Call DrugSafetyTool for medication validation
6. Check each drug for interactions, contraindications
7. Aggregate results from both tools
8. Generate validation report with:
   - Compliance status (COMPLIANT, NON_COMPLIANT, PARTIAL)
   - Deviations from guidelines
   - Drug safety warnings
   - Recommendations for improvement
9. Return ValidationReport
```

**Business Rules**:
- Only licensed doctors can create treatment records
- Treatment diagnosis must use ICD-10 codes
- Medications must include dosage and frequency
- Estimated cost must be provided for insurance claims
- Treatment date cannot be in the future
- Doctor must document reason if deviating from guidelines

#### TreatmentRepository
**Interface**: Extends JpaRepository<Treatment, UUID>

**Custom Queries**:
- findByPatientId(UUID patientId): All treatments for a patient
- findByDoctorId(UUID doctorId): Treatments recorded by a doctor
- findByPatientIdAndDiagnosis(UUID patientId, String diagnosis): Treatments for specific condition
- findByTreatmentDateBetween(LocalDate start, LocalDate end): Treatments in date range

#### Treatment Model
**Entity**: JPA entity mapped to 'treatments' table

**Fields**:
- id (UUID, primary key)
- treatmentNumber (String, unique, format: TRT-YYYYMMDD-XXXX)
- patientId (UUID, foreign key to users)
- doctorId (UUID, foreign key to users)
- diagnosis (String, primary diagnosis with ICD-10 code)
- secondaryDiagnoses (JSON, list of additional diagnoses)
- treatmentPlan (Text, description of treatment approach)
- procedures (JSON, list of procedures with CPT codes)
- medications (JSON, structured list: [{name, dosage, frequency, duration}])
- hospitalName (String)
- hospitalType (Enum: NETWORK, NON_NETWORK, DAY_CARE)
- admissionDate (LocalDate)
- dischargeDate (LocalDate, nullable if ongoing)
- estimatedCost (BigDecimal)
- actualCost (BigDecimal, populated after treatment completion)
- treatmentStatus (Enum: PLANNED, IN_PROGRESS, COMPLETED, CANCELLED)
- guidelineCompliance (Enum: NOT_CHECKED, COMPLIANT, NON_COMPLIANT, PARTIAL)
- notes (Text, doctor's notes)
- createdAt (Timestamp)
- updatedAt (Timestamp)

**Relationships**:
- Many Treatments → One Patient
- Many Treatments → One Doctor
- One Treatment → One Claim

#### DTOs
**TreatmentRequest**: patientId, diagnosis, treatmentPlan, medications, procedures, hospitalName, hospitalType, estimatedCost, treatmentDate
**TreatmentResponse**: treatmentId, treatmentNumber, status, estimatedCost, createdAt
**TreatmentValidationRequest**: diagnosis, medications, procedures, patientAge, patientConditions
**ValidationReport**: compliant (boolean), deviations (list), drugWarnings (list), recommendations (list), guidelineReferences (list)
**DrugCheckRequest**: medications, patientConditions, patientAge, patientAllergies
**DrugCheckResponse**: hasInteractions (boolean), interactions (list), contraindications (list), warnings (list)

### Interactions with Other Modules
- **Auth Module**: Validates doctor credentials
- **Claim Module**: Treatment data used for claim submission
- **Agent Orchestrator**: Calls GuidelineValidatorTool and DrugSafetyTool
- **RAG Pipeline**: Retrieves clinical guidelines for validation
- **Decision Module**: Treatment data included in decision context

### Clinical Guidelines Integration
- Guidelines stored in ChromaDB vector database
- Sources: ADA (American Diabetes Association), AHA (American Heart Association), WHO, ICMR
- Updated periodically to reflect latest clinical evidence
- Guideline validator matches diagnosis to relevant guideline, then checks treatment alignment

---

## 4. Policy Module

### Purpose
Manage insurance policy documents, extract policy terms, enable coverage analysis, and support policy-based decision-making through RAG.

### Responsibilities
- Handle policy document uploads (PDF, DOCX)
- Ingest policy documents into RAG pipeline
- Parse policy clauses and coverage rules
- Provide coverage breakdown for specific treatments
- Link policies to patients
- Track policy validity and expiration

### Key Components

#### PolicyController
**Endpoints**:
- POST /api/v1/policies/upload - Upload policy document
- GET /api/v1/policies/{policyId} - Get policy details
- GET /api/v1/policies/patient/{patientId} - Patient's policies
- POST /api/v1/policies/analyze - Analyze coverage for a treatment
- GET /api/v1/policies/{policyId}/coverage - Coverage breakdown
- PUT /api/v1/policies/{policyId} - Update policy metadata

**Authorization**:
- PATIENT can upload and view their own policies
- ADMIN can view and manage all policies

#### PolicyService
**Core Methods**:
- uploadPolicy(MultipartFile policyFile, PolicyMetadata metadata, String patientId): Uploads and ingests policy
- analyzeCoverage(CoverageAnalysisRequest request): Analyzes if treatment is covered
- getPolicyById(UUID policyId): Retrieves policy details
- getPoliciesByPatient(UUID patientId): Lists patient's policies
- updatePolicy(UUID policyId, PolicyMetadata metadata): Updates policy info
- deletePolicy(UUID policyId): Soft-deletes policy

**Business Logic Flow for Policy Upload**:
```
1. Receive policy PDF file + metadata (policy number, provider, etc.)
2. Validate file format (PDF or DOCX only)
3. Store file in document storage (filesystem or S3)
4. Create Policy entity in database with PROCESSING status
5. Trigger RAG ingestion pipeline asynchronously:
   a. Extract text from PDF
   b. Chunk text into semantic segments
   c. Generate embeddings for each chunk
   d. Store in ChromaDB with policy metadata
6. Update Policy status to ACTIVE once ingestion complete
7. Return PolicyResponse to client
```

**Business Logic Flow for Coverage Analysis**:
```
1. Receive analysis request (policyId, treatment details)
2. Retrieve policy from database
3. Call RAG pipeline to retrieve relevant policy clauses:
   - Query: "coverage for [treatment] [diagnosis]"
   - Filters: policy_id = [policyId]
4. Call CoverageCalculatorTool to apply policy rules
5. Generate coverage breakdown:
   - Covered items
   - Non-covered items (exclusions)
   - Co-payment amount
   - Deductibles
   - Sub-limits
   - Annual limit remaining
6. Return CoverageBreakdown
```

**Business Rules**:
- One patient can have multiple policies (primary, secondary)
- Policy must have a valid policy number
- Policy document required for full coverage analysis
- Policies expire and must be renewed
- Only active policies used for claim evaluation

#### PolicyRepository
**Interface**: Extends JpaRepository<Policy, UUID>

**Custom Queries**:
- findByPatientId(UUID patientId): Patient's policies
- findByPolicyNumber(String policyNumber): Lookup by policy number
- findByStatus(PolicyStatus status): Filter by status
- findByExpiryDateBefore(LocalDate date): Find expiring policies

#### Policy Model
**Entity**: JPA entity mapped to 'policies' table

**Fields**:
- id (UUID, primary key)
- patientId (UUID, foreign key to users)
- policyNumber (String, unique, indexed)
- providerName (String, insurance company name)
- planName (String, policy plan name)
- coverageAmount (BigDecimal, sum insured)
- premium (BigDecimal, annual premium)
- startDate (LocalDate, policy effective date)
- expiryDate (LocalDate, policy end date)
- status (Enum: ACTIVE, EXPIRED, SUSPENDED, CANCELLED)
- documentPath (String, path to stored policy PDF)
- documentUrl (String, URL to access policy document)
- isIngestedIntoRAG (Boolean, whether document is in vector DB)
- ingestionStatus (Enum: PENDING, PROCESSING, COMPLETED, FAILED)
- coverageCategories (JSON, list of covered categories)
- exclusions (JSON, list of exclusions)
- coPay (JSON, co-payment rules by category)
- annualLimit (BigDecimal, overall annual limit)
- annualLimitUsed (BigDecimal, amount utilized so far)
- createdAt (Timestamp)
- updatedAt (Timestamp)

**Relationships**:
- Many Policies → One Patient
- One Policy → Many Claims

#### DTOs
**PolicyUploadRequest**: policyFile (MultipartFile), policyNumber, providerName, planName, coverageAmount, premium, startDate, expiryDate
**PolicyResponse**: policyId, policyNumber, providerName, status, coverageAmount, expiryDate
**CoverageAnalysisRequest**: policyId, diagnosis, procedures, medications, estimatedCost
**CoverageBreakdown**: policyClause (list), coveredItems (list), nonCoveredItems (list), coPay, deductibles, payableByInsurance, patientResponsibility, annualLimitRemaining, warnings (list)

### Interactions with Other Modules
- **Auth Module**: Links policy to patient account
- **Claim Module**: Policy data used for claim validation
- **RAG Pipeline**: Policy document ingested into vector DB
- **Agent Orchestrator**: Policy clauses retrieved during decision-making
- **Decision Module**: Coverage breakdown influences decision

### Document Storage
- **File Storage**: PDFs stored in filesystem (dev) or S3 bucket (prod)
- **Naming Convention**: {patientId}/{policyId}_policy.pdf
- **Access Control**: Signed URLs for secure document access
- **Backup**: Regular backups to prevent data loss

---

## 5. Decision Module

### Purpose
Generate, store, and present AI-powered healthcare decisions with full explainability, reasoning traces, and citations.

### Responsibilities
- Trigger agent orchestrator for decision generation
- Store decision results with metadata
- Provide decision retrieval with explanations
- Track decision history and audit trail
- Generate patient-friendly summaries
- Support decision review and appeal

### Key Components

#### DecisionController
**Endpoints**:
- POST /api/v1/decisions/generate - Trigger new decision
- GET /api/v1/decisions/{decisionId} - Get decision details with explanation
- GET /api/v1/decisions/claim/{claimId} - Get decisions for a claim
- GET /api/v1/decisions/patient/{patientId} - Patient's decision history
- POST /api/v1/decisions/{decisionId}/feedback - Submit user feedback
- POST /api/v1/decisions/{decisionId}/appeal - Request manual review

**Authorization**:
- PATIENT can trigger decisions for their claims and view their decisions
- DOCTOR can view decisions related to their treatments
- ADMIN can view all decisions and override if necessary

#### DecisionService
**Core Methods**:
- generateDecision(DecisionRequest request, String userId): Triggers agent orchestrator
- getDecisionById(UUID decisionId): Retrieves full decision with explanation
- getDecisionsByClaim(UUID claimId): All decisions for a claim (including revisions)
- getDecisionsByPatient(UUID patientId): Patient's decision history
- submitFeedback(UUID decisionId, FeedbackRequest feedback): Records user feedback
- requestAppeal(UUID decisionId, AppealRequest appeal): Escalates to manual review

**Business Logic Flow for Decision Generation**:
```
1. Receive DecisionRequest (claimId, decisionType, context)
2. Fetch related entities from database:
   - Claim details
   - Patient information
   - Policy document
   - Treatment details
3. Build AgentContext object with all information
4. Call AgentOrchestrator.orchestrate(context)
5. Agent executes 8-step reasoning loop (see agent-flow.md):
   - Intent detection
   - Tool selection
   - Tool execution (RAG, drug safety, coverage calc, etc.)
   - Observation
   - LLM synthesis
   - Explanation generation
6. Receive DecisionResult from agent
7. Create Decision entity and persist to database
8. Update related Claim status if applicable
9. Send notification to patient
10. Return DecisionResponse
```

**Business Rules**:
- One claim can have multiple decisions (initial + revisions + appeals)
- Most recent decision is the active one
- Decisions are immutable once created (audit requirement)
- Decisions with confidence < 70% automatically flagged for manual review
- Patients can appeal rejected decisions within 30 days

#### DecisionRepository
**Interface**: Extends JpaRepository<Decision, UUID>

**Custom Queries**:
- findByClaimId(UUID claimId): All decisions for a claim
- findByClaimIdAndActiveTrue(UUID claimId): Active decision for a claim
- findByPatientId(UUID patientId): Patient's decision history
- findByOutcome(DecisionOutcome outcome): Filter by outcome
- findByConfidenceScoreLessThan(Double threshold): Low-confidence decisions

#### Decision Model
**Entity**: JPA entity mapped to 'decisions' table

**Fields**:
- id (UUID, primary key)
- decisionNumber (String, unique, format: DEC-YYYYMMDD-XXXX)
- claimId (UUID, foreign key to claims, nullable for non-claim decisions)
- patientId (UUID, foreign key to users)
- decisionType (Enum: CLAIM_ELIGIBILITY, TREATMENT_VALIDATION, DRUG_SAFETY, BILL_EXPLANATION, POLICY_COVERAGE)
- outcome (Enum: APPROVED, REJECTED, PARTIAL, WARNING, INFO)
- payableAmount (BigDecimal, for CLAIM_ELIGIBILITY decisions)
- patientResponsibility (BigDecimal, amount patient must pay)
- reasoning (Text, detailed explanation from LLM)
- confidenceScore (Double, 0-100)
- citations (JSON, list of policy clauses, guidelines, sources)
- warnings (JSON, list of warnings or concerns)
- suggestedActions (JSON, list of recommendations for patient)
- friendlySummary (Text, patient-friendly explanation)
- toolsUsed (JSON, list of tools agent executed)
- processingTimeMs (Integer, how long agent took)
- isActive (Boolean, whether this is the current decision for the claim)
- reviewStatus (Enum: NOT_REVIEWED, UNDER_REVIEW, REVIEWED, OVERRIDDEN)
- reviewedBy (UUID, admin who reviewed, nullable)
- reviewNotes (Text, admin review comments)
- createdAt (Timestamp)

**Relationships**:
- Many Decisions → One Claim
- Many Decisions → One Patient
- One Decision → One Admin (reviewer, optional)

#### DTOs
**DecisionRequest**: claimId (optional), decisionType, context (map of key-value pairs), patientId
**DecisionResponse**: decisionId, decisionNumber, outcome, payableAmount, confidenceScore, reasoning, citations, warnings, suggestedActions, friendlySummary, createdAt
**DecisionWithExplanation**: Full decision +reasoning trace + tool execution log + confidence breakdown
**FeedbackRequest**: rating (1-5 stars), comment, wasHelpful (boolean)
**AppealRequest**: reason, additionalDocuments (list)

### Interactions with Other Modules
- **Claim Module**: Decision updates claim status
- **Agent Orchestrator**: Core decision logic executed here
- **RAG Pipeline**: Retrieves policy clauses and guidelines
- **Explainability Engine**: Generates citations and confidence scores
- **Auth Module**: Validates user authorization

### Explainability Features
Every decision includes:
1. **Outcome**: Clear APPROVED / REJECTED / PARTIAL status
2. **Reasoning**: Step-by-step explanation of why decision was made
3. **Citations**: Specific policy clauses, guideline references with page numbers
4. **Confidence Score**: Transparent confidence level (0-100%)
5. **Warnings**: Proactive alerts about risks, limits, or concerns
6. **Suggested Actions**: Actionable recommendations for patient
7. **Friendly Summary**: Jargon-free explanation suitable for non-experts

---

## 6. Agent Orchestrator

### Purpose
Core AI reasoning engine that coordinates multi-step decision-making, tool invocation, and LLM synthesis.

### Responsibilities
- Detect user intent from request context
- Select appropriate tools for the task
- Execute tools in optimal order (parallel where possible)
- Observe tool results and determine if sufficient
- Synthesize results using LLM
- Generate structured output
- Handle errors and retries

### Key Components

#### AgentOrchestrator (Service)
**Core Method**:
- orchestrate(AgentContext context): Main entry point for agent reasoning loop

**Supporting Methods**:
- detectIntent(AgentContext context): Analyzes context to determine intent
- selectTools(Intent intent, AgentContext context): Chooses which tools to invoke
- executeTools(List<AgentTool> tools, AgentContext context): Runs tools (parallel if possible)
- observeResults(List<ToolResult> results): Validates if results are sufficient
- synthesizeWithLLM(List<ToolResult> results, AgentContext context): Calls LLM for reasoning
- buildResponse(String llmOutput, List<ToolResult> toolResults): Constructs final decision

**Orchestration Flow**:
Detailed 8-step flow described in agent-flow.md

#### AgentContext (Data Class)
Holds all information needed for agent decision-making:
- requestType: Type of decision being made
- claimData: Claim details if applicable
- patientData: Patient info (age, conditions, allergies)
- policyData: Policy info (coverage, limits, rules)
- treatmentData: Treatment details (diagnosis, medications, procedures)
- hospitalData: Hospital info (network status, tier)
- metadata: Additional key-value pairs

#### AgentTools (Package)
Contains all domain-specific tools the agent can invoke.

**Tool Interface**:
```
interface AgentTool {
    String getName();
    String getDescription();
    ToolResult execute(ToolInput input);
}
```

**Available Tools**:
See Tool Catalog section below.

### Tool Catalog

#### PolicyRetrieverTool
**Purpose**: Retrieve relevant policy clauses from RAG vector database
**Input**: query (String), policyId (UUID), topK (int)
**Output**: List of PolicyClause with text, clause number, similarity score
**Implementation**: Calls RAGPipelineService.retrieve()

#### DrugSafetyTool
**Purpose**: Check drug interactions and contraindications
**Input**: medications (List<String>), patientConditions, patientAge
**Output**: DrugSafetyReport with interactions, contraindications, warnings
**Implementation**: Queries drug interaction database (external API or local DB)

#### GuidelineValidatorTool
**Purpose**: Validate treatment against clinical best practices
**Input**: diagnosis, treatmentPlan, medications, procedures
**Output**: GuidelineValidationReport with compliance status, deviations, recommendations
**Implementation**: Retrieves guidelines from RAG, compares treatment plan

#### CoverageCalculatorTool
**Purpose**: Calculate exact payable amounts based on policy rules
**Input**: policyId, treatmentCost, hospitalType, treatmentCategory
**Output**: CoverageCalculation with baseCoverage, coPay, discounts, payableAmount
**Implementation**: Fetches policy rules from DB, applies calculation logic

#### ClaimValidationTool
**Purpose**: Verify claim data completeness and integrity
**Input**: claimData (Claim object)
**Output**: ValidationResult with valid (boolean), completenessScore, errors, warnings
**Implementation**: Business rule validation (required fields, data consistency)

#### BillingAnalyzerTool
**Purpose**: Break down hospital bills into understandable components
**Input**: billDocument (file or structured data)
**Output**: BillBreakdown with lineItems, categoryTotals, coveredItems, explanations
**Implementation**: Parses bill, maps to policy coverage

### Interactions with Other Modules
- **Decision Module**: Called by DecisionService to generate decisions
- **RAG Pipeline**: Invokes PolicyRetrieverTool which queries ChromaDB
- **Treatment Module**: Invokes GuidelineValidatorTool and DrugSafetyTool
- **LLM Integration**: Sends prompts to OpenAI/Ollama for reasoning
- **Explainability Engine**: Receives LLM output for post-processing

### Error Handling
- Tool execution failures logged but don't halt entire agent
- If critical tool fails (like PolicyRetriever), agent retries with modified query
- If multiple tools fail, agent returns LOW_CONFIDENCE decision
- All errors recorded for debugging and QA

### Performance Optimization
- Tools executed in parallel where no dependencies exist
- Tool results cached for frequent queries (TTL: 5 minutes)
- LLM responses cached for identical contexts
- Timeout limits on tool execution to prevent hanging

---

## 7. RAG Pipeline Module

### Purpose
Handle document ingestion and semantic retrieval for the Retrieval-Augmented Generation system.

### Responsibilities
- Ingest policy documents, medical guidelines, drug information
- Extract text from various document formats
- Chunk text into semantic segments
- Generate embeddings for chunks
- Store vectors in ChromaDB
- Retrieve relevant context via similarity search
- Maintain and monitor vector database health

### Key Components

#### DocumentIngestionService
**Core Methods**:
- ingestDocument(byte[] documentBytes, DocumentMetadata metadata): Full ingestion pipeline
- extractText(byte[] documentBytes, FileFormat format): Text extraction
- chunkText(String text, ChunkingStrategy strategy): Split into chunks
- generateEmbeddings(List<String> chunks): Create vector embeddings
- storeInVectorDB(List<TextChunk> chunks, List<float[]> embeddings): Persist to ChromaDB

**Chunking Strategies**:
- Semantic chunking: Respect document structure (clauses, sections)
- Sentence-based chunking: Group 3-5 sentences
- Fixed-size chunking: 400 tokens with 50-token overlap

**Supported Formats**: PDF, DOCX, TXT, HTML

#### EmbeddingService
**Core Methods**:
- embedText(String text): Generate embedding for single text
- embedBatch(List<String> texts): Generate embeddings in batch
- getEmbeddingDimension(): Returns embedding vector size

**Embedding Models**:
- OpenAI text-embedding-ada-002 (1536 dimensions)
- OpenAI text-embedding-3-large (3072 dimensions)
- Local: all-MiniLM-L6-v2 (384 dimensions)

#### VectorRetriever
**Core Methods**:
- retrieve(String query, RetrievalFilters filters, int topK):Semantic search
- hybridSearch(String query, List<String> keywords, filters, topK): Combined semantic + keyword
- retrieveWithReranking(String query, filters, topK): Advanced reranking

**Retrieval Filters**:
- policyId: Scope to specific policy
- category: Filter by document category
- dateRange: Only recent documents
- minSimilarity: Minimum similarity threshold

#### HybridRetriever
Combines vector similarity with keyword matching using BM25 and Reciprocal Rank Fusion.

#### RAGPipeline (Facade)
**Unified Interface**:
- ingest(byte[] document, metadata): One-stop ingestion
- retrieve(String query, filters, topK): One-stop retrieval

Detailed RAG architecture in rag-pipeline.md.

### Interactions with Other Modules
- **Policy Module**: Ingests uploaded policy documents
- **Agent Orchestrator**: Provides retrieved context for decisions
- **Decision Module**: Supplies policy clauses for citations

### ChromaDB Configuration
- Collections: insurance_policies, medical_guidelines, drug_information, hospital_networks
- Index: HNSW (Hierarchical Navigable Small World)
- Persistence: Local storage or cloud (S3)

---

## 8. Explainability Engine Module

### Purpose
Post-process agent outputs to create patient-friendly, transparent explanations with citations, confidence scores, and actionable recommendations.

### Responsibilities
- Extract decision outcome from LLM response
- Map reasoning to source citations
- Calculate confidence scores
- Identify warnings and risks
- Generate patient-friendly summaries
- Format explanations for UI display

### Key Components

#### ExplanationEngine (Service)
**Core Methods**:
- buildExplanation(String llmReasoning, List<ToolResult> toolResults): Main method
- extractOutcome(String llmReasoning): Parse decision outcome
- extractCitations(String llmReasoning, List<ToolResult> toolResults): Map clause references
- calculateConfidence(List<ToolResult> toolResults): Score confidence
- extractWarnings(String llmReasoning): Identify warnings
- generateFriendlySummary(DecisionData decision): Create patient-friendly text

#### ReasoningTracer
**Purpose**: Track agent's step-by-step thought process
**Output**: List of reasoning steps with timestamps for UI display

#### ConfidenceScorer
**Purpose**: Calculate confidence score based on multiple factors
**Factors**:
- Retrieval quality (similarity scores)
- Tool success rate
- LLM confidence indicators
- Data completeness

**Formula**: Weighted average of factors, normalized to 0-100 scale

### Interactions with Other Modules
- **Agent Orchestrator**: Receives LLM output and tool results
- **Decision Module**: Provides processed explanation for storage
- **Frontend**: Structured explanation displayed in UI

### Citation Mapping
LLM mentions "Clause 4.2.1" → Engine fetches: {source: "policy.pdf", page: 12, text: "full clause text"}

---

## Common Utilities Module

### GlobalExceptionHandler
Centralized exception handling for consistent error responses across all controllers.

**Handled Exceptions**:
- ResourceNotFoundException → 404
- UnauthorizedException → 401
- ForbiddenException → 403
- ValidationException → 400
- InternalServerException → 500

**Error Response Format**:
```
{
  "timestamp": "2026-02-19T14:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Claim with ID abc-123 not found",
  "path": "/api/v1/claims/abc-123"
}
```

### ApiResponse
Standardized response wrapper for consistent API responses.

**Success Response**:
```
{
  "success": true,
  "message": "Claim submitted successfully",
  "data": { claimId: "...", status: "PENDING", ... }
}
```

### ValidationUtils
Common validation logic used across modules:
- Email format validation
- Phone number validation
- Date range validation
- Amount validation (non-negative, within limits)

---

## Module Interaction Summary

```
AuthModule → All modules (provides authentication)
ClaimModule → DecisionModule (triggers decisions)
ClaimModule → PolicyModule (validates coverage)
ClaimModule → TreatmentModule (links treatment data)
TreatmentModule → AgentOrchestrator (validates treatment plans)
PolicyModule → RAGPipeline (ingests documents)
DecisionModule → AgentOrchestrator (generates decisions)
AgentOrchestrator → RAGPipeline (retrieves context)
AgentOrchestrator → ExplainabilityEngine (processes output)
```

---

This modular architecture ensures clear separation of concerns, easy testing, and scalability as the system grows.
