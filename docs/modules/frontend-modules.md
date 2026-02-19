# Frontend Modules

This document provides a comprehensive overview of the React-based frontend architecture for MediSureAI, covering all user interfaces, state management, API integrations, and shared components.

---

## Frontend Architecture Overview

MediSureAI's frontend is built with modern web technologies optimized for performance, maintainability, and user experience:

**Core Technologies**:
- **React 18**: Component-based UI library with hooks
- **JavaScript (JSX)**: All components and files use .js/.jsx extensions (no TypeScript)
- **Vite**: Lightning-fast dev server and optimized production builds
- **Tailwind CSS**: Utility-first CSS framework for responsive design
- **Zustand**: Lightweight state management (alternative to Redux)
- **React Router v6**: Client-side routing with role-based guards
- **Axios**: HTTP client with interceptors for auth and error handling

**Architecture Pattern**: Feature-based modular structure

```
src/
├── pages/           # Page-level components (one per route)
├── components/      # Reusable UI components
├── api/             # API service layer (Axios)
├── hooks/           # Custom React hooks
├── store/           # Zustand state management
├── types/           # TypeScript type definitions
├── utils/           # Helper functions and constants
├── config/          # Configuration files
└── App.tsx          # Root component with routing
```

---

## 1. Patient Portal

### Purpose
Provide patients with a comprehensive interface to manage their insurance policies, submit claims, track claim status, understand coverage, and receive AI-powered explanations of medical bills and decisions.

### Key Pages

#### PatientDashboard
**Route**: /patient/dashboard

**Purpose**: Central hub showing overview of patient's health insurance status

**Features**:
- Active policy summary cards (coverage amount, expiry date, premium)
- Recent claims list with status badges
- Quick action buttons (Submit Claim, Upload Policy, View Coverage)
- Notifications for pending actions (policy expiring soon, claim updates)
- Coverage utilization visualization (progress bars for annual limits)
- Upcoming treatment reminders

**Data Consumed**:
- GET /api/v1/policies/patient/:patientId - Active policies
- GET /api/v1/claims/patient/:patientId - Recent claims
- GET /api/v1/treatments/patient/:patientId - Upcoming treatments

**State Management**: Uses patientDashboardStore for caching dashboard data

**User Interactions**:
- Click on policy card → Navigate to policy details
- Click on claim → View claim details with decision
- Click "Submit Claim" → Navigate to claim submission form
- Click "Upload Policy" → Open policy upload modal

---

#### PolicyAnalyzer
**Route**: /patient/policy-analyzer

**Purpose**: Help patients understand their policy coverage through AI-powered analysis

**Features**:
- Policy document viewer (embedded PDF)
- Natural language query interface: "Is my diabetes treatment covered?"
- AI-powered Q&A using RAG retrieval from uploaded policy
- Coverage summary cards (OPD, IPD, maternity, dental, etc.)
- Exclusions list with explanations
- Co-payment breakdown by treatment category
- Annual limit tracker with visual progress
- Network hospital finder by location

**Data Consumed**:
- GET /api/v1/policies/:policyId - Policy details
- GET /api/v1/policies/:policyId/coverage - Coverage breakdown
- POST /api/v1/policies/analyze - AI-powered coverage analysis

**Components Used**:
- PolicyUploader (for new policy upload)
- CoverageBreakdown (visual coverage details)
- QueryInterface (natural language Q&A)

**User Interactions**:
- Ask question about coverage → API call to analyze → Display AI response with policy clause citations
- View coverage category → Expand to see detailed sub-limits
- Search network hospitals → Filter by city and specialty

---

#### ClaimSubmission
**Route**: /patient/claim/submit

**Purpose**: Guide patients through structured claim submission process

**Features**:
- Multi-step form with progress indicator
- Step 1: Select policy (from patient's active policies)
- Step 2: Select treatment (from patient's treatment history or add new)
- Step 3: Enter claim amount and attach documents
- Step 4: Review and submit
- Validation at each step (required fields, amount limits)
- Document upload (medical bills, prescriptions, discharge summary)
- Estimated coverage calculation (real-time preview)
- Pre-submission warnings (e.g., "Annual limit 80% utilized")

**Data Consumed**:
- GET /api/v1/policies/patient/:patientId - Policies to select from
- GET /api/v1/treatments/patient/:patientId - Treatments to select from
- POST /api/v1/claims/submit - Submit claim

**Components Used**:
- ClaimForm (multi-step form component)
- FileUploader (drag-and-drop document upload)
- CoverageEstimator (real-time coverage calculation preview)

**User Interactions**:
- Fill form step-by-step → Validation → Review → Submit
- On submit → Shows loading spinner → Agent processes claim → Navigate to decision result page

---

#### ClaimStatus
**Route**: /patient/claims/:claimId

**Purpose**: Display detailed claim information with real-time status and AI decision

**Features**:
- Claim status badge (PENDING, APPROVED, REJECTED, PARTIAL)
- Claim timeline (submitted → under review → decided)
- Claim amount vs approved amount comparison
- AI decision display with full explainability:
  - Decision outcome with clear messaging
  - Reasoning paragraph explaining why
  - Policy clause citations (clickable to view clause)
  - Confidence score visualization (percentage with color coding)
  - Warnings list (if any)
  - Suggested actions for patient
- Patient-friendly summary in plain language
- Download options (decision PDF, claim receipt)
- Appeal button if rejected

**Data Consumed**:
- GET /api/v1/claims/:claimId - Claim details
- GET /api/v1/decisions/claim/:claimId - Decision with explanation

**Components Used**:
- DecisionResult (main decision display)
- ReasoningTrace (step-by-step agent thought process)
- ConfidenceScore (visual confidence indicator)
- CitationsList (policy clause references)
- WarningsList (alerts and warnings)

**User Interactions**:
- View decision → Expand reasoning section → Click citation to view source clause
- Low confidence or rejection → Click "Request Appeal" → Opens appeal form
- Submit feedback (thumbs up/down) on decision helpfulness

---

#### BillExplainer
**Route**: /patient/bill-explainer

**Purpose**: Help patients understand complex hospital bills with AI-powered breakdowns

**Features**:
- Bill upload interface (PDF/image accepted)
- AI-powered bill parsing
- Line-item breakdown with explanations
- Category grouping (room charges, doctor fees, pharmacy, procedures, diagnostics)
- Coverage mapping: Which items are covered vs not covered
- Visual breakdown (pie chart of bill components)
- Comparison: Billed amount vs covered amount vs patient responsibility
- Detailed explanations for common confusions (e.g., "Why is room rent capped?")
- Savings tips ("Using network hospital would save you ₹X")

**Data Consumed**:
- POST /api/v1/billing/analyze - Upload and analyze bill
- GET /api/v1/policies/:policyId/coverage - Map bill to policy

**Components Used**:
- FileUploader (bill upload)
- BillBreakdownTable (itemized list)
- CategoryChart (visual breakdown)
- CoverageMapper (what's covered explanation)

**User Interactions**:
- Upload bill → AI analyzes → Display breakdown with explanations
- Hover over line item → Tooltip explains what it is
- Click "Why not covered?" → Modal explains exclusion reason

---

### Shared Features Across Patient Portal
- Responsive design (desktop, tablet, mobile)
- Accessibility compliance (ARIA labels, keyboard navigation)
- Real-time notifications for claim status updates
- Consistent navigation (sidebar with active page highlighting)
- Quick access toolbar (profile, notifications, help)

---

## 2. Doctor Dashboard

### Purpose
Empower doctors with clinical decision support tools including treatment validation, drug safety checking, guideline compliance, and discharge instruction generation.

### Key Pages

#### DoctorDashboard
**Route**: /doctor/dashboard

**Purpose**: Central hub for doctor's clinical activities

**Features**:
- Patient list (recent patients, upcoming appointments)
- Pending treatment validations
- Drug safety alerts summary
- Recent treatment history
- Quick action buttons (Validate Treatment, Check Drug Interaction)
- Statistics: Total patients, treatments validated, guideline compliance rate

**Data Consumed**:
- GET /api/v1/treatments/doctor/:doctorId - Doctor's treatments
- GET /api/v1/patients/doctor/:doctorId - Doctor's patient list

**User Interactions**:
- Click patient → View patient details with treatment history
- Click "Validate Treatment" → Navigate to treatment validation form

---

#### TreatmentValidation
**Route**: /doctor/treatment/validate

**Purpose**: Validate treatment plans against clinical best practices using AI

**Features**:
- Treatment plan entry form (diagnosis, medications, procedures)
- Patient context input (age, conditions, allergies)
- AI-powered guideline validation
- Compliance report display:
  - Overall compliance status (COMPLIANT, NON_COMPLIANT, PARTIAL)
  - Deviations from guidelines with severity levels
  - Evidence-based recommendations
  - Guideline references with links
- Treatment cost estimator
- Alternative treatment suggestions if non-compliant
- Generate patient education materials

**Data Consumed**:
- POST /api/v1/treatments/validate - Validate treatment plan
- GET /api/v1/guidelines/search - Search relevant guidelines

**Components Used**:
- TreatmentForm (structured input)
- GuidelineViewer (display clinical guidelines)
- ComplianceReport (validation results)

**User Interactions**:
- Enter treatment plan → Click "Validate" → AI checks against guidelines
- View deviations → Adjust treatment → Re-validate
- Treatment compliant → Click "Save and Prescribe" → Creates treatment record

---

#### DrugSafetyChecker
**Route**: /doctor/drug-check

**Purpose**: Check drug interactions and contraindications in real-time

**Features**:
- Medication list builder (add multiple drugs with dosages)
- Patient profile input (age, weight, conditions, allergies)
- Real-time interaction checking as drugs are added
- Interaction severity matrix (drug1 ↔ drug2 with severity levels)
- Contraindication alerts with explanations
- Alternative medication suggestions
- Pregnancy/lactation safety information
- Dosage recommendations based on patient factors
- Export interaction report PDF

**Data Consumed**:
- POST /api/v1/treatments/drug-check - Check interactions
- GET /api/v1/drugs/search - Drug name autocomplete

**Components Used**:
- MedicationInput (drug list builder)
- InteractionMatrix (visual interaction display)
- SafetyAlerts (warnings and contraindications)

**User Interactions**:
- Add drug to list → Auto-check interactions → Display warnings
- High-severity interaction found → Alert modal → Suggest alternative
- Click drug name → View drug monograph

---

#### PatientRecords
**Route**: /doctor/patients/:patientId

**Purpose**: Comprehensive patient record view with treatment history

**Features**:
- Patient demographics and medical history
- Chronological treatment timeline
- Medication history with adherence tracking
- Diagnostic results and lab reports
- Insurance coverage summary
- Previous decisions and claim statuses
- Add new treatment record
- Generate discharge summary

**Data Consumed**:
- GET /api/v1/patients/:patientId - Patient details
- GET /api/v1/treatments/patient/:patientId - Treatment history
- GET /api/v1/claims/patient/:patientId - Insurance claims

---

### Shared Features Across Doctor Dashboard
- EMR integration ready (HL7 FHIR compatible)
- E-prescription generation
- Clinical guideline quick reference panel
- Drug database search with autocomplete
- Secure messaging with patients (future scope)

---

## 3. Admin Panel

### Purpose
Hospital administrators and insurance claim reviewers can monitor system activity, review claims, audit decisions, configure rules, and generate reports.

### Key Pages

#### AdminDashboard
**Route**: /admin/dashboard

**Purpose**: System-wide overview and analytics

**Features**:
- Key metrics cards (total claims, approval rate, avg processing time)
- Claims pipeline visualization (funnel chart: submitted → under review → decided)
- Decision confidence distribution (histogram)
- Policy coverage utilization trends (line chart over time)
- Recent activity feed
- System health indicators (API response time, agent latency, RAG retrieval quality)
- Alerts for issues (low-confidence decisions, processing errors)

**Data Consumed**:
- GET /api/v1/admin/metrics - System-wide metrics
- GET /api/v1/admin/activity - Recent activity log

**User Interactions**:
- Click metric card → Drill down to detailed view
- Click alert → Navigate to issue details

---

#### ClaimReview
**Route**: /admin/claims/review

**Purpose**: Manual review queue for flagged claims

**Features**:
- Claims table with filters (status, confidence level, date range)
- Priority queue (low-confidence decisions at top)
- Claim detail panel with:
  - Full claim info
  - AI decision with reasoning
  - Policy clauses retrieved
  - Tool execution log
  - Confidence score breakdown
- Admin actions: APPROVE, REJECT, REQUEST_MORE_INFO
- Override AI decision with reason (audit trail)
- Batch review mode

**Data Consumed**:
- GET /api/v1/admin/claims - Claims pending review
- PUT /api/v1/claims/:claimId/status - Update claim status
- POST /api/v1/decisions/:decisionId/override - Override AI decision

**User Interactions**:
- Filter claims by confidence < 70% → Review each → Approve/Reject/Request info
- Override AI → Enter reason → Decision updated with audit trail

---

#### AuditTrail
**Route**: /admin/audit

**Purpose**: Complete audit log of system actions for compliance

**Features**:
- Searchable audit log table
- Filters: user, action type, entity type, date range
- Export audit logs (CSV, JSON)
- Detailed view for each audit entry:
  - Who performed action
  - What action was performed
  - When it happened
  - Entity affected
  - Before/after values (for updates)
- Compliance reports (for regulatory requirements)

**Data Consumed**:
- GET /api/v1/admin/audit-logs - Audit logs with pagination
- POST /api/v1/admin/audit-logs/export - Export logs

**User Interactions**:
- Search logs by user email → View all actions by that user
- Filter by action "OVERRIDE_DECISION" → See all manual overrides

---

#### PolicyManagement
**Route**: /admin/policies

**Purpose**: Manage policy templates, coverage rules, and policy conflicts

**Features**:
- Policy list with search and filter
- Policy template editor (define standard coverage rules)
- Coverage rule builder (visual editor for co-pay, limits, exclusions)
- Policy conflict detector (overlapping coverage between policies)
- RAG ingestion status monitor (which policies indexed, ingestion errors)
- Re-trigger policy ingestion for failed documents

**Data Consumed**:
- GET /api/v1/admin/policies - All policies
- PUT /api/v1/policies/:policyId - Update policy metadata
- POST /api/v1/rag/reingest - Re-trigger RAG ingestion

---

### Shared Features Across Admin Panel
- Role-based access control (only ADMIN role can access)
- Real-time system monitoring
- Report generation and export
- Configuration management
- User management (future: add/remove users, assign roles)

---

## 4. API Layer

### Purpose
Provide a clean abstraction over HTTP communication with the backend, handle authentication, error handling, and request/response transformation.

### File Structure
```
src/api/
├── claimApi.ts
├── treatmentApi.ts
├── policyApi.ts
├── decisionApi.ts
├── authApi.ts
└── adminApi.ts
```

### claimApi.ts

**Functions**:
- submitClaim(data: ClaimRequest): Promise<ClaimResponse>
- getClaimById(claimId: string): Promise<ClaimDetailDTO>
- getClaimsByPatient(patientId: string): Promise<ClaimResponse[]>
- updateClaimStatus(claimId: string, status: ClaimStatus): Promise<void>
- searchClaims(criteria: ClaimSearchCriteria): Promise<ClaimResponse[]>

### treatmentApi.ts

**Functions**:
- createTreatment(data: TreatmentRequest): Promise<TreatmentResponse>
- validateTreatment(data: TreatmentValidationRequest): Promise<ValidationReport>
- checkDrugInteractions(data: DrugCheckRequest): Promise<DrugCheckResponse>
- getTreatmentById(treatmentId: string): Promise<TreatmentDetailDTO>
- getTreatmentsByPatient(patientId: string): Promise<TreatmentResponse[]>

### policyApi.ts

**Functions**:
- uploadPolicy(file: File, metadata: PolicyMetadata): Promise<PolicyResponse>
- getPolicyById(policyId: string): Promise<PolicyDetailDTO>
- getPoliciesByPatient(patientId: string): Promise<PolicyResponse[]>
- analyzeCoverage(data: CoverageAnalysisRequest): Promise<CoverageBreakdown>
- getCoverageBreakdown(policyId: string): Promise<CoverageBreakdown>

### decisionApi.ts

**Functions**:
- generateDecision(data: DecisionRequest): Promise<DecisionResponse>
- getDecisionById(decisionId: string): Promise<DecisionWithExplanation>
- getDecisionsByClaim(claimId: string): Promise<DecisionResponse[]>
- submitFeedback(decisionId: string, feedback: FeedbackRequest): Promise<void>

---

## 5. State Management (Zustand)

### Purpose
Manage global and feature-specific state with minimal boilerplate using Zustand.

### File Structure
```
src/store/
├── authStore.ts         # Authentication state
├── claimStore.ts        # Claims state
├── decisionStore.ts     # Decisions state
└── policyStore.ts       # Policies state
```

### authStore.ts

**State**:
- user: UserDTO | null (current logged-in user)
- token: string | null (JWT access token)
- refreshToken: string | null
- isAuthenticated: boolean
- isLoading: boolean

**Actions**:
- login(email, password): Authenticate user, store token
- logout(): Clear auth state, redirect to login
- refreshAuthToken(): Refresh expired token
- setUser(user): Update user info

**Persistence**: Token stored in localStorage for persistence across sessions

**Usage**:
```
import { useAuthStore } from '@/store/authStore';

const { user, login, logout } = useAuthStore();
```

### claimStore.ts

**State**:
- claims: Claim[] (list of user's claims)
- activeClaim: Claim | null (currently viewing claim)
- isLoading: boolean
- error: string | null

**Actions**:
- submitClaim(data): Submit new claim, add to claims list
- fetchClaims(): Load user's claims from API
- setActiveClaim(claim): Set currently viewing claim
- updateClaimStatus(claimId, status): Update claim status

**Usage**:
```
import { useClaimStore } from '@/store/claimStore';

const { claims, submitClaim, fetchClaims } = useClaimStore();
```

### decisionStore.ts

**State**:
- decisions: Decision[] (list of decisions)
- activeDecision: DecisionWithExplanation | null
- isLoading: boolean

**Actions**:
- fetchDecision(decisionId): Load decision details
- fetchDecisionsByClaim(claimId): Load all decisions for a claim

### policyStore.ts

**State**:
- policies: Policy[] (user's policies)
- activePolicy: Policy | null
- isLoading: boolean

**Actions**:
- uploadPolicy(file, metadata): Upload new policy
- fetchPolicies(): Load user's policies
- setActivePolicy(policy): Set currently viewing policy

---

## 6. Shared Components

### Purpose
Reusable UI components used across multiple pages and dashboards.

### Common Components

#### Navbar
**Location**: src/components/common/Navbar.tsx

**Purpose**: Top navigation bar with user info and actions

**Features**:
- Logo / app name
- User profile dropdown (name, role, profile link, logout)
- Notification bell with badge count
- Search bar (global search for claims, policies)
- Responsive (hamburger menu on mobile)

**Props**: user (UserDTO), notificationCount (number)

---

#### Sidebar
**Location**: src/components/common/Sidebar.tsx

**Purpose**: Side navigation for dashboard pages

**Features**:
- Role-based navigation items (different for PATIENT, DOCTOR, ADMIN)
- Active page highlighting
- Collapsible on mobile
- Icons for each nav item

**Props**: role (UserRole), activePage (string)

**Navigation Items**:
- PATIENT: Dashboard, Policies, Claims, Bill Explainer
- DOCTOR: Dashboard, Patients, Validate Treatment, Drug Check
- ADMIN: Dashboard, Claim Review, Audit Trail, Policy Management

---

#### LoadingSpinner
**Location**: src/components/common/LoadingSpinner.tsx

**Purpose**: Consistent loading indicator

**Features**:
- Animated spinner icon
- Optional loading message
- Full-screen overlay mode or inline mode

**Props**: message (string, optional), fullScreen (boolean)

---

#### ExplanationCard
**Location**: src/components/common/ExplanationCard.tsx

**Purpose**: Display AI-generated explanations with expandable sections

**Features**:
- Title with icon
- Collapsible content
- Color-coded based on type (success, warning, error, info)
- Citation links

**Props**: title (string), content (string), type (string), citations (array)

---

### Claim Components

#### ClaimForm
**Location**: src/components/claim/ClaimForm.tsx

**Purpose**: Multi-step form for claim submission

**Features**:
- Step progress indicator
- Form validation with error messages
- File upload for supporting documents
- Real-time coverage estimation
- Review step before submission

**Props**: onSubmit (function), initialData (object, optional)

---

#### ClaimStatus
**Location**: src/components/claim/ClaimStatus.tsx

**Purpose**: Display claim status with timeline

**Features**:
- Status badge with color coding
- Timeline visualization (submitted → reviewed → decided)
- Date stamps for each stage
- Link to view decision

**Props**: claim (Claim)

---

#### ClaimHistory
**Location**: src/components/claim/ClaimHistory.tsx

**Purpose**: Table view of user's claim history

**Features**:
- Sortable columns (date, amount, status)
- Filterable by status
- Pagination
- Click row to view details

**Props**: claims (Claim[]), onClaimClick (function)

---

### Treatment Components

#### TreatmentForm
**Location**: src/components/treatment/TreatmentForm.tsx

**Purpose**: Input form for treatment details

**Features**:
- Diagnosis input with ICD-10 code lookup
- Medication list builder (add/remove drugs)
- Procedure input with CPT code lookup
- Date picker for treatment date
- Cost estimator

**Props**: onSubmit (function), patientId (string)

---

#### GuidelineViewer
**Location**: src/components/treatment/GuidelineViewer.tsx

**Purpose**: Display clinical guidelines with search

**Features**:
- Formatted guideline text
- Search within guideline
- Highlight relevant sections
- Reference citations

**Props**: guideline (Guideline), query (string, optional)

---

### Decision Components

#### DecisionResult
**Location**: src/components/decision/DecisionResult.tsx

**Purpose**: Main component for displaying AI decision with full explainability

**Features**:
- Decision outcome badge (APPROVED, REJECTED, PARTIAL)
- Reasoning paragraph
- Expandable sections: Reasoning Trace, Citations, Warnings, Suggested Actions
- Confidence score visualization
- Patient-friendly summary
- Feedback buttons (thumbs up/down)
- Appeal button (if rejected)

**Props**: decision (DecisionWithExplanation), onFeedback (function), onAppeal (function)

**Sub-components**:
- ConfidenceScore: Visual confidence indicator
- ReasoningTrace: Step-by-step thought process
- CitationsList: Policy clause references
- WarningsList: Alerts and warnings

---

#### ConfidenceScore
**Location**: src/components/decision/ConfidenceScore.tsx

**Purpose**: Visualize AI confidence level

**Features**:
- Circular progress indicator or linear progress bar
- Color-coded: Green (>80%), Yellow (60-80%), Red (<60%)
- Percentage text
- Tooltip explaining confidence factors

**Props**: score (number 0-100), showBreakdown (boolean)

---

#### ReasoningTrace
**Location**: src/components/decision/ReasoningTrace.tsx

**Purpose**: Display agent's step-by-step reasoning process

**Features**:
- Expandable accordion showing each agent step
- Timestamps for each step
- Tool execution results
- Observation and validation checkpoints

**Props**: trace (ReasoningStep[])

---

### Policy Components

#### PolicyUploader
**Location**: src/components/policy/PolicyUploader.tsx

**Purpose**: Drag-and-drop policy document upload

**Features**:
- Drag-and-drop file area
- File type validation (PDF, DOCX only)
- File size limit (10MB max)
- Upload progress bar
- Metadata input (policy number, provider, dates)

**Props**: onUpload (function), acceptedFormats (array)

---

#### CoverageBreakdown
**Location**: src/components/policy/CoverageBreakdown.tsx

**Purpose**: Visual display of policy coverage details

**Features**:
- Coverage category cards (OPD, IPD, Maternity, etc.)
- Sub-limits with progress bars
- Co-payment percentages
- Exclusions list
- Network benefits

**Props**: coverage (CoverageBreakdown)

---

## 7. Custom Hooks

### Purpose
Encapsulate reusable logic and side effects in custom React hooks.

### useAuth.ts

**Purpose**: Authentication logic and user session management

**Hooks**:
- useAuth(): Returns auth state and methods
- useRequireAuth(role): Redirect if not authenticated or lacks role
- useCurrentUser(): Returns current user info

**Usage**:
```
const { user, isAuthenticated, login, logout } = useAuth();
```

### useClaim.ts

**Purpose**: Claim-related data fetching and mutations

**Hooks**:
- useClaims(patientId): Fetches and caches claims
- useClaim(claimId): Fetches single claim with decision
- useSubmitClaim(): Returns mutation function for submitting claims

**Usage**:
```
const { data: claims, isLoading, error } = useClaims(patientId);
const { mutate: submitClaim } = useSubmitClaim();
```

### useDecision.ts

**Purpose**: Decision data fetching

**Hooks**:
- useDecision(decisionId): Fetches decision with explanation
- useDecisionsByClaim(claimId): Fetches all decisions for a claim

### usePolicy.ts

**Purpose**: Policy data and coverage analysis

**Hooks**:
- usePolicies(patientId): Fetches user's policies
- usePolicy(policyId): Fetches single policy details
- useCoverageAnalysis(policyId, treatment): Analyzes coverage for treatment

---

## 8. TypeScript Types

### Purpose
Define type-safe interfaces for data structures used across the frontend.

### File Structure
```
src/types/
├── claimTypes.ts
├── policyTypes.ts
├── treatmentTypes.ts
└── decisionTypes.ts
```

### claimTypes.ts

**Exports**:
- ClaimStatus (enum: PENDING, APPROVED, REJECTED, PARTIAL)
- Claim (interface)
- ClaimRequest (interface)
- ClaimResponse (interface)
- ClaimDetailDTO (interface)
- ClaimSearchCriteria (interface)

### policyTypes.ts

**Exports**:
- PolicyStatus (enum: ACTIVE, EXPIRED, SUSPENDED)
- Policy (interface)
- PolicyMetadata (interface)
- CoverageBreakdown (interface)
- CoverageCategory (interface)

### treatmentTypes.ts

**Exports**:
- TreatmentStatus (enum: PLANNED, IN_PROGRESS, COMPLETED)
- Treatment (interface)
- TreatmentRequest (interface)
- ValidationReport (interface)
- DrugCheckResponse (interface)

### decisionTypes.ts

**Exports**:
- DecisionOutcome (enum: APPROVED, REJECTED, PARTIAL)
- DecisionType (enum: CLAIM_ELIGIBILITY, TREATMENT_VALIDATION, etc.)
- Decision (interface)
- DecisionWithExplanation (interface)
- Citation (interface)
- ReasoningStep (interface)

---

## 9. Utilities

### formatters.ts

**Functions**:
- formatCurrency(amount): Format number as currency (₹ symbol, commas)
- formatDate(date): Format date as "Feb 19, 2026"
- formatDateTime(date): Format with time "Feb 19, 2026, 2:30 PM"
- formatConfidenceScore(score): Format as percentage with color class

### validators.ts

**Functions**:
- validateEmail(email): Check valid email format
- validatePhoneNumber(phone): Check valid phone format
- validatePolicyNumber(policyNumber): Check policy number format
- validateClaimAmount(amount, maxAmount): Check amount in valid range

### constants.ts

**Exports**:
- API_BASE_URL: Backend API URL
- USER_ROLES: Enum of user roles
- CLAIM_STATUSES: Enum of claim statuses
- DECISION_OUTCOMES: Enum of decision outcomes
- FILE_SIZE_LIMIT: Max upload file size
- ACCEPTED_FILE_TYPES: Allowed file types for upload

---

## Frontend Data Flow Example

**Scenario**: Patient submits a claim

1. **User Action**: Patient fills ClaimForm and clicks Submit
2. **Form Validation**: ClaimForm validates inputs client-side
3. **API Call**: ClaimForm calls `useSubmitClaim` hook
4. **API Service**: Hook calls `claimApi.submitClaim(data)`
5. **Axios**: Request sent to backend with JWT token in header
6. **Backend Processing**: Claim created, agent generates decision
7. **Response**: Backend returns ClaimResponse
8. **State Update**: claimStore adds new claim to claims list
9. **Navigation**: User redirected to ClaimStatus page with new claimId
10. **Decision Fetch**: ClaimStatus page calls `useDecision(claimId)`
11. **Display**: DecisionResult component renders decision with explainability

**Total User Experience**: Submit form → Loading indicator (2-5 seconds) → View decision with explanation

---

## Responsive Design Strategy

All components follow mobile-first responsive design:

**Breakpoints** (Tailwind):
- sm: 640px (mobile)
- md: 768px (tablet)
- lg: 1024px (desktop)
- xl: 1280px (large desktop)

**Responsive Patterns**:
- Sidebar collapsed to hamburger menu on mobile
- Multi-column layouts stack on mobile
- Tables convert to card view on mobile
- Font sizes scale down on small screens
- Touch-friendly button sizes on mobile

---

## Accessibility Considerations

- Semantic HTML (proper heading hierarchy, nav, main, footer)
- ARIA labels on interactive elements
- Keyboard navigation support (tab order, focus indicators)
- Color contrast ratios meet WCAG AA standards
- Screen reader friendly (alt text on images, descriptive link text)
- Form labels properly associated with inputs
- Error messages announced to screen readers

---

This modular frontend architecture ensures a consistent, maintainable, and scalable user interface for all MediSureAI users.
