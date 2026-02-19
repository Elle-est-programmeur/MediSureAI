# Database Schema & ER Diagram

## Data Model Overview

MediSureAI uses a relational database (PostgreSQL) to store structured operational data including users, policies, treatments, claims, and decisions. The schema is designed for:

- **Data integrity**: Foreign key constraints ensure referential integrity
- **Audit capability**: Timestamps on all entities for tracking changes
- **Scalability**: Indexed columns for frequently queried fields
- **Flexibility**: JSON columns for semi-structured data (medications, citations, etc.)
- **Privacy**: Sensitive fields can be encrypted at application level

The database follows Third Normal Form (3NF) to minimize redundancy while maintaining query performance through strategic indexing and denormalization where appropriate.

---

## Entity Relationship Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              USERS                                      │
│  ┌───────────────────────────────────────────────────────────────────┐ │
│  │ PK  id (UUID)                                                     │ │
│  │     name (VARCHAR 255)                                            │ │
│  │     email (VARCHAR 255) UNIQUE                                    │ │
│  │     password_hash (VARCHAR 255)                                   │ │
│  │     role (VARCHAR 50)  [PATIENT, DOCTOR, ADMIN]                  │ │
│  │     phone_number (VARCHAR 20)                                     │ │
│  │     date_of_birth (DATE)                                          │ │
│  │     specialization (VARCHAR 255) -- for DOCTOR                    │ │
│  │     license_number (VARCHAR 100) -- for DOCTOR                    │ │
│  │     hospital_affiliation (VARCHAR 255)                            │ │
│  │     is_active (BOOLEAN DEFAULT TRUE)                              │ │
│  │     created_at (TIMESTAMP DEFAULT NOW())                          │ │
│  │     updated_at (TIMESTAMP)                                        │ │
│  └───────────────────────────────────────────────────────────────────┘ │
└────┬─────────────────────────┬─────────────────────────┬──────────────┘
     │                         │                         │
     │ 1:N (as patient)        │ 1:N (as patient)        │ 1:N (as doctor)
     │                         │                         │
     ↓                         ↓                         ↓
┌─────────────────┐  ┌─────────────────┐  ┌──────────────────────────┐
│   POLICIES      │  │     CLAIMS      │  │      TREATMENTS          │
│ ┌─────────────┐ │  │ ┌─────────────┐ │  │ ┌──────────────────────┐ │
│ │PK id (UUID) │ │  │ │PK id (UUID) │ │  │ │PK id (UUID)          │ │
│ │FK patient_id│─┼──┼─│FK patient_id│ │  │ │FK patient_id         │ │
│ │policy_number│ │  │ │FK policy_id │─┼──┼─│FK doctor_id          │ │
│ │provider_name│ │  │ │FK treatment │ │  │ │treatment_number      │ │
│ │plan_name    │ │  │ │  _id        │─┼┐ │ │diagnosis (TEXT)      │ │
│ │coverage_amt │ │  │ │claim_number │ ││ │ │secondary_diagnoses   │ │
│ │premium      │ │  │ │claim_amount │ ││ │ │  (JSONB)             │ │
│ │start_date   │ │  │ │approved_amt │ ││ │ │treatment_plan (TEXT) │ │
│ │expiry_date  │ │  │ │status       │ ││ │ │procedures (JSONB)    │ │
│ │status       │ │  │ │rejection    │ ││ │ │medications (JSONB)   │ │
│ │document_path│ │  │ │  _reason    │ ││ │ │hospital_name         │ │
│ │document_url │ │  │ │submitted_at │ ││ │ │hospital_type         │ │
│ │is_ingested  │ │  │ │processed_at │ ││ │ │admission_date        │ │
│ │  _into_rag  │ │  │ │updated_at   │ ││ │ │discharge_date        │ │
│ │ingestion    │ │  │ └─────────────┘ ││ │ │estimated_cost        │ │
│ │  _status    │ │  └────┬────────────┘│ │ │actual_cost           │ │
│ │coverage     │ │       │             │ │ │treatment_status      │ │
│ │  _categories│ │       │ 1:N         │ │ │guideline_compliance  │ │
│ │  (JSONB)    │ │       ↓             │ │ │notes (TEXT)          │ │
│ │exclusions   │ │  ┌─────────────────┐│ │ │created_at            │ │
│ │  (JSONB)    │ │  │   DECISIONS     ││ │ │updated_at            │ │
│ │co_pay       │ │  │┌───────────────┐││ │ └──────────────────────┘ │
│ │  (JSONB)    │ │  ││PK id (UUID)   │││ └──────────────────────────┘
│ │annual_limit │ │  ││FK claim_id    │││              ↑
│ │annual_limit │ │  ││FK patient_id  │││              │ 1:1
│ │  _used      │ │  ││decision       │││              │
│ │created_at   │ │  ││  _number      │││              │
│ │updated_at   │ │  ││decision_type  │││         ┌────┘
│ └─────────────┘ │  ││outcome        │││         │
└────┬────────────┘  ││payable_amount │││    ┌────┴────┐
     │ 1:N           ││patient        │││    │  CLAIM  │
     │               ││  _responsibility│││  │relationships│
     └───────────────┼┤reasoning (TEXT)││├────┤         │
                     ││confidence     │││    └─────────┘
                     ││  _score       │││
                     ││citations      │││
                     ││  (JSONB)      │││
                     ││warnings       │││
                     ││  (JSONB)      │││
                     ││suggested      │││
                     ││  _actions     │││
                     ││  (JSONB)      │││
                     ││friendly       │││
                     ││  _summary     │││
                     ││  (TEXT)       │││
                     ││tools_used     │││
                     ││  (JSONB)      │││
                     ││processing     │││
                     ││  _time_ms     │││
                     ││is_active      │││
                     ││review_status  │││
                     ││reviewed_by    │││
                     ││  (UUID FK)    ││├─── references USERS (admin)
                     ││review_notes   │││
                     ││created_at     │││
                     │└───────────────┘││
                     └─────────────────┘│
                                       │
┌──────────────────────────────────────┼──────────────────────────────────┐
│                                      │                                   │
│                         AUDIT_LOGS   ↓                                   │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ PK  id (UUID)                                                   │    │
│  │     entity_type (VARCHAR 100)  [CLAIM, POLICY, DECISION, etc.]  │    │
│  │     entity_id (UUID)                                             │    │
│  │     action (VARCHAR 100) [CREATE, UPDATE, DELETE, OVERRIDE]      │    │
│  │ FK  performed_by (UUID) -- references USERS                      │────┘
│  │     details (JSONB) -- before/after values, reason, etc.         │
│  │     timestamp (TIMESTAMP DEFAULT NOW())                          │
│  └─────────────────────────────────────────────────────────────────┘
└──────────────────────────────────────────────────────────────────────────┘
```

---

## Table Descriptions

### USERS Table

**Purpose**: Store all system users including patients, doctors, and administrators.

**Primary Key**: id (UUID, auto-generated)

**Unique Constraints**:
- email (UNIQUE) - ensures one account per email address

**Indexes**:
- PRIMARY KEY on id
- UNIQUE INDEX on email
- INDEX on role (for role-based queries)

**Column Details**:

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| id | UUID | NO | Unique identifier for user |
| name | VARCHAR(255) | NO | Full name of user |
| email | VARCHAR(255) | NO | Email address, used for login |
| password_hash | VARCHAR(255) | NO | BCrypt hashed password |
| role | VARCHAR(50) | NO | User role: PATIENT, DOCTOR, or ADMIN |
| phone_number | VARCHAR(20) | YES | Contact phone number |
| date_of_birth | DATE | YES | Date of birth (required for patients) |
| specialization | VARCHAR(255) | YES | Medical specialization (required for doctors) |
| license_number | VARCHAR(100) | YES | Medical license number (required for doctors) |
| hospital_affiliation | VARCHAR(255) | YES | Hospital where doctor practices |
| is_active | BOOLEAN | NO | Account status, default TRUE |
| created_at | TIMESTAMP | NO | Account creation timestamp |
| updated_at | TIMESTAMP | YES | Last update timestamp |

**Business Rules**:
- Email must be unique across all users
- Role cannot be changed after creation (security constraint)
- password_hash is never exposed in API responses
- Doctors must have specialization and license_number populated
- Patients must have date_of_birth for age-based eligibility checks

**Sample Row**:
```
id: 550e8400-e29b-41d4-a716-446655440001
name: Alice Kumar
email: alice@example.com
password_hash: $2a$12$... (BCrypt hash)
role: PATIENT
phone_number: +91-9876543210
date_of_birth: 1980-05-15
specialization: NULL
license_number: NULL
hospital_affiliation: NULL
is_active: TRUE
created_at: 2026-01-15 10:30:00
updated_at: 2026-01-15 10:30:00
```

---

### POLICIES Table

**Purpose**: Store insurance policy documents and metadata for patients.

**Primary Key**: id (UUID, auto-generated)

**Foreign Keys**:
- patient_id → USERS(id) ON DELETE CASCADE

**Unique Constraints**:
- policy_number (UNIQUE) - each policy has unique identifier

**Indexes**:
- PRIMARY KEY on id
- FOREIGN KEY INDEX on patient_id
- UNIQUE INDEX on policy_number
- INDEX on status (for filtering active policies)
- INDEX on expiry_date (for finding expiring policies)

**Column Details**:

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| id | UUID | NO | Unique identifier for policy |
| patient_id | UUID | NO | Foreign key to USERS table (patient who owns policy) |
| policy_number | VARCHAR(100) | NO | Unique policy identifier from insurance provider |
| provider_name | VARCHAR(255) | NO | Insurance company name (e.g., "Max Bupa Health Insurance") |
| plan_name | VARCHAR(255) | NO | Policy plan name (e.g., "Premium Health Plus") |
| coverage_amount | DECIMAL(12,2) | NO | Sum insured / maximum coverage amount in rupees |
| premium | DECIMAL(10,2) | NO | Annual premium paid for policy |
| start_date | DATE | NO | Policy effective start date |
| expiry_date | DATE | NO | Policy expiration date |
| status | VARCHAR(50) | NO | ACTIVE, EXPIRED, SUSPENDED, or CANCELLED |
| document_path | TEXT | YES | File system path to stored policy PDF |
| document_url | TEXT | YES | Signed URL for secure document access |
| is_ingested_into_rag | BOOLEAN | NO | Whether policy document has been ingested into vector DB |
| ingestion_status | VARCHAR(50) | NO | PENDING, PROCESSING, COMPLETED, or FAILED |
| coverage_categories | JSONB | YES | Array of covered categories: ["DIABETES_COVERAGE", "CARDIAC_CARE"] |
| exclusions | JSONB | YES | Array of exclusions: ["Pre-existing conditions", "Cosmetic surgery"] |
| co_pay | JSONB | YES | Co-payment rules: {"DIABETES": 25, "GENERAL": 10} (percentages) |
| annual_limit | DECIMAL(12,2) | NO | Maximum coverage per year |
| annual_limit_used | DECIMAL(12,2) | NO | Amount already utilized this year, default 0 |
| created_at | TIMESTAMP | NO | Policy record creation timestamp |
| updated_at | TIMESTAMP | YES | Last update timestamp |

**Business Rules**:
- One patient can have multiple policies (primary, secondary coverage)
- Policy must be ACTIVE to be used for claim submission
- annual_limit_used resets when policy renews (start_date anniversary)
- is_ingested_into_rag must be TRUE for AI-powered coverage analysis
- expiry_date must be after start_date

**Sample Row**:
```
id: 661e8400-e29b-41d4-a716-446655440002
patient_id: 550e8400-e29b-41d4-a716-446655440001
policy_number: POL-2026-12345
provider_name: Max Bupa Health Insurance
plan_name: Premium Health Plus
coverage_amount: 500000.00
premium: 25000.00
start_date: 2026-01-01
expiry_date: 2026-12-31
status: ACTIVE
document_path: /data/policies/550e8400.../POL-2026-12345.pdf
document_url: https://storage.example.com/...?signed_url
is_ingested_into_rag: TRUE
ingestion_status: COMPLETED
coverage_categories: ["DIABETES_COVERAGE", "CARDIAC_CARE", "GENERAL_ILLNESS"]
exclusions: ["Pre-existing conditions for first 2 years", "Cosmetic procedures"]
co_pay: {"DIABETES": 25, "CARDIAC": 20, "GENERAL": 10}
annual_limit: 500000.00
annual_limit_used: 127500.00
created_at: 2026-01-05 09:00:00
updated_at: 2026-02-10 14:20:00
```

---

### TREATMENTS Table

**Purpose**: Record medical treatments provided to patients, including diagnosis, medications, procedures, and costs.

**Primary Key**: id (UUID, auto-generated)

**Foreign Keys**:
- patient_id → USERS(id) ON DELETE CASCADE
- doctor_id → USERS(id) ON DELETE CASCADE

**Indexes**:
- PRIMARY KEY on id
- FOREIGN KEY INDEX on patient_id
- FOREIGN KEY INDEX on doctor_id
- INDEX on admission_date (for date range queries)
- INDEX on treatment_status (for filtering by status)

**Column Details**:

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| id | UUID | NO | Unique identifier for treatment |
| treatment_number | VARCHAR(100) | NO | Human-readable treatment identifier (e.g., TRT-20260219-0001) |
| patient_id | UUID | NO | Foreign key to patient in USERS table |
| doctor_id | UUID | NO | Foreign key to doctor in USERS table |
| diagnosis | TEXT | NO | Primary diagnosis with ICD-10 code |
| secondary_diagnoses | JSONB | YES | Array of additional diagnoses: [{"code": "E11.9", "description": "Type 2 Diabetes"}] |
| treatment_plan | TEXT | YES | Detailed description of treatment approach |
| procedures | JSONB | YES | Array of procedures: [{"code": "CPT-12345", "name": "Blood glucose monitoring", "quantity": 3}] |
| medications | JSONB | YES | Array of medications: [{"name": "Insulin Glargine", "dosage": "50 units", "frequency": "daily", "duration": "30 days"}] |
| hospital_name | VARCHAR(255) | YES | Name of hospital where treatment provided |
| hospital_type | VARCHAR(50) | YES | NETWORK, NON_NETWORK, or DAY_CARE |
| admission_date | DATE | YES | Date of hospital admission (if applicable) |
| discharge_date | DATE | YES | Date of discharge (NULL if treatment ongoing) |
| estimated_cost | DECIMAL(12,2) | NO | Estimated total cost of treatment |
| actual_cost | DECIMAL(12,2) | YES | Actual cost after treatment completion |
| treatment_status | VARCHAR(50) | NO | PLANNED, IN_PROGRESS, COMPLETED, or CANCELLED |
| guideline_compliance | VARCHAR(50) | YES | NOT_CHECKED, COMPLIANT, NON_COMPLIANT, or PARTIAL |
| notes | TEXT | YES | Doctor's notes and observations |
| created_at | TIMESTAMP | NO | Treatment record creation timestamp |
| updated_at | TIMESTAMP | YES | Last update timestamp |

**Business Rules**:
- One treatment record per treatment episode
- Doctor role must be DOCTOR (enforced at application level)
- discharge_date must be after admission_date if both present
- actual_cost populated only after treatment_status = COMPLETED
- One treatment can have only one associated claim

**Sample Row**:
```
id: 772e8400-e29b-41d4-a716-446655440003
treatment_number: TRT-20260219-0001
patient_id: 550e8400-e29b-41d4-a716-446655440001
doctor_id: 883e8400-e29b-41d4-a716-446655440004
diagnosis: E11.9 - Type 2 Diabetes Mellitus without complications
secondary_diagnoses: [{"code": "I10", "description": "Essential hypertension"}]
treatment_plan: Insulin therapy initiation with dietary counseling and blood glucose monitoring
procedures: [{"code": "CPT-82947", "name": "Glucose blood test", "quantity": 4}]
medications: [
  {"name": "Insulin Glargine", "dosage": "50 units", "frequency": "once daily", "duration": "ongoing"},
  {"name": "Metformin", "dosage": "500mg", "frequency": "twice daily", "duration": "ongoing"}
]
hospital_name: Apollo Hospital Mumbai
hospital_type: NETWORK
admission_date: 2026-02-15
discharge_date: 2026-02-20
estimated_cost: 85000.00
actual_cost: 87500.00
treatment_status: COMPLETED
guideline_compliance: COMPLIANT
notes: Patient responded well to insulin therapy. Blood glucose levels stabilized within target range.
created_at: 2026-02-15 08:30:00
updated_at: 2026-02-20 16:45:00
```

---

### CLAIMS Table

**Purpose**: Track insurance claim submissions from patients for reimbursement of medical treatments.

**Primary Key**: id (UUID, auto-generated)

**Foreign Keys**:
- patient_id → USERS(id) ON DELETE CASCADE
- policy_id → POLICIES(id) ON DELETE CASCADE
- treatment_id → TREATMENTS(id) ON DELETE CASCADE

**Indexes**:
- PRIMARY KEY on id
- UNIQUE INDEX on treatment_id (one claim per treatment)
- FOREIGN KEY INDEX on patient_id
- FOREIGN KEY INDEX on policy_id
- INDEX on status (for filtering by claim status)
- INDEX on submitted_at (for date range queries)

**Column Details**:

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| id | UUID | NO | Unique identifier for claim |
| claim_number | VARCHAR(100) | NO | Human-readable claim identifier (e.g., CLM-20260219-0001) |
| patient_id | UUID | NO | Foreign key to patient who submitted claim |
| policy_id | UUID | NO | Foreign key to policy under which claim is made |
| treatment_id | UUID | NO | Foreign key to treatment for which claim is made |
| claim_amount | DECIMAL(12,2) | NO | Total amount claimed by patient |
| approved_amount | DECIMAL(12,2) | YES | Amount approved by insurance (NULL if rejected) |
| status | VARCHAR(50) | NO | PENDING, APPROVED, REJECTED, PARTIAL, or UNDER_REVIEW |
| rejection_reason | TEXT | YES | Explanation for rejection (if status = REJECTED) |
| submitted_at | TIMESTAMP | NO | Timestamp when claim was submitted |
| processed_at | TIMESTAMP | YES | Timestamp when AI decision was generated |
| updated_at | TIMESTAMP | YES | Last update timestamp |

**Business Rules**:
- Treatment can have only one claim (enforced by UNIQUE constraint on treatment_id)
- Policy must be ACTIVE at time of treatment
- claim_amount cannot exceed treatment estimated_cost
- approved_amount populated only if status = APPROVED or PARTIAL
- rejection_reason required if status = REJECTED

**Status Transitions**:
```
PENDING → UNDER_REVIEW → APPROVED
                       → REJECTED
                       → PARTIAL
```

**Sample Row**:
```
id: 994e8400-e29b-41d4-a716-446655440005
claim_number: CLM-20260220-0001
patient_id: 550e8400-e29b-41d4-a716-446655440001
policy_id: 661e8400-e29b-41d4-a716-446655440002
treatment_id: 772e8400-e29b-41d4-a716-446655440003
claim_amount: 87500.00
approved_amount: 57375.00
status: APPROVED
rejection_reason: NULL
submitted_at: 2026-02-20 17:00:00
processed_at: 2026-02-20 17:03:25
updated_at: 2026-02-20 17:03:25
```

---

### DECISIONS Table

**Purpose**: Store AI-generated decisions with full explainability including reasoning, citations, confidence scores, and recommendations.

**Primary Key**: id (UUID, auto-generated)

**Foreign Keys**:
- claim_id → CLAIMS(id) ON DELETE CASCADE (nullable for non-claim decisions)
- patient_id → USERS(id) ON DELETE CASCADE
- reviewed_by → USERS(id) ON DELETE SET NULL (nullable, references admin reviewer)

**Indexes**:
- PRIMARY KEY on id
- FOREIGN KEY INDEX on claim_id
- FOREIGN KEY INDEX on patient_id
- INDEX on outcome (for filtering by decision outcome)
- INDEX on confidence_score (for finding low-confidence decisions)
- INDEX on is_active (for getting current decision for a claim)

**Column Details**:

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| id | UUID | NO | Unique identifier for decision |
| decision_number | VARCHAR(100) | NO | Human-readable decision identifier (e.g., DEC-20260220-0001) |
| claim_id | UUID | YES | Foreign key to claim (nullable for non-claim decisions like treatment validation) |
| patient_id | UUID | NO | Foreign key to patient |
| decision_type | VARCHAR(50) | NO | CLAIM_ELIGIBILITY, TREATMENT_VALIDATION, DRUG_SAFETY, BILL_EXPLANATION, POLICY_COVERAGE |
| outcome | VARCHAR(50) | NO | APPROVED, REJECTED, PARTIAL, WARNING, or INFO |
| payable_amount | DECIMAL(12,2) | YES | Amount insurance will pay (for CLAIM_ELIGIBILITY decisions) |
| patient_responsibility | DECIMAL(12,2) | YES | Amount patient must pay |
| reasoning | TEXT | NO | Detailed explanation of decision from LLM |
| confidence_score | DECIMAL(5,2) | NO | AI confidence level 0-100 |
| citations | JSONB | YES | Array of policy clause references: [{"clause": "4.2.1", "source": "policy.pdf", "page": 12, "text": "..."}] |
| warnings | JSONB | YES | Array of warnings: ["Annual limit 73% utilized", "Monitor blood glucose"] |
| suggested_actions | JSONB | YES | Array of recommendations: ["Consider day-care admission", "Use pharmacy benefits"] |
| friendly_summary | TEXT | YES | Patient-friendly explanation in plain language |
| tools_used | JSONB | YES | Array of tools agent executed: ["PolicyRetriever", "DrugSafetyTool", "CoverageCalculator"] |
| processing_time_ms | INTEGER | YES | Milliseconds taken for agent to generate decision |
| is_active | BOOLEAN | NO | Whether this is the current active decision for the claim (default TRUE) |
| review_status | VARCHAR(50) | NO | NOT_REVIEWED, UNDER_REVIEW, REVIEWED, or OVERRIDDEN |
| reviewed_by | UUID | YES | Foreign key to admin user who reviewed (if applicable) |
| review_notes | TEXT | YES | Admin's review comments or override reason |
| created_at | TIMESTAMP | NO | Decision creation timestamp |

**Business Rules**:
- One claim can have multiple decisions (initial + revisions + appeals)
- Only one decision can be is_active = TRUE per claim at a time
- Decisions are immutable (never updated, only new versions created)
- confidence_score < 70 automatically sets review_status = UNDER_REVIEW
- citations array must reference actual retrieved policy clauses

**Sample Row**:
```
id: aa5e8400-e29b-41d4-a716-446655440006
decision_number: DEC-20260220-0001
claim_id: 994e8400-e29b-41d4-a716-446655440005
patient_id: 550e8400-e29b-41d4-a716-446655440001
decision_type: CLAIM_ELIGIBILITY
outcome: APPROVED
payable_amount: 57375.00
patient_responsibility: 30125.00
reasoning: The claim for Type 2 Diabetes treatment at Apollo Hospital Mumbai is APPROVED. According to Clause 4.2.1 of your Premium Health Plus policy, diabetes management including insulin therapy is covered with a 25% co-payment, resulting in ₹21,875 patient responsibility. As this is a network hospital admission, Clause 8.1 grants an additional 10% discount of ₹6,537.50 on the payable amount. The total cost of ₹87,500 is within your annual diabetes treatment limit of ₹500,000 (Clause 4.3). Drug safety analysis shows minor interaction between prescribed medications requiring glucose monitoring but no contraindications. The insurance will pay ₹57,375, and you are responsible for ₹30,125.
confidence_score: 94.60
citations: [
  {"clause": "4.2.1", "source": "Premium_Health_Plus_Policy.pdf", "page": 12, "text": "Diabetes management including insulin therapy is covered with 25% co-payment"},
  {"clause": "8.1", "source": "Premium_Health_Plus_Policy.pdf", "page": 24, "text": "Network hospital admissions receive 10% discount"},
  {"clause": "4.3", "source": "Premium_Health_Plus_Policy.pdf", "page": 13, "text": "Annual limit for diabetes treatments: ₹500,000"}
]
warnings: ["After this claim, you will have used 11.5% of your annual diabetes coverage limit", "Monitor blood glucose levels 4 times daily"]
suggested_actions: ["Keep all medical bills and prescriptions for records", "Schedule follow-up appointment with endocrinologist within 2 weeks", "Consider using hospital day-care facilities for future consultations"]
friendly_summary: Great news! Your claim for diabetes treatment has been approved. Your insurance will cover ₹57,375 of the ₹87,500 total cost. You'll need to pay ₹30,125, which includes your 25% co-payment minus a 10% network hospital discount.
tools_used: ["PolicyRetrieverTool", "DrugSafetyTool", "CoverageCalculatorTool", "ClaimValidationTool"]
processing_time_ms: 2847
is_active: TRUE
review_status: NOT_REVIEWED
reviewed_by: NULL
review_notes: NULL
created_at: 2026-02-20 17:03:25
```

---

### AUDIT_LOGS Table

**Purpose**: Maintain complete audit trail of all system actions for compliance, debugging, and security monitoring.

**Primary Key**: id (UUID, auto-generated)

**Foreign Keys**:
- performed_by → USERS(id) ON DELETE SET NULL

**Indexes**:
- PRIMARY KEY on id
- FOREIGN KEY INDEX on performed_by
- INDEX on entity_type (for filtering by entity)
- INDEX on action (for filtering by action type)
- INDEX on timestamp (for date range queries)
- COMPOSITE INDEX on (entity_type, entity_id) for entity audit history

**Column Details**:

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| id | UUID | NO | Unique identifier for audit log entry |
| entity_type | VARCHAR(100) | NO | Type of entity: CLAIM, POLICY, DECISION, TREATMENT, USER |
| entity_id | UUID | NO | ID of the entity that was affected |
| action | VARCHAR(100) | NO | Action performed: CREATE, UPDATE, DELETE, OVERRIDE, LOGIN, LOGOUT |
| performed_by | UUID | YES | Foreign key to user who performed action (nullable for system actions) |
| details | JSONB | YES | Structured details about action: before/after values, reason, IP address, etc. |
| timestamp | TIMESTAMP | NO | When action was performed (default NOW()) |

**Business Rules**:
- Audit logs are append-only (never updated or deleted)
- System actions (automated agent decisions) have performed_by = NULL
- details field contains comprehensive context for forensic analysis

**Sample Rows**:
```
-- User registration
id: bb6e8400-e29b-41d4-a716-446655440007
entity_type: USER
entity_id: 550e8400-e29b-41d4-a716-446655440001
action: CREATE
performed_by: 550e8400-e29b-41d4-a716-446655440001
details: {"ip_address": "192.168.1.100", "user_agent": "Mozilla/5.0...", "role": "PATIENT"}
timestamp: 2026-01-15 10:30:00

-- Claim submission
id: cc7e8400-e29b-41d4-a716-446655440008
entity_type: CLAIM
entity_id: 994e8400-e29b-41d4-a716-446655440005
action: CREATE
performed_by: 550e8400-e29b-41d4-a716-446655440001
details: {"claim_amount": 87500.00, "policy_id": "661e8400...", "treatment_id": "772e8400..."}
timestamp: 2026-02-20 17:00:00

-- Decision generated by AI agent
id: dd8e8400-e29b-41d4-a716-446655440009
entity_type: DECISION
entity_id: aa5e8400-e29b-41d4-a716-446655440006
action: CREATE
performed_by: NULL
details: {"claim_id": "994e8400...", "outcome": "APPROVED", "confidence": 94.6, "processing_time_ms": 2847}
timestamp: 2026-02-20 17:03:25

-- Admin overrides decision
id: ee9e8400-e29b-41d4-a716-446655440010
entity_type: DECISION
entity_id: aa5e8400-e29b-41d4-a716-446655440006
action: OVERRIDE
performed_by: ff0e8400-e29b-41d4-a716-446655440011
details: {"before": {"outcome": "REJECTED", "confidence": 65.2}, "after": {"outcome": "APPROVED"}, "reason": "Medical necessity confirmed by external review"}
timestamp: 2026-02-21 10:15:00
```

---

## Relationship Explanations

### User → Policies (One-to-Many)

**Description**: A patient (User with role PATIENT) can own multiple insurance policies.

**Relationship**: One User → Many Policies

**Foreign Key**: policies.patient_id references users.id

**Cascade**: ON DELETE CASCADE (if user deleted, their policies are deleted)

**Use Cases**:
- Primary and secondary insurance coverage
- Family floater policies
- Policy renewal (old policy expires, new policy created)

---

### User → Claims (One-to-Many as Patient)

**Description**: A patient submits multiple claims over time for different treatments.

**Relationship**: One User (as patient) → Many Claims

**Foreign Key**: claims.patient_id references users.id

**Cascade**: ON DELETE CASCADE

---

### User → Treatments (One-to-Many as Patient)

**Description**: A patient receives multiple treatments from various doctors.

**Relationship**: One User (as patient) → Many Treatments

**Foreign Key**: treatments.patient_id references users.id

**Cascade**: ON DELETE CASCADE

---

### User → Treatments (One-to-Many as Doctor)

**Description**: A doctor provides treatment to multiple patients.

**Relationship**: One User (as doctor) → Many Treatments

**Foreign Key**: treatments.doctor_id references users.id

**Cascade**: ON DELETE CASCADE

**Note**: Same entity (User) participates in two different relationships with Treatments.

---

### Policy → Claims (One-to-Many)

**Description**: One policy can have multiple claims submitted against it over its validity period.

**Relationship**: One Policy → Many Claims

**Foreign Key**: claims.policy_id references policies.id

**Cascade**: ON DELETE CASCADE

**Use Cases**:
- Patient submits multiple claims throughout the year
- Track annual limit utilization across all claims

---

### Treatment → Claim (One-to-One)

**Description**: Each treatment can have at most one claim associated with it.

**Relationship**: One Treatment → One Claim

**Foreign Key**: claims.treatment_id references treatments.id

**Unique Constraint**: UNIQUE(treatment_id) ensures one-to-one relationship

**Rationale**: Prevents duplicate claims for same treatment

---

### Claim → Decisions (One-to-Many)

**Description**: One claim can have multiple decisions over time (initial decision, revised decision, appeal decision).

**Relationship**: One Claim → Many Decisions

**Foreign Key**: decisions.claim_id references claims.id

**Cascade**: ON DELETE CASCADE

**Active Decision**: Only one decision has is_active = TRUE at a time for a claim

**Use Cases**:
- Initial AI decision
- Patient appeals → new decision generated
- Admin manually reviews → new decision created

---

### User → Audit Logs (One-to-Many)

**Description**: Track all actions performed by a user.

**Relationship**: One User → Many Audit Logs

**Foreign Key**: audit_logs.performed_by references users.id

**Cascade**: ON DELETE SET NULL (preserve audit trail even if user deleted)

---

## Data Integrity Constraints

### Primary Key Constraints
All tables use UUID primary keys for:
- Globally unique identifiers
- No sequential ID leakage security issue
- Distributed system friendly
- Obfuscated entity counts

### Foreign Key Constraints
All foreign keys enforce referential integrity:
- Cannot create claim without valid policy_id
- Cannot create treatment without valid patient_id and doctor_id
- Cannot reference non-existent entities

### Check Constraints
Business rule validation at database level:
- claim_amount > 0
- approved_amount ≤ claim_amount (if not null)
- confidence_score between 0 and 100
- start_date < expiry_date for policies
- discharge_date ≥ admission_date for treatments (if both not null)

### Unique Constraints
Prevent duplicates:
- users.email (one account per email)
- policies.policy_number (unique policy identifiers)
- claims.treatment_id (one claim per treatment)

---

## Indexing Strategy

### Query Optimization Indexes

**Frequently Queried Columns**:
- users.email (for login queries)
- users.role (for role-based filtering)
- policies.patient_id (get user's policies)
- policies.status (filter active policies)
- claims.patient_id (get user's claims)
- claims.status (filter claims by status)
- decisions.claim_id (get decisions for claim)
- decisions.confidence_score (find low-confidence decisions)

**Composite Indexes**:
- (entity_type, entity_id) on audit_logs for entity history lookups
- (patient_id, status) on claims for filtered patient claim lists

### Index Maintenance
- Rebuild indexes periodically to prevent fragmentation
- Analyze query execution plans to identify missing indexes
- Drop unused indexes to reduce write overhead

---

## Data Privacy and Security

### Sensitive Data
- password_hash: Never exposed in API responses
- document_path: Internal file paths not exposed to users
- review_notes: Only visible to admins

### Encryption
- password_hash: BCrypt hashed (cannot be reversed)
- Consider encrypting: date_of_birth, phone_number (PII) at application level
- Documents: Encrypted at rest in storage

### Access Control
- Row-level security: Patients can only access their own records
- Enforced at application layer with user_id checks
- Admin role can access all records

---

This database schema provides a robust, scalable foundation for MediSureAI's healthcare decision support capabilities while maintaining data integrity, auditability, and security.
