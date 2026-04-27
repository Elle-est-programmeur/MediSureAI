import { useNavigate, useLocation, Link } from "react-router-dom";
import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { useAuth } from "../context/AuthContext";

const DOCTOR_LINKS = [
  { label: "Dashboard", path: "/doctor/dashboard" },
  { label: "My Patients", path: "/doctor/patients" },
  { label: "New Record", path: "/doctor/records/new" },
  { label: "Billing", path: "/doctor/billing/new" },
];

const PATIENT_LINKS = [
  { label: "Dashboard", path: "/patient/dashboard" },
  { label: "My Records", path: "/patient/records" },
  { label: "Billing", path: "/patient/billing" },
  { label: "AI Chat", path: "/patient/chat" },
  { label: "Documents", path: "/patient/documents" },
];

function getInitials(name) {
  if (!name) return "?";
  return name
    .split(" ")
    .map((w) => w[0])
    .join("")
    .toUpperCase()
    .slice(0, 2);
}

export default function Navbar() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, isAuthenticated, logout } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);

  const handleLogout = () => {
    logout();
    setMobileOpen(false);
    navigate("/");
  };

  const links = user?.role === "DOCTOR" ? DOCTOR_LINKS : user?.role === "PATIENT" ? PATIENT_LINKS : [];

  const isActive = (path) => location.pathname === path || location.pathname.startsWith(path + "/");

  const roleBadgeColor = user?.role === "DOCTOR" ? "from-emerald-500 to-teal-600" : "from-cyan-500 to-blue-600";

  return (
    <nav className="shrink-0 w-full h-16 flex items-center justify-between px-4 sm:px-8 z-50 bg-slate-900/95 backdrop-blur-xl border-b border-white/10 relative">
      {/* ── Brand ── */}
      <div
        className="flex items-center gap-2 cursor-pointer select-none"
        onClick={() => navigate("/")}
      >
        <span className="text-xl">🏥</span>
        <span
          className="text-lg font-bold text-white tracking-tight"
          style={{ fontFamily: "Orbitron, sans-serif" }}
        >
          MediSure AI
        </span>
      </div>

      {/* ── Desktop nav links ── */}
      {isAuthenticated && links.length > 0 && (
        <div className="hidden md:flex items-center gap-1">
          {links.map((link) => (
            <Link
              key={link.path}
              to={link.path}
              className={`relative px-3 py-2 text-sm font-medium rounded-lg transition-colors ${
                isActive(link.path)
                  ? "text-cyan-400 bg-white/5"
                  : "text-slate-400 hover:text-white hover:bg-white/5"
              }`}
            >
              {link.label}
              {isActive(link.path) && (
                <motion.div
                  layoutId="nav-indicator"
                  className="absolute bottom-0 left-2 right-2 h-0.5 bg-gradient-to-r from-cyan-400 to-blue-500 rounded-full"
                  transition={{ type: "spring", stiffness: 300, damping: 30 }}
                />
              )}
            </Link>
          ))}
        </div>
      )}

      {/* ── Right side ── */}
      <div className="flex items-center gap-3">
        {isAuthenticated && user ? (
          <div className="flex items-center gap-3">
            {/* User pill (desktop) */}
            <div className="hidden sm:flex items-center gap-2 bg-white/5 border border-white/10 rounded-full pl-1 pr-3 py-1">
              <div
                className={`w-7 h-7 rounded-full bg-gradient-to-br ${roleBadgeColor} flex items-center justify-center text-white text-[10px] font-bold`}
                style={{ fontFamily: "Orbitron, sans-serif" }}
              >
                {getInitials(user.username)}
              </div>
              <span className="text-sm text-white font-medium">{user.username}</span>
              <span className="text-[10px] px-1.5 py-0.5 rounded-full bg-white/10 text-slate-300 font-semibold tracking-wide uppercase">
                {user.role}
              </span>
            </div>

            {/* Logout (desktop) */}
            <button
              onClick={handleLogout}
              className="hidden sm:block px-3 py-1.5 rounded-lg bg-white/5 border border-white/10 text-slate-400 text-sm hover:text-red-400 hover:border-red-500/30 hover:bg-red-500/10 transition-colors"
            >
              Logout
            </button>

            {/* Hamburger (mobile) */}
            <button
              onClick={() => setMobileOpen(!mobileOpen)}
              className="md:hidden flex flex-col gap-1 p-2"
              aria-label="Menu"
            >
              <motion.span
                animate={mobileOpen ? { rotate: 45, y: 6 } : { rotate: 0, y: 0 }}
                className="w-5 h-0.5 bg-white block"
              />
              <motion.span
                animate={mobileOpen ? { opacity: 0 } : { opacity: 1 }}
                className="w-5 h-0.5 bg-white block"
              />
              <motion.span
                animate={mobileOpen ? { rotate: -45, y: -6 } : { rotate: 0, y: 0 }}
                className="w-5 h-0.5 bg-white block"
              />
            </button>
          </div>
        ) : (
          <div className="flex items-center gap-2">
            <button
              onClick={() => navigate("/login")}
              className="px-4 py-2 rounded-xl bg-white/10 border border-white/20 text-white text-sm font-medium hover:bg-white/20 transition"
            >
              Login
            </button>
            <button
              onClick={() => navigate("/register")}
              className="px-4 py-2 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 text-white text-sm font-semibold hover:scale-105 transition-transform shadow-lg shadow-cyan-500/25"
            >
              Register
            </button>
          </div>
        )}
      </div>

      {/* ── Mobile menu panel ── */}
      <AnimatePresence>
        {mobileOpen && isAuthenticated && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            transition={{ duration: 0.2 }}
            className="absolute top-16 left-0 right-0 bg-slate-900/98 backdrop-blur-xl border-b border-white/10 p-4 flex flex-col gap-1 md:hidden z-50"
          >
            {/* User info */}
            <div className="flex items-center gap-3 mb-3 pb-3 border-b border-white/10">
              <div
                className={`w-9 h-9 rounded-full bg-gradient-to-br ${roleBadgeColor} flex items-center justify-center text-white text-xs font-bold`}
                style={{ fontFamily: "Orbitron, sans-serif" }}
              >
                {getInitials(user?.username)}
              </div>
              <div>
                <div className="text-white text-sm font-medium">{user?.username}</div>
                <div className="text-slate-400 text-xs uppercase tracking-wider">{user?.role}</div>
              </div>
            </div>

            {links.map((link) => (
              <Link
                key={link.path}
                to={link.path}
                onClick={() => setMobileOpen(false)}
                className={`px-4 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                  isActive(link.path)
                    ? "text-cyan-400 bg-white/5 border-l-2 border-cyan-400"
                    : "text-slate-400 hover:text-white hover:bg-white/5"
                }`}
              >
                {link.label}
              </Link>
            ))}

            <button
              onClick={handleLogout}
              className="mt-2 px-4 py-2.5 rounded-lg text-sm font-medium text-red-400 hover:bg-red-500/10 text-left transition-colors"
            >
              Logout
            </button>
          </motion.div>
        )}
      </AnimatePresence>
    </nav>
  );
}