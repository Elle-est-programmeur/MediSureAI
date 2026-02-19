# API Reference

## API Overview

MediSureAI exposes a RESTful API for all frontend-backend communication. The API follows REST principles with:

- **Resource-based URLs**: Endpoints represent resources (claims, policies, treatments)
- **HTTP methods**: GET (retrieve), POST (create), PUT (update), DELETE (remove)
- **JSON payloads**: All request/response bodies use JSON format
- **Stateless**: Each request includes authentication token, no server-side sessions
- **HATEOAS-inspired**: Responses include relevant resource links where appropriate

---

## Base URL Convention

All API endpoints are prefixed with the base URL and API version:

**Development**: http://localhost:8080/api/v1
**Production**: https://api.medisure.ai/api/v1

Example full endpoint: http://localhost:8080/api/v1/claims/submit

---

## Authentication Method

MediSureAI uses **JWT (JSON Web Token)** based authentication with Bearer token scheme.

### Authentication Flow

**Step 1**: User logs in with credentials
```
POST /api/v1/auth/login
Body: { "email": "alice@example.com", "password": "SecurePass123" }
Response: { "token": "eyJhbGciOiJSUzI1...", "refreshToken": "...", "user": {...} }
```

**Step 2**: Store token on client (localStorage or secure httpOnly cookie)

**Step 3**: Include token in Authorization header for all subsequent requests
```
GET /api/v1/claims/patient/550e8400...
Headers: 
  Authorization: Bearer eyJhbGciOiJSUzI1...
  Content-Type: application/json
```

**Step 4**: Token expires after 15 minutes. Use refresh token to get new access token
```
POST /api/v1/auth/refresh
Body: { "refreshToken": "..." }
Response: { "token": "eyJhbGciOiJSUzI1..." }
```

### Token Structure

JWT token contains:
- **Header**: Algorithm (RS256), token type (JWT)
- **Payload**: User ID, email, role, expiration time
- **Signature**: Signed with server's private key

Token payload example:
```
{
  "sub": "550e8400-e29b-41d4-a716-446655440001",
  "email": "alice@example.com",
  "role": "PATIENT",
  "iat": 1708356000,
  "exp": 1708356900
}
```

### Authentication Errors

**401 Unauthorized**: Token missing, invalid, or expired
```
{
  "timestamp": "2026-02-19T14:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "JWT token has expired",
  "path": "/api/v1/claims/submit"
}
```

**403 Forbidden**: Token valid but user lacks required role
```
{
  "timestamp": "2026-02-19T14:30:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Requires ADMIN role",
  "path": "/api/v1/admin/claims"
}
```

---

## Auth Endpoints

### Register New User

**Endpoint**: POST /api/v1/auth/register

**Purpose**: Create new user account with role assignment

**Who calls it**: Public (unauthenticated), self-registration for patients; admin-initiated for doctors

**Request Body**:
```
{
  "name": "Alice Kumar",
  "email": "alice@example.com",
  "password": "SecurePass123",
  "role": "PATIENT",
  "phoneNumber": "+91-9876543210",
  "dateOfBirth": "1980-05-15",
  "specialization": null,
  "licenseNumber": null
}
```

**Request Fields**:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| name | string | Yes | Full name of user |
| email | string | Yes | Email address (must be unique) |
| password | string | Yes | Password (min 8 chars, must include uppercase, lowercase, number) |
| role | string | Yes | One of: PATIENT, DOCTOR, ADMIN |
| phoneNumber | string | No | Contact phone number |
| dateOfBirth | string (ISO date) | No | Required for PATIENT role |
| specialization | string | No | Required for DOCTOR role |
| licenseNumber | string | No | Required for DOCTOR role |

**Response** (201 Created):
```
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "token": "eyJhbGciOiJSUzI1...",
    "refreshToken": "refresh_token_here",
    "user": {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "name": "Alice Kumar",
      "email": "alice@example.com",
      "role": "PATIENT"
    }
  }
}
```

**Validation Errors** (400 Bad Request):
```
{
  "status": 400,
  "error": "Bad Request",
  "message": "Email already registered",
  "path": "/api/v1/auth/register"
}
```

---

### Login

**Endpoint**: POST /api/v1/auth/login

**Purpose**: Authenticate user and receive JWT token

**Who calls it**: All users (unauthenticated)

**Request Body**:
```
{
  "email": "alice@example.com",
  "password": "SecurePass123"
}
```

**Response** (200 OK):
```
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 900,
    "user": {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "name": "Alice Kumar",
      "email": "alice@example.com",
      "role": "PATIENT",
      "phoneNumber": "+91-9876543210"
    }
  }
}
```

**Authentication Errors** (401 Unauthorized):
```
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid email or password"
}
```

---

### Refresh Token

**Endpoint**: POST /api/v1/auth/refresh

**Purpose**: Get new access token using refresh token

**Who calls it**: All authenticated users (when access token expires)

**Request Body**:
```
{
  "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response** (200 OK):
```
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 900
  }
}
```

---

### Get Current User

**Endpoint**: GET /api/v1/auth/me

**Purpose**: Retrieve current logged-in user's profile

**Who calls it**: All authenticated users

**Headers**: Authorization: Bearer {token}

**Response** (200 OK):
```
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "name": "Alice Kumar",
    "email": "alice@example.com",
    "role": "PATIENT",
    "phoneNumber": "+91-9876543210",
    "dateOfBirth": "1980-05-15",
    "createdAt": "2026-01-15T10:30:00Z"
  }
}
```

---

## Claim Endpoints

### Submit New Claim

**Endpoint**: POST /api/v1/claims/submit

**Purpose**: Submit insurance claim for treatment reimbursement

**Who calls it**: PATIENT role only

**Request Body**:
```
{
  "policyId": "661e8400-e29b-41d4-a716-446655440002",
  "treatmentId": "772e8400-e29b-41d4-a716-446655440003",
  "claimAmount": 87500.00,
  "supportingDocuments": [
    "https://storage.example.com/bills/bill_12345.pdf",
    "https://storage.example.com/prescriptions/prescription_456.pdf"
  ]
}
```

**Request Fields**:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| policyId | UUID | Yes | ID of policy under which claim is made |
| treatmentId | UUID | Yes | ID of treatment for which reimbursement is sought |
| claimAmount | decimal | Yes | Total amount being claimed |
| supportingDocuments | string[] | No | URLs to uploaded supporting documents |

**Response** (201 Created):
```
{
  "success": true,
  "message": "Claim submitted successfully. AI decision will be generated shortly.",
  "data": {
    "claimId": "994e8400-e29b-41d4-a716-446655440005",
    "claimNumber": "CLM-20260220-0001",
    "status": "PENDING",
    "claimAmount": 87500.00,
    "submittedAt": "2026-02-20T17:00:00Z",
    "estimatedProcessingTime": "2-5 seconds"
  }
}
```

**Workflow**: Claim created with PENDING status → Agent orchestrator triggered asynchronously → Decision generated → Claim status updated → Patient notified

---

### Get Claim by ID

**Endpoint**: GET /api/v1/claims/:claimId

**Purpose**: Retrieve detailed claim information with related data

**Who calls it**: PATIENT (own claims), DOCTOR (related treatments), ADMIN (all claims)

**Path Parameters**: claimId (UUID)

**Response** (200 OK):
```
{
  "success": true,
  "data": {
    "claimId": "994e8400-e29b-41d4-a716-446655440005",
    "claimNumber": "CLM-20260220-0001",
    "status": "APPROVED",
    "claimAmount": 87500.00,
    "approvedAmount": 57375.00,
    "submittedAt": "2026-02-20T17:00:00Z",
    "processedAt": "2026-02-20T17:03:25Z",
    "policy": {
      "policyId": "661e8400-e29b-41d4-a716-446655440002",
      "policyNumber": "POL-2026-12345",
      "providerName": "Max Bupa Health Insurance",
      "planName": "Premium Health Plus"
    },
    "treatment": {
      "treatmentId": "772e8400-e29b-41d4-a716-446655440003",
      "treatmentNumber": "TRT-20260219-0001",
      "diagnosis": "E11.9 - Type 2 Diabetes Mellitus",
      "hospitalName": "Apollo Hospital Mumbai",
      "estimatedCost": 85000.00
    },
    "latestDecision": {
      "decisionId": "aa5e8400-e29b-41d4-a716-446655440006",
      "outcome": "APPROVED",
      "confidenceScore": 94.6,
      "reasoning": "The claim for Type 2 Diabetes treatment...",
      "payableAmount": 57375.00,
      "patientResponsibility": 30125.00
    }
  }
}
```

---

### List Claims for Patient

**Endpoint**: GET /api/v1/claims/patient/:patientId

**Purpose**: Retrieve all claims submitted by a patient

**Who calls it**: PATIENT (own claims only), ADMIN (any patient)

**Path Parameters**: patientId (UUID)

**Query Parameters**:
- status (optional): Filter by claim status (PENDING, APPROVED, REJECTED, PARTIAL)
- fromDate (optional): Start date filter (ISO 8601 format)
- toDate (optional): End date filter
- page (optional): Page number (default: 0)
- size (optional): Page size (default: 20)

**Example**: GET /api/v1/claims/patient/550e8400.../api/v1/claims/patient/550e8400...?status=APPROVED&page=0&size=10

**Response** (200 OK):
```
{
  "success": true,
  "data": {
    "claims": [
      {
        "claimId": "994e8400-e29b-41d4-a716-446655440005",
        "claimNumber": "CLM-20260220-0001",
        "status": "APPROVED",
        "claimAmount": 87500.00,
        "approvedAmount": 57375.00,
        "submittedAt": "2026-02-20T17:00:00Z",
        "treatmentDiagnosis": "Type 2 Diabetes Mellitus"
      },
      {
        "claimId": "885e8400-e29b-41d4-a716-446655440012",
        "claimNumber": "CLM-20260115-0002",
        "status": "APPROVED",
        "claimAmount": 45000.00,
        "approvedAmount": 40500.00,
        "submittedAt": "2026-01-15T10:00:00Z",
        "treatmentDiagnosis": "Essential Hypertension"
      }
    ],
    "pagination": {
      "page": 0,
      "size": 10,
      "totalElements": 2,
      "totalPages": 1
    }
  }
}
```

---

### Update Claim Status

**Endpoint**: PUT /api/v1/claims/:claimId/status

**Purpose**: Manually update claim status (admin override)

**Who calls it**: ADMIN role only

**Path Parameters**: claimId (UUID)

**Request Body**:
```
{
  "status": "APPROVED",
  "reason": "Medical necessity confirmed by external review"
}
```

**Response** (200 OK):
```
{
  "success": true,
  "message": "Claim status updated successfully",
  "data": {
    "claimId": "994e8400-e29b-41d4-a716-446655440005",
    "status": "APPROVED",
    "updatedAt": "2026-02-21T10:15:00Z"
  }
}
```

---

## Treatment Endpoints

### Create Treatment Record

**Endpoint**: POST /api/v1/treatments/create

**Purpose**: Record new treatment provided to patient

**Who calls it**: DOCTOR role only

**Request Body**:
```
{
  "patientId": "550e8400-e29b-41d4-a716-446655440001",
  "diagnosis": "E11.9 - Type 2 Diabetes Mellitus without complications",
  "secondaryDiagnoses": [
    {"code": "I10", "description": "Essential hypertension"}
  ],
  "treatmentPlan": "Insulin therapy initiation with dietary counseling",
  "medications": [
    {
      "name": "Insulin Glargine",
      "dosage": "50 units",
      "frequency": "once daily",
      "duration": "ongoing"
    },
    {
      "name": "Metformin",
      "dosage": "500mg",
      "frequency": "twice daily",
      "duration": "ongoing"
    }
  ],
  "procedures": [
    {
      "code": "CPT-82947",
      "name": "Glucose blood test",
      "quantity": 4
    }
  ],
  "hospitalName": "Apollo Hospital Mumbai",
  "hospitalType": "NETWORK",
  "admissionDate": "2026-02-15",
  "dischargeDate": "2026-02-20",
  "estimatedCost": 85000.00
}
```

**Response** (201 Created):
```
{
  "success": true,
  "message": "Treatment record created successfully",
  "data": {
    "treatmentId": "772e8400-e29b-41d4-a716-446655440003",
    "treatmentNumber": "TRT-20260219-0001",
    "status": "COMPLETED",
    "createdAt": "2026-02-15T08:30:00Z"
  }
}
```

---

### Validate Treatment Plan

**Endpoint**: POST /api/v1/treatments/validate

**Purpose**: Validate treatment plan against clinical guidelines using AI

**Who calls it**: DOCTOR role

**Request Body**:
```
{
  "diagnosis": "Type 2 Diabetes Mellitus",
  "medications": [
    {"name": "Insulin Glargine", "dosage": "50 units", "frequency": "once daily"},
    {"name": "Metformin", "dosage": "500mg", "frequency": "twice daily"}
  ],
  "procedures": [
    {"name": "Blood glucose monitoring", "frequency": "4 times daily"}
  ],
  "patientAge": 45,
  "patientConditions": ["Type 2 Diabetes", "Hypertension"]
}
```

**Response** (200 OK):
```
{
  "success": true,
  "data": {
    "compliant": true,
    "overallStatus": "COMPLIANT",
    "deviations": [],
    "drugWarnings": [
      {
        "severity": "MINOR",
        "message": "Combined use of insulin and metformin may enhance glucose-lowering effect",
        "recommendation": "Monitor blood glucose levels closely"
      }
    ],
    "recommendations": [
      "Consider lifestyle modification counseling",
      "Schedule diabetes education session",
      "Recommend retinal examination annually"
    ],
    "guidelineReferences": [
      {
        "guideline": "ADA 2025 Standards of Care in Diabetes",
        "section": "9.1 Pharmacologic Approaches",
        "recommendation": "Metformin is the preferred initial pharmacologic agent for Type 2 Diabetes",
        "evidenceLevel": "A"
      },
      {
        "guideline": "ADA 2025 Standards of Care in Diabetes",
        "section": "9.3 Insulin Therapy",
        "recommendation": "Basal insulin (such as Insulin Glargine) recommended when oral agents insufficient",
        "evidenceLevel": "A"
      }
    ],
    "processingTime": "1.8 seconds"
  }
}
```

**Use Case**: Doctor enters treatment plan → Clicks "Validate" → AI checks against guidelines → Doctor views compliance report → Adjusts treatment if needed → Saves finalized treatment

---

### Check Drug Interactions

**Endpoint**: POST /api/v1/treatments/drug-check

**Purpose**: Check for drug-drug interactions and contraindications

**Who calls it**: DOCTOR role

**Request Body**:
```
{
  "medications": [
    "Insulin Glargine",
    "Metformin",
    "Aspirin"
  ],
  "patientAge": 45,
  "patientConditions": ["Type 2 Diabetes", "Hypertension"],
  "patientAllergies": []
}
```

**Response** (200 OK):
```
{
  "success": true,
  "data": {
    "hasInteractions": true,
    "interactions": [
      {
        "drug1": "Insulin Glargine",
        "drug2": "Metformin",
        "severity": "MINOR",
        "description": "Combined use may enhance glucose-lowering effect",
        "clinicalEffect": "Increased risk of hypoglycemia",
        "recommendation": "Monitor blood glucose levels 4 times daily"
      },
      {
        "drug1": "Metformin",
        "drug2": "Aspirin",
        "severity": "MINOR",
        "description": "Aspirin may slightly increase risk of lactic acidosis",
        "clinicalEffect": "Rare but serious metabolic complication",
        "recommendation": "Monitor kidney function regularly"
      }
    ],
    "contraindications": [],
    "warnings": [
      "Monitor blood glucose levels closely during initiation and dose adjustments",
      "Watch for signs of hypoglycemia: dizziness, sweating, confusion",
      "Regular kidney function monitoring recommended with Metformin use"
    ],
    "riskLevel": "LOW"
  }
}
```

---

### Get Treatment by ID

**Endpoint**: GET /api/v1/treatments/:treatmentId

**Purpose**: Retrieve complete treatment record

**Who calls it**: PATIENT (own treatments), DOCTOR (own patients), ADMIN (all)

**Response** (200 OK): Similar to treatment creation response with full details

---

## Policy Endpoints

### Upload Policy Document

**Endpoint**: POST /api/v1/policies/upload

**Purpose**: Upload insurance policy PDF and trigger RAG ingestion

**Who calls it**: PATIENT (own policies), ADMIN (any patient)

**Content-Type**: multipart/form-data

**Form Fields**:
- policyFile (file): PDF or DOCX file (max 10MB)
- policyNumber (string): Unique policy number
- providerName (string): Insurance company name
- planName (string): Policy plan name
- coverageAmount (decimal): Sum insured
- premium (decimal): Annual premium
- startDate (string): Policy start date (ISO 8601)
- expiryDate (string): Policy end date

**Request Example**:
```
POST /api/v1/policies/upload
Content-Type: multipart/form-data

------WebKitFormBoundary
Content-Disposition: form-data; name="policyFile"; filename="policy.pdf"
Content-Type: application/pdf

(binary PDF content)
------WebKitFormBoundary
Content-Disposition: form-data; name="policyNumber"

POL-2026-12345
------WebKitFormBoundary
Content-Disposition: form-data; name="providerName"

Max Bupa Health Insurance
------WebKitFormBoundary
...
```

**Response** (201 Created):
```
{
  "success": true,
  "message": "Policy uploaded successfully. Document ingestion in progress.",
  "data": {
    "policyId": "661e8400-e29b-41d4-a716-446655440002",
    "policyNumber": "POL-2026-12345",
    "status": "ACTIVE",
    "ingestionStatus": "PROCESSING",
    "documentUrl": "https://storage.example.com/policies/550e8400.../POL-2026-12345.pdf?signed",
    "uploadedAt": "2026-01-05T09:00:00Z"
  }
}
```

**Workflow**: File uploaded → Stored in S3/filesystem → Policy record created with PROCESSING status → RAG pipeline triggered asynchronously → Document parsed, chunked, embedded → Stored in ChromaDB → Policy status updated to ACTIVE → Patient notified

---

### Analyze Coverage

**Endpoint**: POST /api/v1/policies/analyze

**Purpose**: AI-powered analysis of whether specific treatment is covered

**Who calls it**: PATIENT (own policies), DOCTOR (patient's policies)

**Request Body**:
```
{
  "policyId": "661e8400-e29b-41d4-a716-446655440002",
  "diagnosis": "Type 2 Diabetes Mellitus",
  "procedures": ["Insulin therapy", "Blood glucose monitoring"],
  "medications": ["Insulin Glargine", "Metformin"],
  "estimatedCost": 85000.00
}
```

**Response** (200 OK):
```
{
  "success": true,
  "data": {
    "isCovered": true,
    "coverageStatus": "PARTIAL",
    "policyClauses": [
      {
        "clause": "4.2.1",
        "text": "Diabetes management including insulin therapy is covered with 25% co-payment",
        "source": "Premium_Health_Plus_Policy.pdf",
        "page": 12
      },
      {
        "clause": "4.3",
        "text": "Annual limit for diabetes-related treatments: ₹500,000",
        "source": "Premium_Health_Plus_Policy.pdf",
        "page": 13
      }
    ],
    "coveredItems": [
      "Insulin therapy",
      "Blood glucose monitoring",
      "Diabetes medications"
    ],
    "nonCoveredItems": [],
    "coPay": 21250.00,
    "coPayPercentage": 25,
    "deductibles": 0,
    "networkDiscount": 8500.00,
    "estimatedPayableByInsurance": 57375.00,
    "estimatedPatientResponsibility": 27625.00,
    "annualLimitRemaining": 442625.00,
    "warnings": [
      "After this treatment, you will have used 11.5% of your annual diabetes coverage limit"
    ],
    "suggestedActions": [
      "Consider using network hospital to avail 10% discount",
      "Utilize pharmacy benefits for long-term medication refills"
    ]
  }
}
```

---

### Get Coverage Breakdown

**Endpoint**: GET /api/v1/policies/:policyId/coverage

**Purpose**: Retrieve complete coverage details for a policy

**Who calls it**: PATIENT (own policies), ADMIN (any policy)

**Response** (200 OK):
```
{
  "success": true,
  "data": {
    "policyId": "661e8400-e29b-41d4-a716-446655440002",
    "policyNumber": "POL-2026-12345",
    "overallCoverage": 500000.00,
    "categories": [
      {
        "category": "DIABETES_CARE",
        "limit": 100000.00,
        "used": 57375.00,
        "remaining": 42625.00,
        "utilizationPercentage": 57.4,
        "coPay": 25
      },
      {
        "category": "CARDIAC_CARE",
        "limit": 200000.00,
        "used": 0,
        "remaining": 200000.00,
        "utilizationPercentage": 0,
        "coPay": 20
      },
      {
        "category": "GENERAL_ILLNESS",
        "limit": 500000.00,
        "used": 70000.00,
        "remaining": 430000.00,
        "utilizationPercentage": 14,
        "coPay": 10
      }
    ],
    "exclusions": [
      "Pre-existing conditions for first 2 years",
      "Cosmetic surgery",
      "Experimental treatments"
    ],
    "networkBenefits": {
      "discount": 10,
      "description": "10% discount on total eligible bill for network hospital admissions"
    }
  }
}
```

---

## Decision Endpoints

### Generate Decision

**Endpoint**: POST /api/v1/decisions/generate

**Purpose**: Manually trigger AI decision generation (normally automatic)

**Who calls it**: PATIENT (own claims), DOCTOR (related treatments), ADMIN (any claim)

**Request Body**:
```
{
  "claimId": "994e8400-e29b-41d4-a716-446655440005",
  "decisionType": "CLAIM_ELIGIBILITY",
  "context": {
    "patientAge": 45,
    "diagnosis": "Type 2 Diabetes",
    "treatmentPlan": "Insulin therapy + dietary management",
    "hospitalType": "NETWORK",
    "estimatedCost": 85000
  }
}
```

**Response** (201 Created):
```
{
  "success": true,
  "message": "Decision generated successfully",
  "data": {
    "decisionId": "aa5e8400-e29b-41d4-a716-446655440006",
    "decisionNumber": "DEC-20260220-0001",
    "outcome": "APPROVED",
    "payableAmount": 57375.00,
    "patientResponsibility": 30125.00,
    "confidenceScore": 94.6,
    "reasoning": "The claim for Type 2 Diabetes treatment at Apollo Hospital Mumbai is APPROVED. According to Clause 4.2.1 of your Premium Health Plus policy...",
    "citations": [
      {
        "clause": "4.2.1",
        "source": "Premium_Health_Plus_Policy.pdf",
        "page": 12,
        "text": "Diabetes management including insulin therapy is covered with 25% co-payment"
      }
    ],
    "warnings": [
      "After this claim, you will have used 11.5% of your annual diabetes coverage limit"
    ],
    "suggestedActions": [
      "Keep all medical bills and prescriptions for records",
      "Schedule follow-up appointment with endocrinologist within 2 weeks"
    ],
    "friendlySummary": "Great news! Your claim for diabetes treatment has been approved. Your insurance will cover ₹57,375 of the ₹87,500 total cost.",
    "processingTime": "2.8 seconds",
    "createdAt": "2026-02-20T17:03:25Z"
  }
}
```

**Note**: This endpoint is primarily for admin override scenarios. Normal flow triggers decision automatically on claim submission.

---

### Get Decision by ID

**Endpoint**: GET /api/v1/decisions/:decisionId

**Purpose**: Retrieve full decision with complete explainability details

**Who calls it**: PATIENT (own decisions), DOCTOR (related), ADMIN (all)

**Response** (200 OK): Full decision object with additional reasoning trace
```
{
  "success": true,
  "data": {
    "decisionId": "aa5e8400-e29b-41d4-a716-446655440006",
    "decisionNumber": "DEC-20260220-0001",
    "outcome": "APPROVED",
    "payableAmount": 57375.00,
    "patientResponsibility": 30125.00,
    "confidenceScore": 94.6,
    "reasoning": "...",
    "citations": [...],
    "warnings": [...],
    "suggestedActions": [...],
    "friendlySummary": "...",
    "reasoningTrace": [
      {
        "step": 1,
        "action": "Intent Detection",
        "result": "Detected CLAIM_ELIGIBILITY decision type",
        "timestamp": "2026-02-20T17:03:12Z"
      },
      {
        "step": 2,
        "action": "Tool Selection",
        "result": "Selected tools: PolicyRetriever, DrugSafetyTool, CoverageCalculator, ClaimValidator",
        "timestamp": "2026-02-20T17:03:12.234Z"
      },
      {
        "step": 3,
        "action": "Tool Execution - PolicyRetriever",
        "result": "Retrieved 3 relevant policy clauses with avg similarity 0.87",
        "timestamp": "2026-02-20T17:03:13.120Z"
      },
      {
        "step": 4,
        "action": "Tool Execution - DrugSafetyTool",
        "result": "No major interactions found, 2 minor warnings",
        "timestamp": "2026-02-20T17:03:13.456Z"
      },
      {
        "step": 5,
        "action": "Tool Execution - CoverageCalculator",
        "result": "Calculated payable amount: ₹57,375",
        "timestamp": "2026-02-20T17:03:13.789Z"
      },
      {
        "step": 6,
        "action": "Observation",
        "result": "Retrieval quality sufficient (confidence: 0.89), proceeding to LLM synthesis",
        "timestamp": "2026-02-20T17:03:14.012Z"
      },
      {
        "step": 7,
        "action": "LLM Synthesis",
        "result": "LLM generated structured decision with reasoning",
        "timestamp": "2026-02-20T17:03:15.200Z"
      },
      {
        "step": 8,
        "action": "Explanation Generation",
        "result": "Generated citations, confidence score, and patient-friendly summary",
        "timestamp": "2026-02-20T17:03:15.247Z"
      }
    ],
    "toolsUsed": ["PolicyRetrieverTool", "DrugSafetyTool", "CoverageCalculatorTool", "ClaimValidationTool"],
    "processingTimeMs": 2847,
    "createdAt": "2026-02-20T17:03:25Z"
  }
}
```

**Explainability Features**:
- Outcome: Clear decision (APPROVED/REJECTED/PARTIAL)
- Reasoning: Natural language explanation
- Citations: Specific policy references with page numbers
- Confidence Score: Transparent AI confidence level
- Warnings: Proactive alerts
- Suggested Actions: Actionable next steps
- Reasoning Trace: Complete agent thought process
- Friendly Summary: Jargon-free explanation

---

### Get Decisions for Claim

**Endpoint**: GET /api/v1/decisions/claim/:claimId

**Purpose**: Retrieve all decisions for a claim (initial + revisions + appeals)

**Who calls it**: PATIENT (own claims), ADMIN (all)

**Response** (200 OK): Array of decisions ordered by creation date (newest first)

---

### Submit Decision Feedback

**Endpoint**: POST /api/v1/decisions/:decisionId/feedback

**Purpose**: Allow users to rate decision helpfulness

**Who calls it**: PATIENT (own decisions)

**Request Body**:
```
{
  "rating": 5,
  "wasHelpful": true,
  "comment": "Very clear explanation, helped me understand my coverage"
}
```

**Response** (200 OK):
```
{
  "success": true,
  "message": "Feedback submitted successfully"
}
```

---

## Error Response Format

All endpoints follow consistent error response structure:

**Format**:
```
{
  "timestamp": "2026-02-19T14:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Claim amount cannot exceed treatment estimated cost",
  "path": "/api/v1/claims/submit",
  "details": {
    "claimAmount": 100000,
    "treatmentCost": 85000,
    "maxAllowed": 85000
  }
}
```

**HTTP Status Codes**:
- 200 OK: Successful GET/PUT
- 201 Created: Successful POST (resource created)
- 204 No Content: Successful DELETE
- 400 Bad Request: Validation error, invalid input
- 401 Unauthorized: Missing or invalid authentication token
- 403 Forbidden: Authenticated but lacks required role/permission
- 404 Not Found: Resource does not exist
- 409 Conflict: Resource conflict (e.g., duplicate policy number)
- 500 Internal Server Error: Unexpected server error

---

## Rate Limiting

To prevent API abuse:

**Limits**:
- Unauthenticated endpoints: 100 requests per hour per IP
- Authenticated endpoints: 1000 requests per hour per user
- File upload endpoints: 50 uploads per hour per user

**Rate Limit Headers** (included in response):
```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 985
X-RateLimit-Reset: 1708359600
```

**Rate Limit Exceeded** (429 Too Many Requests):
```
{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Please try again in 15 minutes.",
  "retryAfter": 900
}
```

---

## Pagination

List endpoints support pagination:

**Query Parameters**:
- page: Page number (0-indexed, default: 0)
- size: Items per page (default: 20, max: 100)
- sort: Sort field (default varies by endpoint)
- order: Sort order (asc or desc, default: desc)

**Example**: GET /api/v1/claims/patient/550e8400...?page=0&size=20&sort=submittedAt&order=desc

**Response includes pagination metadata**:
```
{
  "data": {
    "claims": [...],
    "pagination": {
      "page": 0,
      "size": 20,
      "totalElements": 45,
      "totalPages": 3,
      "isFirst": true,
      "isLast": false,
      "hasNext": true,
      "hasPrevious": false
    }
  }
}
```

---

## CORS Configuration

Cross-Origin Resource Sharing (CORS) is configured to allow frontend access:

**Allowed Origins** (development):
- http://localhost:5173
- http://localhost:3000

**Allowed Origins** (production):
- https://app.medisure.ai

**Allowed Methods**: GET, POST, PUT, DELETE, OPTIONS

**Allowed Headers**: Authorization, Content-Type, X-Requested-With

**Exposed Headers**: X-RateLimit-Limit, X-RateLimit-Remaining

**Credentials Allowed**: Yes (for cookies if used)

---

This API provides a comprehensive, secure, and well-documented interface for all MediSureAI operations with full support for the agentic AI decision-making workflow.
