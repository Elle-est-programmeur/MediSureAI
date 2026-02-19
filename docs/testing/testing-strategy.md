# Testing Strategy

## Testing Philosophy

MediSureAI operates in the healthcare and insurance domain where **accuracy, reliability, and explainability are critical**. Our testing philosophy reflects these high standards:

### Core Principles

**1. Safety First**
- Medical decisions impact patient health; errors can have serious consequences
- Every code change must be validated against safety criteria
- Critical paths (claim decisions, drug interactions) have 100% test coverage requirement

**2. Test-Driven Confidence**
- Tests are not an afterthought; they are part of the definition of "done"
- Write tests before fixing bugs (reproduce issue first)
- Code without tests is considered incomplete

**3. Multi-Layer Defense**
- Unit tests: Fast feedback on isolated logic
- Integration tests: Verify component interactions
- End-to-end tests: Validate complete user workflows
- AI validation tests: Ensure model outputs meet quality standards

**4. Realistic Test Data**
- Use anonymized real-world insurance policies, medical guidelines
- Mock only external dependencies (LLM APIs), not business logic
- Maintain test data sets that reflect production diversity

**5. Continuous Testing**
- All tests run on every commit (CI pipeline)
- Nightly regression suite for slow-running AI tests
- Performance benchmarks tracked over time

### Testing Pyramid

```
           /\
          /  \         E2E Tests (5-10%)
         /____\        - Critical user workflows
        /      \       - Smoke tests
       /________\      
      /          \     Integration Tests (20-30%)
     /____________\    - API endpoints with database
    /              \   - RAG pipeline with ChromaDB
   /________________\  - Agent orchestration
  /                  \ 
 /____________________\ Unit Tests (60-70%)
                        - Service logic
                        - Utilities
                        - Validation rules
```

**Distribution**:
- **Unit Tests**: 60-70% (fast, isolated, abundant)
- **Integration Tests**: 20-30% (moderate speed, realistic interactions)
- **E2E Tests**: 5-10% (slow, full system validation)
- **AI Validation Tests**: Special category (assessed separately)

---

## Unit Testing Scope

Unit tests validate individual components in isolation with mocked dependencies.

### What to Unit Test

#### 1. Service Layer Business Logic

**High Priority**:
- `ClaimService`: Claim validation, status transitions, business rules
- `DecisionService`: Decision outcome calculation, confidence scoring
- `PolicyService`: Coverage calculation, eligibility logic
- `TreatmentService`: Treatment plan validation, cost estimation
- `AgentOrchestrator`: Agent loop control flow, tool selection logic

**Medium Priority**:
- `AuthService`: User registration, password validation, JWT generation
- `ExplainabilityEngine`: Reasoning generation, citation formatting
- `RAGRetrievalService`: Query transformation, result ranking

**Example Test**:
```java
@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {
    @Mock
    private ClaimRepository claimRepository;
    
    @Mock
    private PolicyService policyService;
    
    @InjectMocks
    private ClaimService claimService;
    
    @Test
    void submitClaim_WhenPolicyActive_ShouldCreatePendingClaim() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID policyId = UUID.randomUUID();
        ClaimDTO claimDTO = ClaimDTO.builder()
            .policyId(policyId)
            .claimAmount(new BigDecimal("85000.00"))
            .build();
        
        Policy mockPolicy = Policy.builder()
            .id(policyId)
            .status(PolicyStatus.ACTIVE)
            .coverageAmount(new BigDecimal("500000.00"))
            .build();
        
        when(policyService.findById(policyId)).thenReturn(mockPolicy);
        when(claimRepository.save(any(Claim.class))).thenAnswer(i -> i.getArgument(0));
        
        // Act
        Claim result = claimService.submitClaim(userId, claimDTO);
        
        // Assert
        assertThat(result.getStatus()).isEqualTo(ClaimStatus.PENDING);
        assertThat(result.getClaimAmount()).isEqualByComparingTo("85000.00");
        verify(claimRepository).save(any(Claim.class));
    }
    
    @Test
    void submitClaim_WhenPolicyExpired_ShouldThrowException() {
        // Arrange
        UUID policyId = UUID.randomUUID();
        ClaimDTO claimDTO = ClaimDTO.builder()
            .policyId(policyId)
            .claimAmount(new BigDecimal("85000.00"))
            .build();
        
        Policy expiredPolicy = Policy.builder()
            .id(policyId)
            .status(PolicyStatus.EXPIRED)
            .build();
        
        when(policyService.findById(policyId)).thenReturn(expiredPolicy);
        
        // Act & Assert
        assertThatThrownBy(() -> claimService.submitClaim(UUID.randomUUID(), claimDTO))
            .isInstanceOf(PolicyExpiredException.class)
            .hasMessageContaining("Policy has expired");
    }
}
```

#### 2. Utility Classes and Helpers

**ValidationUtils**: All validation methods
- `isValidEmail()`, `isValidPhoneNumber()`, `isValidICD10Code()`
- Edge cases: null, empty, malformed input

**FormatUtils**: All formatting methods
- `formatCurrency()`, `formatDate()`, `formatConfidenceScore()`
- Different locales, edge values (very large/small numbers)

**PolicyCalculator**: Coverage calculations
- Co-pay calculation with various percentages
- Network vs non-network pricing
- Annual limit tracking

**Example Test**:
```java
class ValidationUtilsTest {
    @ParameterizedTest
    @ValueSource(strings = {
        "alice@example.com",
        "bob+test@company.org",
        "user123@sub.domain.co.uk"
    })
    void isValidEmail_WithValidEmails_ReturnsTrue(String email) {
        assertThat(ValidationUtils.isValidEmail(email)).isTrue();
    }
    
    @ParameterizedTest
    @ValueSource(strings = {
        "notanemail",
        "@example.com",
        "user@",
        "user @example.com"
    })
    void isValidEmail_WithInvalidEmails_ReturnsFalse(String email) {
        assertThat(ValidationUtils.isValidEmail(email)).isFalse();
    }
    
    @Test
    void isValidEmail_WithNull_ReturnsFalse() {
        assertThat(ValidationUtils.isValidEmail(null)).isFalse();
    }
}
```

#### 3. Domain Models and DTOs

**Model Validation**:
- Test `@NotNull`, `@Size`, `@Pattern` annotations work correctly
- Test custom validators (e.g., `@ValidICD10Code`)

**DTO Mapping**:
- Model → DTO conversion preserves all fields
- DTO → Model conversion handles nulls correctly

**Example Test**:
```java
class ClaimDTOTest {
    private Validator validator;
    
    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    
    @Test
    void claimDTO_WithValidData_PassesValidation() {
        ClaimDTO dto = ClaimDTO.builder()
            .policyId(UUID.randomUUID())
            .treatmentId(UUID.randomUUID())
            .claimAmount(new BigDecimal("85000.00"))
            .build();
        
        Set<ConstraintViolation<ClaimDTO>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }
    
    @Test
    void claimDTO_WithNullPolicyId_FailsValidation() {
        ClaimDTO dto = ClaimDTO.builder()
            .policyId(null)
            .treatmentId(UUID.randomUUID())
            .claimAmount(new BigDecimal("85000.00"))
            .build();
        
        Set<ConstraintViolation<ClaimDTO>> violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .contains("Policy ID is required");
    }
}
```

### What NOT to Unit Test

**Avoid testing**:
- Framework code (Spring Boot, JPA)
- Simple getters/setters with no logic
- Configuration classes with only annotations
- Auto-generated code (Lombok builders)
- Database queries (covered by integration tests)

### Mocking Strategy

**Mock External Dependencies**:
- LLM APIs (OpenAI, Ollama) → Mock with predefined responses
- ChromaDB → Mock with in-memory test vectors
- File storage → Mock with temporary directories
- Email services → Mock to avoid sending real emails

**Do NOT Mock**:
- Business logic services (test real implementations)
- Domain models (use real objects)
- DTOs (use real objects)
- Utility classes (test real code)

**Mocking Tools**:
- **Mockito**: Primary mocking framework
- **MockMvc**: For controller testing
- **@MockBean**: For Spring context mocking

---

## API / Integration Testing Scope

Integration tests validate interactions between components and external systems.

### What to Integration Test

#### 1. REST API Endpoints (Controller → Service → Repository)

**Test each endpoint**:
- Happy path (valid request → 200/201)
- Invalid input (missing fields, bad format → 400)
- Unauthorized access (no token → 401)
- Forbidden access (wrong role → 403)
- Not found (invalid ID → 404)

**Example Test**:
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase
class ClaimControllerIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private ClaimRepository claimRepository;
    
    private String jwtToken;
    
    @BeforeEach
    void setUp() {
        // Login and get JWT token
        LoginRequest loginRequest = new LoginRequest("patient@test.com", "Password123");
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
            "/api/v1/auth/login",
            loginRequest,
            LoginResponse.class
        );
        jwtToken = response.getBody().getToken();
    }
    
    @Test
    void submitClaim_WithValidData_Returns201Created() {
        // Arrange
        ClaimDTO claimDTO = ClaimDTO.builder()
            .policyId(UUID.fromString("661e8400-e29b-41d4-a716-446655440002"))
            .treatmentId(UUID.fromString("772e8400-e29b-41d4-a716-446655440003"))
            .claimAmount(new BigDecimal("85000.00"))
            .build();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        HttpEntity<ClaimDTO> request = new HttpEntity<>(claimDTO, headers);
        
        // Act
        ResponseEntity<ApiResponse<ClaimResponseDTO>> response = restTemplate.exchange(
            "/api/v1/claims/submit",
            HttpMethod.POST,
            request,
            new ParameterizedTypeReference<ApiResponse<ClaimResponseDTO>>() {}
        );
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getStatus()).isEqualTo(ClaimStatus.PENDING);
        
        // Verify database persistence
        List<Claim> claims = claimRepository.findAll();
        assertThat(claims).hasSize(1);
        assertThat(claims.get(0).getClaimAmount()).isEqualByComparingTo("85000.00");
    }
    
    @Test
    void submitClaim_WithoutAuthentication_Returns401Unauthorized() {
        // Arrange
        ClaimDTO claimDTO = ClaimDTO.builder()
            .policyId(UUID.randomUUID())
            .claimAmount(new BigDecimal("85000.00"))
            .build();
        
        // Act
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
            "/api/v1/claims/submit",
            claimDTO,
            ApiResponse.class
        );
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
```

#### 2. Database Interactions (Repository Layer)

**Test custom queries**:
- Custom `@Query` methods
- Complex JPA specifications
- Pagination and sorting
- Transactions and rollback behavior

**Example Test**:
```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ClaimRepositoryTest {
    @Autowired
    private ClaimRepository claimRepository;
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Test
    void findByPatientId_WhenClaimsExist_ReturnsAllClaims() {
        // Arrange
        UUID patientId = UUID.randomUUID();
        Claim claim1 = Claim.builder()
            .patientId(patientId)
            .claimAmount(new BigDecimal("85000.00"))
            .status(ClaimStatus.PENDING)
            .build();
        Claim claim2 = Claim.builder()
            .patientId(patientId)
            .claimAmount(new BigDecimal("50000.00"))
            .status(ClaimStatus.APPROVED)
            .build();
        entityManager.persist(claim1);
        entityManager.persist(claim2);
        entityManager.flush();
        
        // Act
        List<Claim> results = claimRepository.findByPatientId(patientId);
        
        // Assert
        assertThat(results).hasSize(2);
        assertThat(results).extracting(Claim::getClaimAmount)
            .containsExactlyInAnyOrder(
                new BigDecimal("85000.00"),
                new BigDecimal("50000.00")
            );
    }
    
    @Test
    void findByStatusAndSubmittedDateBetween_ReturnsFilteredClaims() {
        // Test complex query with multiple parameters
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        
        // ... test implementation
    }
}
```

#### 3. Security and Authentication

**Test JWT authentication**:
- Valid token → access granted
- Expired token → 401 Unauthorized
- Invalid signature → 401 Unauthorized
- Missing roles → 403 Forbidden

**Test role-based access**:
- PATIENT can access own claims only
- DOCTOR can access own patients' treatments
- ADMIN can access all resources

**Example Test**:
```java
@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void accessAdminEndpoint_WithPatientRole_Returns403Forbidden() throws Exception {
        String patientToken = generateTokenForRole(UserRole.PATIENT);
        
        mockMvc.perform(get("/api/v1/admin/claims")
                .header("Authorization", "Bearer " + patientToken))
            .andExpect(status().isForbidden());
    }
    
    @Test
    void accessAdminEndpoint_WithAdminRole_Returns200OK() throws Exception {
        String adminToken = generateTokenForRole(UserRole.ADMIN);
        
        mockMvc.perform(get("/api/v1/admin/claims")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());
    }
}
```

---

## RAG Pipeline Testing Approach

Testing the RAG (Retrieval-Augmented Generation) pipeline requires special strategies due to its ML/AI nature.

### Challenge: Non-Deterministic Outputs

**Problem**: LLM outputs vary across runs even with same input.

**Solution**: Test for **quality characteristics** rather than exact outputs.

### Testing Strategies

#### 1. Retrieval Quality Testing

**Goal**: Verify ChromaDB retrieves relevant documents for queries.

**Metrics**:
- **Recall@K**: Percentage of relevant docs in top K results
- **Precision@K**: Percentage of retrieved docs that are relevant
- **MRR (Mean Reciprocal Rank)**: Position of first relevant result

**Test Setup**:
```java
@SpringBootTest
class RAGRetrievalQualityTest {
    @Autowired
    private RAGRetrievalService retrievalService;
    
    @Autowired
    private ChromaDBClient chromaDBClient;
    
    @BeforeEach
    void setUp() {
        // Load test documents into ChromaDB
        loadTestPolicyDocument("test_policy_diabetes.pdf");
        loadTestPolicyDocument("test_policy_cardiac.pdf");
    }
    
    @Test
    void retrieveRelevantChunks_DiabetesQuery_ReturnsHighRecall() {
        // Arrange
        String query = "Is insulin therapy covered for Type 2 Diabetes?";
        int k = 5;
        
        // Expected relevant document IDs (manually labeled ground truth)
        Set<String> relevantDocIds = Set.of(
            "chunk_diabetes_001",
            "chunk_diabetes_002",
            "chunk_diabetes_coverage"
        );
        
        // Act
        List<RetrievalResult> results = retrievalService.retrieve(query, k);
        Set<String> retrievedDocIds = results.stream()
            .map(RetrievalResult::getDocumentId)
            .collect(Collectors.toSet());
        
        // Assert
        long relevantRetrieved = retrievedDocIds.stream()
            .filter(relevantDocIds::contains)
            .count();
        
        double recallAtK = (double) relevantRetrieved / relevantDocIds.size();
        assertThat(recallAtK).isGreaterThanOrEqualTo(0.6); // At least 60% recall
        
        // Check top result is highly relevant
        assertThat(results.get(0).getSimilarityScore()).isGreaterThan(0.75);
    }
    
    @Test
    void retrieveRelevantChunks_CardiacQuery_DoesNotReturnDiabetesContent() {
        // Arrange
        String query = "Is cardiac bypass surgery covered?";
        int k = 5;
        
        // Act
        List<RetrievalResult> results = retrievalService.retrieve(query, k);
        
        // Assert - should not retrieve diabetes-related chunks
        long diabetesChunks = results.stream()
            .filter(r -> r.getContent().toLowerCase().contains("diabetes"))
            .count();
        
        assertThat(diabetesChunks).isLessThanOrEqualTo(1); // At most 1 false positive
    }
}
```

#### 2. Embedding Quality Testing

**Goal**: Verify embedding model produces semantically meaningful vectors.

**Test**: Similar concepts should have high cosine similarity.

```java
@Test
void embeddingService_SimilarConcepts_HighCosineSimilarity() {
    // Arrange
    String text1 = "Insulin therapy for diabetes";
    String text2 = "Diabetes treatment with insulin";
    String text3 = "Heart bypass surgery"; // Unrelated
    
    // Act
    float[] embedding1 = embeddingService.generateEmbedding(text1);
    float[] embedding2 = embeddingService.generateEmbedding(text2);
    float[] embedding3 = embeddingService.generateEmbedding(text3);
    
    // Assert
    double similarity12 = cosineSimilarity(embedding1, embedding2);
    double similarity13 = cosineSimilarity(embedding1, embedding3);
    
    assertThat(similarity12).isGreaterThan(0.8); // Similar texts
    assertThat(similarity13).isLessThan(0.5);    // Unrelated texts
}
```

#### 3. Document Ingestion Testing

**Goal**: Verify documents are correctly parsed, chunked, and stored.

```java
@Test
void ingestDocument_ValidPDF_StoresAllChunks() {
    // Arrange
    Path pdfPath = Paths.get("src/test/resources/test_policy.pdf");
    String policyId = UUID.randomUUID().toString();
    
    // Act
    IngestionResult result = ragIngestionService.ingestDocument(pdfPath, policyId);
    
    // Assert
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getChunksProcessed()).isGreaterThan(10);
    assertThat(result.getEmbeddingsStored()).isEqualTo(result.getChunksProcessed());
    
    // Verify data in ChromaDB
    int storedCount = chromaDBClient.countDocuments(policyId);
    assertThat(storedCount).isEqualTo(result.getChunksProcessed());
}
```

#### 4. End-to-End RAG Testing

**Goal**: Test complete RAG pipeline from query to final answer.

```java
@Test
void ragPipeline_CompleteFlow_GeneratesAccurateAnswer() {
    // Arrange
    String question = "What is the co-pay percentage for diabetes treatment?";
    String policyId = "test-policy-123";
    
    // Expected answer (from ground truth test data)
    String expectedCoPay = "25%";
    
    // Act
    RAGResponse response = ragPipeline.ask(question, policyId);
    
    // Assert
    assertThat(response.getAnswer()).containsIgnoringCase(expectedCoPay);
    assertThat(response.getCitations()).isNotEmpty();
    assertThat(response.getConfidenceScore()).isGreaterThan(0.7);
    
    // Verify citations point to correct policy section
    assertThat(response.getCitations().get(0).getClause()).contains("4.2");
}
```

### RAG Test Data Setup

**Create test document corpus**:
```
src/test/resources/
├── test_policies/
│   ├── diabetes_policy.pdf
│   ├── cardiac_policy.pdf
│   └── general_policy.pdf
├── test_guidelines/
│   ├── ada_diabetes_guidelines.pdf
│   └── aha_cardiac_guidelines.pdf
└── expected_answers/
    ├── diabetes_queries.json
    └── cardiac_queries.json
```

**Ground truth format** (`diabetes_queries.json`):
```json
[
  {
    "query": "Is insulin therapy covered?",
    "expectedAnswer": "Yes, covered with 25% co-payment",
    "relevantDocuments": ["diabetes_policy.pdf"],
    "relevantClauses": ["4.2.1"],
    "minimumConfidence": 0.8
  },
  {
    "query": "What is the annual limit for diabetes care?",
    "expectedAnswer": "₹100,000 per year",
    "relevantDocuments": ["diabetes_policy.pdf"],
    "relevantClauses": ["4.3"],
    "minimumConfidence": 0.85
  }
]
```

---

## Agent Decision Testing

Testing the multi-step agent orchestrator is complex due to its dynamic tool selection and reasoning.

### Testing Strategy

#### 1. Tool Execution Testing

**Test each tool independently**:
- PolicyRetrieverTool
- DrugSafetyTool
- GuidelineValidatorTool
- CoverageCalculatorTool
- ClaimValidationTool

**Example**:
```java
@Test
void policyRetrieverTool_ValidQuery_ReturnsPolicyClause() {
    // Arrange
    PolicyRetrieverTool tool = new PolicyRetrieverTool(ragRetrievalService);
    ToolInput input = ToolInput.builder()
        .parameter("query", "diabetes coverage")
        .parameter("policyId", "test-policy-123")
        .build();
    
    // Act
    ToolOutput output = tool.execute(input);
    
    // Assert
    assertThat(output.isSuccess()).isTrue();
    assertThat(output.getResult()).contains("diabetes");
    assertThat(output.getCitations()).isNotEmpty();
}
```

#### 2. Agent Loop Testing

**Test complete agent reasoning loop**:

```java
@SpringBootTest
class AgentOrchestratorTest {
    @Autowired
    private AgentOrchestrator orchestrator;
    
    @MockBean
    private OpenAIClient openAIClient; // Mock LLM calls
    
    @Test
    void processDecision_DiabetesClaim_CompletesSuccessfully() {
        // Arrange
        DecisionContext context = DecisionContext.builder()
            .decisionType(DecisionType.CLAIM_ELIGIBILITY)
            .claimId(UUID.randomUUID())
            .patientAge(45)
            .diagnosis("Type 2 Diabetes")
            .treatmentPlan("Insulin therapy")
            .estimatedCost(new BigDecimal("85000.00"))
            .build();
        
        // Mock LLM response for reasoning synthesis
        when(openAIClient.chat(any())).thenReturn(
            ChatResponse.builder()
                .content("The claim is APPROVED based on policy clause 4.2.1...")
                .build()
        );
        
        // Act
        Decision decision = orchestrator.generateDecision(context);
        
        // Assert
        assertThat(decision.getOutcome()).isIn(DecisionOutcome.APPROVED, DecisionOutcome.PARTIAL);
        assertThat(decision.getPayableAmount()).isGreaterThan(BigDecimal.ZERO);
        assertThat(decision.getReasoning()).isNotBlank();
        assertThat(decision.getCitations()).isNotEmpty();
        assertThat(decision.getConfidenceScore()).isGreaterThan(70.0);
        
        // Verify tools were called
        verify(openAIClient, atLeastOnce()).chat(any());
    }
    
    @Test
    void processDecision_InsufficientRetrieval_RetriesWithRefinedQuery() {
        // Test retry mechanism when initial retrieval confidence is low
        DecisionContext context = DecisionContext.builder()
            .decisionType(DecisionType.CLAIM_ELIGIBILITY)
            .diagnosis("Rare disease XYZ")
            .build();
        
        // First retrieval returns low confidence
        // Agent should refine query and retry
        // Final decision should still be generated (even if confidence is lower)
        
        Decision decision = orchestrator.generateDecision(context);
        
        assertThat(decision).isNotNull();
        // May have lower confidence but should complete
        assertThat(decision.getConfidenceScore()).isGreaterThan(50.0);
    }
}
```

#### 3. Decision Quality Testing

**Test decision characteristics**:

```java
@Test
void generateDecision_ForApprovedClaim_MeetsQualityStandards() {
    // Arrange
    DecisionContext context = createValidClaimContext();
    
    // Act
    Decision decision = agentOrchestrator.generateDecision(context);
    
    // Assert quality standards
    assertThat(decision.getReasoning())
        .withFailMessage("Reasoning should be at least 100 characters")
        .hasSizeGreaterThanOrEqualTo(100);
    
    assertThat(decision.getCitations())
        .withFailMessage("Should have at least 2 citations")
        .hasSizeGreaterThanOrEqualTo(2);
    
    assertThat(decision.getCitations().get(0).getSource())
        .withFailMessage("Citations should include policy source")
        .isNotBlank();
    
    assertThat(decision.getFriendlySummary())
        .withFailMessage("Should generate non-technical summary")
        .doesNotContain("UUID", "null", "ERROR");
    
    assertThat(decision.getProcessingTimeMs())
        .withFailMessage("Processing should complete within 10 seconds")
        .isLessThan(10000);
}
```

### Performance Testing

**Test agent processing time**:
```java
@Test
void generateDecision_PerformanceBenchmark_CompletesUnder5Seconds() {
    // Warm up
    for (int i = 0; i < 3; i++) {
        orchestrator.generateDecision(createValidContext());
    }
    
    // Measure
    long startTime = System.currentTimeMillis();
    Decision decision = orchestrator.generateDecision(createValidContext());
    long duration = System.currentTimeMillis() - startTime;
    
    assertThat(duration).isLessThan(5000); // 5 second threshold
    assertThat(decision.getProcessingTimeMs()).isEqualTo(duration, within(100L));
}
```

---

## Recommended Tools

### Backend Testing (Java/Spring Boot)

| Tool | Purpose | Version |
|------|---------|---------|
| **JUnit Jupiter** | Test framework | 5.10+ |
| **Mockito** | Mocking framework | 5.x |
| **AssertJ** | Fluent assertions | 3.24+ |
| **Spring Boot Test** | Integration testing support | 3.x |
| **MockMvc** | Controller testing | Included with Spring |
| **Testcontainers** | Docker containers for tests | 1.19+ |
| **RestAssured** | API testing | 5.x |
| **ArchUnit** | Architecture validation | 1.2+ |
| **JaCoCo** | Code coverage | 0.8.11+ |

### Frontend Testing (React)

| Tool | Purpose |
|------|---------|
| **Vitest** | Test runner (fast Vite-compatible alternative to Jest) |
| **React Testing Library** | Component testing |
| **MSW (Mock Service Worker)** | API mocking |
| **Playwright** | E2E testing |
| **Testing Library User Event** | User interaction simulation |

### CI/CD Integration

| Tool | Purpose |
|------|---------|
| **GitHub Actions** | CI pipeline |
| **SonarQube** | Code quality analysis |
| **Codecov** | Coverage reporting |
| **Dependabot** | Dependency updates |

---

## Test Folder Structure

### Backend Test Structure

```
backend/src/test/
├── java/
│   └── com/
│       └── medisure/
│           ├── unit/                           # Unit tests
│           │   ├── service/
│           │   │   ├── ClaimServiceTest.java
│           │   │   ├── DecisionServiceTest.java
│           │   │   ├── PolicyServiceTest.java
│           │   │   └── TreatmentServiceTest.java
│           │   ├── util/
│           │   │   ├── ValidationUtilsTest.java
│           │   │   └── FormatUtilsTest.java
│           │   └── agent/
│           │       ├── tools/
│           │       │   ├── PolicyRetrieverToolTest.java
│           │       │   ├── DrugSafetyToolTest.java
│           │       │   └── CoverageCalculatorToolTest.java
│           │       └── orchestrator/
│           │           └── AgentOrchestratorTest.java
│           ├── integration/                    # Integration tests
│           │   ├── api/
│           │   │   ├── ClaimControllerIntegrationTest.java
│           │   │   ├── PolicyControllerIntegrationTest.java
│           │   │   └── AuthControllerIntegrationTest.java
│           │   ├── repository/
│           │   │   ├── ClaimRepositoryTest.java
│           │   │   └── PolicyRepositoryTest.java
│           │   ├── security/
│           │   │   └── SecurityIntegrationTest.java
│           │   └── rag/
│           │       ├── RAGIngestionIntegrationTest.java
│           │       └── RAGRetrievalIntegrationTest.java
│           ├── e2e/                            # End-to-end tests
│           │   ├── ClaimWorkflowE2ETest.java
│           │   └── DecisionGenerationE2ETest.java
│           ├── performance/                    # Performance tests
│           │   ├── AgentPerformanceTest.java
│           │   └── RAGRetrievalPerformanceTest.java
│           ├── architecture/                   # Architecture tests
│           │   └── ArchitectureTest.java
│           └── fixtures/                       # Test data fixtures
│               ├── ClaimFixtures.java
│               ├── PolicyFixtures.java
│               └── UserFixtures.java
└── resources/
    ├── application-test.yml                    # Test configuration
    ├── test_policies/                          # Test documents
    │   ├── diabetes_policy.pdf
    │   └── cardiac_policy.pdf
    ├── expected_answers/                       # Ground truth data
    │   └── diabetes_queries.json
    └── data.sql                                # Test database seed data
```

### Frontend Test Structure

```
frontend/src/
├── components/
│   ├── claim/
│   │   ├── ClaimForm.jsx
│   │   └── ClaimForm.test.jsx               # Co-located test
│   └── decision/
│       ├── DecisionResult.jsx
│       └── DecisionResult.test.jsx
├── pages/
│   ├── patient/
│   │   ├── PatientDashboard.jsx
│   │   └── PatientDashboard.test.jsx
├── hooks/
│   ├── useClaim.js
│   └── useClaim.test.js
├── utils/
│   ├── validators.js
│   └── validators.test.js
├── store/
│   ├── claimStore.js
│   └── claimStore.test.js
├── api/
│   ├── claimApi.js
│   └── claimApi.test.js                     # Mock MSW
└── __tests__/
    ├── e2e/
    │   ├── claim-submission.spec.js         # Playwright E2E
    │   └── decision-display.spec.js
    └── setup/
        ├── setupTests.js                    # Global test setup
        └── mocks/
            ├── handlers.js                  # MSW handlers
            └── server.js                    # MSW server
```

### Test Naming Conventions

**Unit Tests**: `*Test.java` or `*.test.jsx`
**Integration Tests**: `*IntegrationTest.java`
**E2E Tests**: `*E2ETest.java` or `*.spec.js`
**Performance Tests**: `*PerformanceTest.java`

**Test Method Naming**: `methodUnderTest_StateUnderTest_ExpectedBehavior`

Examples:
- `submitClaim_WhenPolicyActive_ShouldCreatePendingClaim`
- `generateDecision_WithInsufficientData_ThrowsException`
- `retrieveChunks_DiabetesQuery_ReturnsRelevantResults`

---

## Continuous Integration

### GitHub Actions Workflow

```yaml
name: Backend Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_DB: medisure_test
          POSTGRES_USER: test_user
          POSTGRES_PASSWORD: test_password
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
      
      chromadb:
        image: chromadb/chroma:latest
        ports:
          - 8000:8000
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Cache Maven packages
        uses: actions/cache@v3
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
      
      - name: Run unit tests
        run: mvn test -Dtest=*Test
      
      - name: Run integration tests
        run: mvn test -Dtest=*IntegrationTest
        env:
          POSTGRES_HOST: postgres
          POSTGRES_DB: medisure_test
          CHROMA_HOST: localhost
          CHROMA_PORT: 8000
      
      - name: Generate coverage report
        run: mvn jacoco:report
      
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          file: ./target/site/jacoco/jacoco.xml
```

### Coverage Requirements

**Minimum Coverage Thresholds**:
- **Overall**: 80%
- **Service Layer**: 90%
- **Utility Classes**: 95%
- **Controllers**: 70%
- **Models/DTOs**: 60%

**JaCoCo Configuration** (`pom.xml`):
```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>0.8.11</version>
  <executions>
    <execution>
      <goals>
        <goal>prepare-agent</goal>
      </goals>
    </execution>
    <execution>
      <id>report</id>
      <phase>test</phase>
      <goals>
        <goal>report</goal>
      </goals>
    </execution>
    <execution>
      <id>check</id>
      <goals>
        <goal>check</goal>
      </goals>
      <configuration>
        <rules>
          <rule>
            <element>BUNDLE</element>
            <limits>
              <limit>
                <counter>LINE</counter>
                <value>COVEREDRATIO</value>
                <minimum>0.80</minimum>
              </limit>
            </limits>
          </rule>
        </rules>
      </configuration>
    </execution>
  </executions>
</plugin>
```

---

## Summary

**Testing Priorities**:
1. **Critical Path**: Claim submission → Decision generation → Approval/Rejection
2. **Security**: Authentication, authorization, role-based access
3. **RAG Quality**: Retrieval accuracy, embedding quality, citation correctness
4. **Agent Reliability**: Tool execution, reasoning quality, performance

**Definition of Done**:
- All unit tests pass
- Integration tests pass
- Code coverage ≥ 80%
- No critical SonarQube issues
- E2E tests for critical workflows pass
- Performance benchmarks met (decision < 5 seconds)

**Remember**: In healthcare AI, **correctness and explainability** are more important than speed. When in doubt, test more thoroughly.
