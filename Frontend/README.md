# 🏥 MediSureAI — Frontend

React single-page app for MediSureAI. It provides role-based portals (Patient & Doctor) over
the Spring Boot backend: document upload, AI chat, medical records, billing & payments, a
patient timeline, and drug-formulary search.

## 🧰 Tech Stack
- **React 19** with hooks and Context API
- **Vite 7** dev server & build
- **Tailwind CSS 4** (via `@tailwindcss/vite`)
- **React Router 7** for routing & role guards
- **Framer Motion** for animations
- **Axios** for the API layer

## ⚡ Quick Start

> Requires **Node.js 20.19+ or 22.12+** (Vite 7).

```bash
cd Frontend
npm install
npm run dev
```

The app runs on `http://localhost:5173` and talks to the backend at `http://localhost:8080`.
Make sure the backend is running first (see [`../Backend/README.md`](../Backend/README.md)).

### Scripts
| Command | Description |
|---------|-------------|
| `npm run dev` | Start the Vite dev server (HMR) |
| `npm run build` | Production build to `dist/` |
| `npm run preview` | Preview the production build |
| `npm run lint` | Run ESLint |

## 🔧 Configuration

The API base URL is configured in `src/services/api.js` (defaults to `http://localhost:8080`).
When running under Docker Compose it is provided via `VITE_API_BASE_URL`. Axios attaches the
JWT access token and handles refresh-token rotation on `401` responses.

## 📁 Project Structure

```
Frontend/src/
├── main.jsx, App.jsx          # Entry point & route definitions
├── pages/
│   ├── Home.jsx, Login.jsx, Register.jsx, Dashboard.jsx
│   ├── doctor/                # DoctorDashboard, DoctorPatients, RecordDetail,
│   │                          # CreateRecord, CreateBilling, DoctorBilling
│   └── patient/               # PatientDashboard, PatientChat, PatientDocuments,
│                              # PatientRecords, PatientBilling, PatientTimeline,
│                              # PatientProfile, PatientFormulary
├── components/
│   ├── ChatInterface.jsx, ChatThinkingBadge.jsx
│   ├── DocumentUpload.jsx, FormularySearch.jsx, Timeline.jsx
│   ├── Navbar.jsx, ParticleBackground.jsx, PaymentModal.jsx
│   └── ui/                    # ConfirmDialog, EmptyState, LoadingSpinner, StatusBadge
├── context/                   # AuthContext, ChatContext, ThemeContext, ToastContext
├── services/api.js            # Axios client + endpoint wrappers
└── styles/, *.css             # Tailwind entry + component styles
```

## 🔐 Auth & Roles
- `AuthContext` stores the authenticated user and tokens, exposing `login`, `register`, and
  `logout`.
- Routes are guarded by role: **PATIENT** and **DOCTOR** see different dashboards and pages.
- The backend enforces the same roles via `@PreAuthorize`, so the UI guards are convenience,
  not the security boundary.

## 🩺 Troubleshooting
Common dev-server, port, Node-version, and API-connectivity issues are documented in
[`TROUBLESHOOTING.md`](TROUBLESHOOTING.md).
