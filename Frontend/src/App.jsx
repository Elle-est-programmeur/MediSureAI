import { Routes, Route, useLocation, Navigate } from "react-router-dom";
import { AnimatePresence, motion } from "framer-motion";
import { useAuth } from "./context/AuthContext";
import Navbar from "./components/Navbar";
import LoadingSpinner from "./components/ui/LoadingSpinner";

import Home from "./pages/Home";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Dashboard from "./pages/Dashboard";

// Doctor pages
import DoctorDashboard from "./pages/doctor/DoctorDashboard";
import DoctorPatients from "./pages/doctor/DoctorPatients";
import CreateRecord from "./pages/doctor/CreateRecord";
import RecordDetail from "./pages/doctor/RecordDetail";
import CreateBilling from "./pages/doctor/CreateBilling";

// Patient pages
import PatientDashboard from "./pages/patient/PatientDashboard";
import PatientRecords from "./pages/patient/PatientRecords";
import PatientBilling from "./pages/patient/PatientBilling";
import PatientChat from "./pages/patient/PatientChat";
import PatientDocuments from "./pages/patient/PatientDocuments";
import PatientTimeline from "./pages/patient/PatientTimeline";
import PatientFormulary from "./pages/patient/PatientFormulary";
import PatientProfile from "./pages/patient/PatientProfile";

// ─── Animated page wrapper ────────────────────────────────────────────────────
function AnimatedPage({ children }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -20 }}
      transition={{ duration: 0.4, ease: [0.22, 1, 0.36, 1] }}
      className="flex-1 w-full min-h-0 flex flex-col relative"
    >
      {children}
    </motion.div>
  );
}

// ─── Route guards ──────────────────────────────────────────────────────────────
function ProtectedRoute({ children }) {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="flex-1 flex items-center justify-center bg-[#0f172a]">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return children;
}

function RoleRoute({ role, children }) {
  const { user, isLoading, getRoleBasedPath } = useAuth();

  if (isLoading) {
    return (
      <div className="flex-1 flex items-center justify-center bg-[#0f172a]">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (user?.role !== role) {
    return <Navigate to={getRoleBasedPath()} replace />;
  }

  return children;
}

// ─── Dashboard redirect ───────────────────────────────────────────────────────
function DashboardRedirect() {
  const { user, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="flex-1 flex items-center justify-center bg-[#0f172a]">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (!user) return <Navigate to="/login" replace />;

  switch (user.role) {
    case "DOCTOR":
      return <Navigate to="/doctor/dashboard" replace />;
    case "PATIENT":
      return <Navigate to="/patient/dashboard" replace />;
    default:
      return <Dashboard />;
  }
}

export default function App() {
  const location = useLocation();

  return (
    <div className="h-screen w-screen flex flex-col overflow-hidden bg-[#0f172a]">
      {/* GLOBAL NAVBAR */}
      <Navbar />

      <AnimatePresence mode="wait">
        <Routes location={location} key={location.pathname}>
          {/* Public routes */}
          <Route path="/" element={<AnimatedPage><Home /></AnimatedPage>} />
          <Route path="/login" element={<AnimatedPage><Login /></AnimatedPage>} />
          <Route path="/register" element={<AnimatedPage><Register /></AnimatedPage>} />

          {/* Legacy dashboard — role redirect */}
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <AnimatedPage>
                  <DashboardRedirect />
                </AnimatedPage>
              </ProtectedRoute>
            }
          />

          {/* ─── Doctor routes ───────────────────────────────────────────── */}
          <Route path="/doctor" element={<Navigate to="/doctor/dashboard" replace />} />
          <Route
            path="/doctor/dashboard"
            element={
              <ProtectedRoute><RoleRoute role="DOCTOR">
                <AnimatedPage><DoctorDashboard /></AnimatedPage>
              </RoleRoute></ProtectedRoute>
            }
          />
          <Route
            path="/doctor/patients"
            element={
              <ProtectedRoute><RoleRoute role="DOCTOR">
                <AnimatedPage><DoctorPatients /></AnimatedPage>
              </RoleRoute></ProtectedRoute>
            }
          />
          <Route
            path="/doctor/records/new"
            element={
              <ProtectedRoute><RoleRoute role="DOCTOR">
                <AnimatedPage><CreateRecord /></AnimatedPage>
              </RoleRoute></ProtectedRoute>
            }
          />
          <Route
            path="/doctor/records/:id"
            element={
              <ProtectedRoute><RoleRoute role="DOCTOR">
                <AnimatedPage><RecordDetail /></AnimatedPage>
              </RoleRoute></ProtectedRoute>
            }
          />
          <Route
            path="/doctor/billing/new"
            element={
              <ProtectedRoute><RoleRoute role="DOCTOR">
                <AnimatedPage><CreateBilling /></AnimatedPage>
              </RoleRoute></ProtectedRoute>
            }
          />

          {/* ─── Patient routes ──────────────────────────────────────────── */}
          <Route path="/patient" element={<Navigate to="/patient/dashboard" replace />} />
          <Route
            path="/patient/dashboard"
            element={
              <ProtectedRoute><RoleRoute role="PATIENT">
                <AnimatedPage><PatientDashboard /></AnimatedPage>
              </RoleRoute></ProtectedRoute>
            }
          />
          <Route
            path="/patient/records"
            element={
              <ProtectedRoute><RoleRoute role="PATIENT">
                <AnimatedPage><PatientRecords /></AnimatedPage>
              </RoleRoute></ProtectedRoute>
            }
          />
          <Route
            path="/patient/billing"
            element={
              <ProtectedRoute><RoleRoute role="PATIENT">
                <AnimatedPage><PatientBilling /></AnimatedPage>
              </RoleRoute></ProtectedRoute>
            }
          />
          <Route
            path="/patient/chat"
            element={
              <ProtectedRoute><RoleRoute role="PATIENT">
                <AnimatedPage><PatientChat /></AnimatedPage>
              </RoleRoute></ProtectedRoute>
            }
          />
          <Route
            path="/patient/documents"
            element={
              <ProtectedRoute><RoleRoute role="PATIENT">
                <AnimatedPage><PatientDocuments /></AnimatedPage>
              </RoleRoute></ProtectedRoute>
            }
          />
          <Route
            path="/patient/timeline"
            element={
              <ProtectedRoute><RoleRoute role="PATIENT">
                <AnimatedPage><PatientTimeline /></AnimatedPage>
              </RoleRoute></ProtectedRoute>
            }
          />
          <Route
            path="/patient/formulary"
            element={
              <ProtectedRoute><RoleRoute role="PATIENT">
                <AnimatedPage><PatientFormulary /></AnimatedPage>
              </RoleRoute></ProtectedRoute>
            }
          />
          <Route
            path="/patient/profile"
            element={
              <ProtectedRoute><RoleRoute role="PATIENT">
                <AnimatedPage><PatientProfile /></AnimatedPage>
              </RoleRoute></ProtectedRoute>
            }
          />
        </Routes>
      </AnimatePresence>
    </div>
  );
}