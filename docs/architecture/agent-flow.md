# Agent Flow Architecture

## What is an Agentic AI System?

An **Agentic AI system** is an artificial intelligence architecture that goes beyond simple prompt-response interactions by implementing autonomous reasoning, tool usage, and iterative problem-solving capabilities. Unlike traditional AI systems that provide immediate answers based on a single query, agentic systems can:

- **Decompose complex tasks** into smaller, manageable steps
- **Select and execute tools** dynamically based on the task requirements
- **Observe results** and adjust their strategy accordingly
- **Iterate and refine** their approach if initial attempts are insufficient
- **Maintain context** across multiple reasoning steps
- **Explain their reasoning** by providing transparent thought processes

In the context of MediSureAI, the agentic approach is essential because healthcare and insurance decisions require:
1. **Multi-source information gathering**: Policy documents, medical guidelines, drug databases, patient history
2. **Sequential reasoning**: First check eligibility, then validate treatment, then calculate coverage
3. **Conditional logic**: If drug interactions found, escalate warning; if coverage insufficient, suggest alternatives
4. **Transparency**: Every decision must be traceable to specific sources for regulatory compliance

Traditional single-shot LLM queries cannot handle this complexity reliably. An agent, however, can orchestrate multiple steps, verify results, and produce comprehensive, justified decisions.

---

## The Full 8-Step Agent Reasoning Loop

MediSureAI implements a sophisticated agent reasoning loop that processes healthcare decision requests through eight distinct phases:

```
┌─────────────────────────────────────────────────────────────────┐
│                    STEP 1: RECEIVE REQUEST                      │
│  Input: Claim ID + Decision Type + Patient Context             │
│  Example: claimId=12345, type=CLAIM_ELIGIBILITY                │
└──────────────────────────┬──────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────────┐
│                  STEP 2: INTENT ANALYSIS                        │
│  Question: What kind of decision does the user need?            │
│  Options: CLAIM_ELIGIBILITY | TREATMENT_VALIDATION |            │
│           DRUG_SAFETY | BILL_EXPLANATION | POLICY_COVERAGE      │
│  Output: Selected intent based on request type and context      │
└──────────────────────────┬──────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────────┐
│                   STEP 3: TOOL SELECTION                        │
│  Based on intent, agent determines which tools to invoke:       │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ CLAIM_ELIGIBILITY requires:                              │  │
│  │   - PolicyRetrieverTool (RAG search policy docs)         │  │
│  │   - ClaimValidationTool (check data completeness)        │  │
│  │   - CoverageCalculatorTool (compute payable amount)      │  │
│  │   - DrugSafetyTool (if medications present)              │  │
│  │                                                           │  │
│  │ TREATMENT_VALIDATION requires:                           │  │
│  │   - GuidelineValidatorTool (check clinical compliance)   │  │
│  │   - DrugSafetyTool (check drug interactions)             │  │
│  │   - CostEstimatorTool (estimate treatment cost)          │  │
│  │                                                           │  │
│  │ DRUG_SAFETY requires:                                    │  │
│  │   - DrugSafetyTool (comprehensive interaction check)     │  │
│  │   - PatientHistoryTool (check allergies, conditions)     │  │
│  └──────────────────────────────────────────────────────────┘  │
└──────────────────────────┬──────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────────┐
│              STEP 4: PARALLEL TOOL EXECUTION                    │
│  Execute selected tools concurrently where possible:            │
│                                                                 │
│  Thread 1: PolicyRetrieverTool                                  │
│    Input: "diabetes insulin therapy"                            │
│    Output: [Clause 4.2.1: Insulin co-pay 25%,                  │
│             Clause 4.3: Annual diabetes limit ₹100,000,         │
│             Clause 8.1: Network hospital 10% discount]          │
│                                                                 │
│  Thread 2: DrugSafetyTool                                       │
│    Input: ["Insulin Glargine", "Metformin"]                     │
│    Output: [No major interactions found,                        │
│             Warning: Monitor blood glucose closely]             │
│                                                                 │
│  Thread 3: CoverageCalculatorTool                               │
│    Input: treatmentCost=85000, policyId=POL-456                 │
│    Output: [baseCoverage=85000, coPay=21250 (25%),             │
│             networkDiscount=8500 (10%),                         │
│             payableAmount=55250]                                │
│                                                                 │
│  Thread 4: ClaimValidationTool                                  │
│    Input: claimData={patient, policy, treatment}                │
│    Output: [Valid: true, Completeness: 100%,                   │
│             Warnings: Annual limit 73% utilized]                │
└──────────────────────────┬──────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────────┐
│            STEP 5: OBSERVATION & VALIDATION                     │
│  Agent assesses tool results:                                   │
│                                                                 │
│  Quality Check:                                                 │
│  ✓ PolicyRetriever: 3 relevant clauses found (confidence >0.8) │
│  ✓ DrugSafetyTool: Complete interaction analysis performed     │
│  ✓ CoverageCalculator: All rules applied successfully          │
│  ✓ ClaimValidation: Data integrity confirmed                   │
│                                                                 │
│  Decision Point:                                                │
│  Is the retrieved context sufficient for a confident decision?  │
│                                                                 │
│  YES → Proceed to LLM synthesis                                 │
│  NO  → Re-query with refined parameters:                        │
│         - Try alternative search terms                          │
│         - Expand search to broader policy categories            │
│         - Query additional data sources                         │
│         Maximum 2 retry attempts to prevent infinite loops      │
└──────────────────────────┬──────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────────┐
│                  STEP 6: LLM SYNTHESIS                          │
│  Construct comprehensive prompt and send to LLM:                │
│                                                                 │
│  Prompt Structure:                                              │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │ SYSTEM: You are a healthcare insurance specialist.        │ │
│  │                                                            │ │
│  │ PATIENT CONTEXT:                                           │ │
│  │ - Age: 45, Diagnosis: Type 2 Diabetes                     │ │
│  │ - Treatment: Insulin therapy + dietary management         │ │
│  │ - Hospital: Apollo (Network), Cost: ₹85,000               │ │
│  │                                                            │ │
│  │ RETRIEVED POLICY CLAUSES:                                  │ │
│  │ [Clause 4.2.1]: Insulin therapy requires 25% co-pay       │ │
│  │ [Clause 4.3]: Annual diabetes coverage limit ₹100,000     │ │
│  │ [Clause 8.1]: Network hospital discount 10%               │ │
│  │                                                            │ │
│  │ TOOL RESULTS:                                              │ │
│  │ - Drug Safety: No major interactions, monitor glucose     │ │
│  │ - Coverage Calculation: Payable ₹55,250 after co-pay      │ │
│  │ - Validation: Claim data complete, 73% annual limit used  │ │
│  │                                                            │ │
│  │ TASK:                                                      │ │
│  │ Determine if this claim should be APPROVED, REJECTED,     │ │
│  │ or PARTIAL. Provide detailed reasoning with clause        │ │
│  │ citations, calculate final payable amount, list warnings, │ │
│  │ and suggest patient actions.                              │ │
│  │                                                            │ │
│  │ Output as JSON: {outcome, payableAmount, reasoning,       │ │
│  │                  citations, warnings, suggestedActions}   │ │
│  └───────────────────────────────────────────────────────────┘ │
│                                                                 │
│  LLM Response:                                                  │
│  {                                                              │
│    "outcome": "PARTIAL",                                        │
│    "payableAmount": 55250,                                      │
│    "reasoning": "The claim is eligible for partial coverage... │
│    "citations": [...],                                          │
│    "warnings": [...],                                           │
│    "suggestedActions": [...]                                    │
│  }                                                              │
└──────────────────────────┬──────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────────┐
│            STEP 7: EXPLANATION GENERATION                       │
│  Explainability Engine processes LLM output:                    │
│                                                                 │
│  1. Extract Decision Outcome                                    │
│     Raw: "PARTIAL"                                              │
│     Formatted: "Partially Approved"                             │
│                                                                 │
│  2. Map Citations to Sources                                    │
│     LLM mentions "Clause 4.2.1"                                 │
│     Engine retrieves: {source: "policy_doc.pdf", page: 12,     │
│                        text: "Insulin therapy co-pay 25%"}     │
│                                                                 │
│  3. Calculate Confidence Score                                  │
│     Factors: Retrieval quality (0.85), Tool success rate (1.0),│
│              LLM confidence (0.9), Data completeness (1.0)      │
│     Formula: (0.85 + 1.0 + 0.9 + 1.0) / 4 = 0.9375             │
│     Final Score: 93.75%                                         │
│                                                                 │
│  4. Extract Warnings                                            │
│     - Annual limit 73% utilized after this claim                │
│     - Monitor blood glucose levels during treatment             │
│                                                                 │
│  5. Generate Patient-Friendly Summary                           │
│     "Your claim for diabetes treatment has been partially       │
│     approved. The insurance will cover ₹55,250 out of the      │
│     total ₹85,000 cost. You are responsible for a 25% co-pay   │
│     of ₹21,250 as per your policy terms for insulin therapy.   │
│     Please note that you have used 73% of your annual diabetes  │
│     coverage limit."                                            │
│                                                                 │
│  6. Build Structured Response Object                            │
│     DecisionResponse {                                          │
│       decisionId, claimId, outcome, payableAmount,              │
│       confidenceScore, reasoning, citations, warnings,          │
│       suggestedActions, friendlySummary, createdAt              │
│     }                                                            │
└──────────────────────────┬──────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────────┐
│           STEP 8: RETURN STRUCTURED RESPONSE                    │
│  Final decision object returned to caller:                      │
│                                                                 │
│  {                                                              │
│    "decisionId": "dec-789",                                     │
│    "claimId": "clm-12345",                                      │
│    "outcome": "PARTIAL",                                        │
│    "payableAmount": 55250.00,                                   │
│    "confidenceScore": 93.75,                                    │
│    "reasoning": "The policy covers diabetes treatment...",     │
│    "citations": [                                               │
│      {                                                          │
│        "clause": "4.2.1",                                       │
│        "source": "policy_doc.pdf",                              │
│        "page": 12,                                              │
│        "text": "Insulin therapy co-pay: 25%"                   │
│      }                                                          │
│    ],                                                           │
│    "warnings": [                                                │
│      "Annual limit 73% utilized after this claim",             │
│      "Monitor blood glucose levels during treatment"           │
│    ],                                                           │
│    "suggestedActions": [                                        │
│      "Consider day-care admission to reduce room charges",     │
│      "Use pharmacy benefits for long-term medications"         │
│    ],                                                           │
│    "friendlySummary": "Your claim has been partially...",      │
│    "timestamp": "2026-02-19T14:30:00Z"                          │
│  }                                                              │
│                                                                 │
│  This response is:                                              │
│  - Persisted to PostgreSQL (decisions table)                   │
│  - Returned to ClaimService                                     │
│  - Sent to Frontend via REST API                                │
│  - Displayed in DecisionResult component                        │
└─────────────────────────────────────────────────────────────────┘
```

---

## How the Agent Selects Tools

Tool selection in MediSureAI follows a hybrid approach combining **rule-based logic** and **LLM-based dynamic selection**.

### Rule-Based Tool Selection

For predictable scenarios, the agent uses predefined rules:

| Decision Type | Always Required | Conditionally Required |
|---------------|-----------------|------------------------|
| CLAIM_ELIGIBILITY | PolicyRetriever, ClaimValidator, CoverageCalculator | DrugSafetyTool (if medications), GuidelineValidator (if procedures) |
| TREATMENT_VALIDATION | GuidelineValidator, DrugSafetyTool | CostEstimator (if cost analysis requested) |
| DRUG_SAFETY | DrugSafetyTool, PatientHistoryTool | PolicyRetriever (if coverage question) |
| BILL_EXPLANATION | BillingAnalyzer, PolicyRetriever | None |
| POLICY_COVERAGE | PolicyRetriever, CoverageAnalyzer | None |

### LLM-Based Dynamic Selection

For complex or ambiguous requests, the agent consults the LLM:

```
Agent: "Given this patient request: 'I want to know if my policy covers knee replacement surgery and what drugs I should avoid after the procedure.'

Available tools:
1. PolicyRetriever - Search policy documents
2. DrugSafetyTool - Check drug interactions
3. GuidelineValidator - Validate against clinical guidelines
4. CoverageCalculator - Calculate coverage amounts
5. BillingAnalyzer - Explain bill components

Which tools should be invoked and in what order?"

LLM: "This request has two parts:
1. Coverage inquiry → PolicyRetriever, CoverageCalculator
2. Drug safety inquiry → DrugSafetyTool, PatientHistoryTool

Execution order:
Step 1: PolicyRetriever (search for knee replacement coverage)
Step 2: CoverageCalculator (compute payable amount)
Step 3: PatientHistoryTool (fetch patient's medication history)
Step 4: DrugSafetyTool (check post-surgery drug interactions)

Parallel execution possible: Steps 1-2 parallel, then Steps 3-4 parallel."
```

### Tool Priority Matrix

When resource constraints exist, tools are prioritized:

| Priority | Tool | Reason |
|----------|------|--------|
| CRITICAL | PolicyRetriever | Cannot make decisions without policy context |
| CRITICAL | ClaimValidator | Data integrity must be verified |
| HIGH | DrugSafetyTool | Patient safety is paramount |
| HIGH | GuidelineValidator | Clinical compliance required |
| MEDIUM | CoverageCalculator | Important but can use fallback estimates |
| LOW | BillingAnalyzer | Nice-to-have, not decision-critical |

---

## Handling Insufficient Retrieval (Retry Logic)

The agent employs a sophisticated retry mechanism when initial tool executions yield insufficient results:

### Insufficiency Detection

The agent considers retrieval insufficient if:
- **Low similarity scores**: Top result has cosine similarity < 0.75
- **Ambiguous matches**: Top 3 results have similar scores but contradictory content
- **Missing context**: LLM indicates it cannot make a confident decision
- **Data gaps**: Critical fields in tool results are null or empty

### Retry Strategy

```
Attempt 1: Initial query with original terms
│
├─ Success (similarity > 0.75, clear match) → Proceed
│
└─ Insufficient → Attempt 2: Query refinement
   │
   ├─ Strategy A: Expand search terms
   │   Original: "diabetes insulin coverage"
   │   Refined: "diabetes insulin therapy treatment reimbursement"
   │
   ├─ Strategy B: Broaden category filter
   │   Original: category="DIABETES"
   │   Refined: category="CHRONIC_DISEASE"
   │
   ├─ Strategy C: Increase top-K results
   │   Original: top_k=5
   │   Refined: top_k=10
   │
   └─ Strategy D: Hybrid search (semantic + keyword)
       Original: Semantic only
       Refined: Semantic + exact keyword matching
   │
   ├─ Success → Proceed
   │
   └─ Still Insufficient → Attempt 3: Fallback to general rules
      │
      ├─ Use default policy provisions
      ├─ Apply industry-standard coverage rules
      ├─ Flag decision as LOW CONFIDENCE
      ├─ Suggest manual review
      │
      └─ Return decision with confidence score < 70%

Maximum 2 automatic retries to prevent infinite loops.
After 2 retries, escalate to manual review queue.
```

### Example Retry Scenario

**Initial Query**: "coverage for heart stent procedure"

**Attempt 1 Results**:
- Top match: "Cardiac catheterization coverage" (similarity: 0.68 - below threshold)
- Agent detects insufficiency

**Attempt 2 Refinement**:
- Expanded query: "cardiac angioplasty stent coronary intervention coverage"
- Top match: "Percutaneous coronary intervention with stent" (similarity: 0.89 - sufficient)
- Proceed with decision

**Total latency impact**: +800ms for second retrieval

---

## Tool Catalog

### 1. PolicyRetrieverTool

**Purpose**: Retrieve relevant policy clauses using RAG semantic search

**Inputs**:
- query: Natural language description of coverage question
- policyId: Specific policy to search (optional, defaults to patient's active policy)
- topK: Number of results to return (default: 5)
- minSimilarity: Minimum cosine similarity threshold (default: 0.75)

**Outputs**:
- List of PolicyClause objects containing: clause number, text, similarity score, source document, page number

**Example**:
```
Input: query="insulin therapy coverage", policyId="POL-456", topK=5
Output: [
  {clause: "4.2.1", text: "Insulin therapy co-pay 25%", score: 0.89, source: "policy.pdf", page: 12},
  {clause: "4.3", text: "Annual diabetes limit ₹100,000", score: 0.85, source: "policy.pdf", page: 13}
]
```

---

### 2. DrugSafetyTool

**Purpose**: Check drug interactions, contraindications, and side effects

**Inputs**:
- medications: List of drug names with dosages
- patientConditions: List of patient's medical conditions
- patientAge: Patient age (affects drug metabolism)
- patientAllergies: Known drug allergies

**Outputs**:
- interactions: List of drug-drug interactions with severity level
- contraindications: Drugs contraindicated given patient conditions
- warnings: General safety warnings and monitoring recommendations

**Example**:
```
Input: medications=["Metformin 500mg", "Aspirin 75mg"], patientConditions=["Type 2 Diabetes", "Hypertension"]
Output: {
  interactions: [{drug1: "Metformin", drug2: "Aspirin", severity: "MINOR", description: "May increase bleeding risk"}],
  contraindications: [],
  warnings: ["Monitor kidney function regularly with Metformin", "Check blood glucose before and after meals"]
}
```

---

### 3. GuidelineValidatorTool

**Purpose**: Validate treatment plans against clinical best practices and medical guidelines

**Inputs**:
- diagnosis: Primary diagnosis code or description
- treatmentPlan: Proposed treatment approach
- procedures: List of planned procedures
- medications: Prescribed medications

**Outputs**:
- compliant: Boolean indicating overall compliance
- deviations: List of deviations from guidelines with severity
- recommendations: Suggested modifications to align with guidelines
- guidelineReferences: Citations to specific clinical guidelines

**Example**:
```
Input: diagnosis="Type 2 Diabetes", treatmentPlan="Insulin therapy", medications=["Insulin Glargine"]
Output: {
  compliant: true,
  deviations: [],
  recommendations: ["Consider lifestyle modification counseling", "Schedule diabetes education session"],
  guidelineReferences: ["ADA 2025 Standards of Care", "IDF Global Guideline"]
}
```

---

### 4. CoverageCalculatorTool

**Purpose**: Apply policy rules to calculate exact payable amounts

**Inputs**:
- policyId: Patient's insurance policy ID
- treatmentCost: Total estimated treatment cost
- hospitalType: NETWORK / NON_NETWORK / DAY_CARE
- treatmentCategory: Type of treatment (affects co-pay percentages)
- roomType: Room category if hospitalization involved

**Outputs**:
- baseCoverage: Amount before deductions
- coPay: Patient co-payment amount
- deductibles: Applicable deductibles
- discounts: Network or other discounts
- payableAmount: Final amount insurance will pay
- patientResponsibility: Amount patient must pay

**Example**:
```
Input: policyId="POL-456", treatmentCost=85000, hospitalType="NETWORK", treatmentCategory="DIABETES"
Output: {
  baseCoverage: 85000,
  coPay: 21250 (25%),
  deductibles: 0,
  discounts: 8500 (10% network discount),
  payableAmount: 55250,
  patientResponsibility: 29750
}
```

---

### 5. ClaimValidationTool

**Purpose**: Verify claim data completeness and business rule compliance

**Inputs**:
- claimData: Complete claim object with patient, policy, treatment details

**Outputs**:
- valid: Boolean indicating if claim can be processed
- completenessScore: Percentage of required fields populated
- errors: List of errors that must be fixed
- warnings: List of warnings that should be reviewed
- missingFields: List of missing required or recommended fields

**Example**:
```
Input: claimData={patientId, policyId, treatmentId, cost=85000, date="2026-02-19"}
Output: {
  valid: true,
  completenessScore: 95,
  errors: [],
  warnings: ["Annual limit 73% utilized", "Policy expires in 60 days"],
  missingFields: ["hospitalBillNumber"]
}
```

---

### 6. BillingAnalyzerTool

**Purpose**: Break down hospital bills into understandable components

**Inputs**:
- billDocument: Scanned or structured bill data
- policyId: Patient's policy for coverage mapping

**Outputs**:
- lineItems: List of bill line items with descriptions
- categoryBreakdown: Costs grouped by category (room, doctors, pharmacy, etc.)
- coveredItems: Items covered by insurance
- nonCoveredItems: Items patient must pay
- explanations: Human-readable explanation for each charge

---

## Example Walkthrough: Patient Submits Diabetes Claim

Let's trace a complete agent execution flow:

### Initial Request

**Patient Action**: Alice, 45 years old, submits a claim for diabetes treatment through the Patient Portal

**Claim Data**:
- Patient: Alice Kumar (age 45)
- Diagnosis: Type 2 Diabetes Mellitus
- Treatment Plan: Insulin therapy + dietary management + blood glucose monitoring
- Medications: Insulin Glargine 50 units/day, Metformin 500mg twice daily
- Hospital: Apollo Hospital Mumbai (Network)
- Estimated Cost: ₹85,000
- Duration: 5 days admission
- Policy: Premium Health Plus (POL-456)

---

### Step 1: Receive Request

Agent receives DecisionRequest:
```
{
  "claimId": "clm-12345",
  "decisionType": "CLAIM_ELIGIBILITY",
  "patientId": "pat-789",
  "policyId": "POL-456",
  "treatmentId": "tmt-456",
  "context": {
    "diagnosis": "Type 2 Diabetes",
    "medications": ["Insulin Glargine", "Metformin"],
    "hospitalType": "NETWORK",
    "estimatedCost": 85000
  }
}
```

---

### Step 2: Intent Analysis

Agent analyzes request:
- Decision type: CLAIM_ELIGIBILITY
- Key entities: diabetes, insulin, network hospital
- Required analysis: Coverage eligibility + drug safety + cost calculation

---

### Step 3: Tool Selection

Agent selects tools based on decision type and context:
1. **PolicyRetrieverTool** - Must retrieve diabetes coverage clauses
2. **DrugSafetyTool** - Medications present, must check interactions
3. **CoverageCalculatorTool** - Must calculate exact payable amount
4. **ClaimValidationTool** - Must verify data integrity

---

### Step 4: Parallel Tool Execution

**Thread 1: PolicyRetrieverTool**
```
Input: query="Type 2 Diabetes insulin therapy coverage network hospital", policyId="POL-456"
Processing: 
  - Generate query embedding
  - Search ChromaDB policy collection
  - Retrieve top 5 matches
Output: [
  {clause: "4.2.1", text: "Diabetes management including insulin therapy is covered with 25% co-payment", similarity: 0.89},
  {clause: "4.3", text: "Annual limit for diabetes-related treatments: ₹100,000", similarity: 0.87},
  {clause: "8.1", text: "Network hospital admissions receive 10% discount on total bill", similarity: 0.82},
  {clause: "6.5", text: "Blood glucose monitoring supplies covered up to ₹5,000 annually", similarity: 0.79},
  {clause: "4.2.3", text: "Diabetes complications covered under separate sub-limit", similarity: 0.76}
]
```

**Thread 2: DrugSafetyTool**
```
Input: medications=["Insulin Glargine", "Metformin"], patientAge=45, conditions=["Type 2 Diabetes"]
Processing:
  - Query drug interaction database
  - Check contraindications for each drug
  - Assess interaction severity
Output: {
  interactions: [{drug1: "Insulin", drug2: "Metformin", severity: "MINOR", description: "Combined use may enhance glucose-lowering effect, monitor blood sugar closely"}],
  contraindications: [],
  warnings: ["Monitor blood glucose levels 4 times daily", "Watch for hypoglycemia symptoms", "Adjust insulin dosage based on blood sugar readings"],
  riskLevel: "LOW"
}
```

**Thread 3: CoverageCalculatorTool**
```
Input: policyId="POL-456", treatmentCost=85000, hospitalType="NETWORK", category="DIABETES"
Processing:
  - Fetch policy rules from database
  - Apply base coverage: 85000
  - Apply co-pay rule (Clause 4.2.1): 25% = 21250
  - Apply network discount (Clause 8.1): 10% of (85000-21250) = 6375
  - Calculate payable: 85000 - 21250 - 6375 = 57375
Output: {
  baseCoverage: 85000,
  coPay: 21250,
  networkDiscount: 6375,
  payableAmount: 57375,
  patientResponsibility: 27625,
  annualLimitRemaining: 42625 (100000 - 57375)
}
```

**Thread 4: ClaimValidationTool**
```
Input: claimData={patient, policy, treatment, cost}
Processing:
  - Verify all required fields present
  - Check policy active and valid
  - Verify patient eligibility
  - Check annual limits
Output: {
  valid: true,
  completenessScore: 100,
  errors: [],
  warnings: ["Annual diabetes limit will be 57% utilized after this claim"],
  missingFields: []
}
```

---

### Step 5: Observation & Validation

Agent assesses tool results:
- PolicyRetriever: 5 clauses found, top match 0.89 similarity ✓ HIGH QUALITY
- DrugSafetyTool: Complete analysis, low risk ✓ SUFFICIENT
- CoverageCalculator: All rules applied ✓ COMPLETE
- ClaimValidation: No errors ✓ VALID

**Decision**: Context is sufficient, proceed to LLM synthesis

---

### Step 6: LLM Synthesis

Agent constructs prompt and sends to GPT-4:

```
SYSTEM: You are an expert healthcare insurance decision specialist. Analyze the following information and provide a structured decision.

PATIENT CONTEXT:
- Name: Alice Kumar
- Age: 45 years
- Diagnosis: Type 2 Diabetes Mellitus
- Treatment: Insulin therapy, dietary management, blood glucose monitoring
- Medications: Insulin Glargine 50 units/day, Metformin 500mg twice daily
- Hospital: Apollo Hospital Mumbai (Network Hospital)
- Duration: 5 days
- Total Cost: ₹85,000

POLICY INFORMATION:
Policy Number: POL-456 (Premium Health Plus)
Active: Yes, Expires: 2026-12-31

RETRIEVED POLICY CLAUSES:
[Clause 4.2.1]: "Diabetes management including insulin therapy is covered with 25% co-payment"
[Clause 4.3]: "Annual limit for diabetes-related treatments: ₹100,000"
[Clause 8.1]: "Network hospital admissions receive 10% discount on total bill"
[Clause 6.5]: "Blood glucose monitoring supplies covered up to ₹5,000 annually"

DRUG SAFETY ANALYSIS:
- Insulin + Metformin: Minor interaction, recommend close glucose monitoring
- Risk Level: LOW
- Contraindications: None
- Warnings: Monitor blood glucose 4x daily, watch for hypoglycemia

COVERAGE CALCULATION:
- Base Coverage: ₹85,000
- Co-pay (25%): ₹21,250
- Network Discount (10%): ₹6,375
- Insurance Payable: ₹57,375
- Patient Responsibility: ₹27,625
- Annual Limit Remaining: ₹42,625

CLAIM VALIDATION:
- Valid: Yes
- Completeness: 100%
- Warning: 57% of annual diabetes limit will be utilized

TASK:
Provide a comprehensive decision on this claim. Output structured JSON with:
1. outcome: APPROVED / REJECTED / PARTIAL
2. payableAmount: Exact amount insurance will pay
3. reasoning: Detailed explanation referencing specific clauses
4. citations: List relevant policy clauses
5. warnings: Any important warnings for patient
6. suggestedActions: Recommendations for patient
```

**LLM Response**:
```
{
  "outcome": "APPROVED",
  "payableAmount": 57375,
  "reasoning": "The claim for Type 2 Diabetes treatment at Apollo Hospital Mumbai is APPROVED. According to Clause 4.2.1 of your Premium Health Plus policy, diabetes management including insulin therapy is covered with a 25% co-payment, resulting in ₹21,250 patient responsibility. As this is a network hospital admission, Clause 8.1 grants an additional 10% discount of ₹6,375 on the payable amount. The total cost of ₹85,000 is within your annual diabetes treatment limit of ₹100,000 (Clause 4.3). Drug safety analysis shows minor interaction between prescribed medications requiring glucose monitoring but no contraindications. The insurance will pay ₹57,375, and you are responsible for ₹27,625.",
  "citations": [
    {"clause": "4.2.1", "text": "Diabetes management including insulin therapy is covered with 25% co-payment"},
    {"clause": "8.1", "text": "Network hospital admissions receive 10% discount"},
    {"clause": "4.3", "text": "Annual limit for diabetes treatments: ₹100,000"}
  ],
  "warnings": [
    "After this claim, you will have used 57% of your annual diabetes coverage limit (₹42,625 remaining)",
    "Monitor blood glucose levels 4 times daily while on combined insulin and metformin therapy",
    "Watch for signs of hypoglycemia (low blood sugar): dizziness, sweating, confusion"
  ],
  "suggestedActions": [
    "Keep all medical bills and prescriptions for records",
    "Schedule follow-up appointment with endocrinologist within 2 weeks",
    "Consider using hospital day-care facilities for future consultations to save on room charges",
    "Utilize blood glucose monitoring supplies allowance (₹5,000 annually under Clause 6.5)"
  ]
}
```

---

### Step 7: Explanation Generation

Explainability Engine processes LLM output:

**Extract Decision**: APPROVED

**Map Citations**:
- Clause 4.2.1 → {source: "Premium_Health_Plus_Policy.pdf", page: 12, category: "Diabetes Coverage"}
- Clause 8.1 → {source: "Premium_Health_Plus_Policy.pdf", page: 24, category: "Network Benefits"}
- Clause 4.3 → {source: "Premium_Health_Plus_Policy.pdf", page: 13, category: "Coverage Limits"}

**Calculate Confidence Score**:
- Retrieval Quality: 0.89 (top match similarity)
- Tool Success Rate: 1.0 (all tools executed successfully)
- LLM Confidence: 0.95 (explicit decision, comprehensive reasoning)
- Data Completeness: 1.0 (100% completeness score)
- **Final Score**: (0.89 + 1.0 + 0.95 + 1.0) / 4 = **94.6%**

**Generate Patient-Friendly Summary**:
"Great news! Your claim for diabetes treatment has been approved. Your insurance will cover ₹57,375 of the ₹85,000 total cost. You'll need to pay ₹27,625, which includes your 25% co-payment minus a 10% network hospital discount. Please note that this uses 57% of your annual diabetes coverage limit, leaving ₹42,625 for the rest of the year."

---

### Step 8: Return Structured Response

Final DecisionResponse object:
```
{
  "decisionId": "dec-987",
  "claimId": "clm-12345",
  "outcome": "APPROVED",
  "payableAmount": 57375.00,
  "patientResponsibility": 27625.00,
  "confidenceScore": 94.6,
  "reasoning": "The claim for Type 2 Diabetes treatment at Apollo Hospital Mumbai is APPROVED...",
  "citations": [
    {
      "clause": "4.2.1",
      "source": "Premium_Health_Plus_Policy.pdf",
      "page": 12,
      "text": "Diabetes management including insulin therapy is covered with 25% co-payment",
      "category": "Diabetes Coverage"
    },
    {
      "clause": "8.1",
      "source": "Premium_Health_Plus_Policy.pdf",
      "page": 24,
      "text": "Network hospital admissions receive 10% discount",
      "category": "Network Benefits"
    },
    {
      "clause": "4.3",
      "source": "Premium_Health_Plus_Policy.pdf",
      "page": 13,
      "text": "Annual limit for diabetes treatments: ₹100,000",
      "category": "Coverage Limits"
    }
  ],
  "warnings": [
    "After this claim, 57% of annual diabetes limit utilized (₹42,625 remaining)",
    "Monitor blood glucose 4x daily with combined insulin-metformin therapy",
    "Watch for hypoglycemia symptoms"
  ],
  "suggestedActions": [
    "Keep all medical bills and prescriptions",
    "Schedule endocrinologist follow-up within 2 weeks",
    "Consider day-care facilities for future consultations",
    "Utilize glucose monitoring supplies allowance (₹5,000/year)"
  ],
  "friendlySummary": "Great news! Your claim for diabetes treatment has been approved...",
  "processingTime": 2847,
  "timestamp": "2026-02-19T14:30:00Z"
}
```

**Persistence**: Saved to decisions table in PostgreSQL
**API Response**: Returned to frontend with HTTP 200
**UI Display**: Rendered in DecisionResult component with expandable sections

**Total Processing Time**: 2.8 seconds

---

## Benefits of the Agentic Approach

1. **Comprehensive Analysis**: Multi-tool orchestration ensures all aspects are considered
2. **Adaptability**: Agent can adjust strategy based on intermediate results
3. **Transparency**: Every step is traceable and explainable
4. **Accuracy**: Retry logic handles edge cases and ambiguous queries
5. **Efficiency**: Parallel tool execution minimizes latency
6. **Scalability**: New tools can be added without changing core logic
7. **Auditability**: Complete reasoning trace for regulatory compliance

This agentic architecture makes MediSureAI far more capable than traditional rule-based systems or simple LLM queries, providing the intelligence and flexibility required for real-world healthcare decision support.
