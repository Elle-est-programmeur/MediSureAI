import { useState, useEffect } from "react";
import { useNavigate, Link, useLocation } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";
import { patientAPI } from "../../services/api";
import LoadingSpinner from "../../components/ui/LoadingSpinner";

const SIDEBAR_LINKS = [
  { label: "Dashboard", path: "/patient/dashboard", icon: "📊" },
  { label: "My Records", path: "/patient/records", icon: "📋" },
  { label: "Billing", path: "/patient/billing", icon: "💳" },
  { label: "AI Chat", path: "/patient/chat", icon: "🤖" },
  { label: "Documents", path: "/patient/documents", icon: "📁" },
  { label: "Timeline", path: "/patient/timeline", icon: "📅" },
  { label: "Drug Formulary", path: "/patient/formulary", icon: "💊" },
  { label: "Profile", path: "/patient/profile", icon: "👤" },
];

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.07 } } };
const item = { hidden: { opacity: 0, y: 20 }, show: { opacity: 1, y: 0 } };

export default function PatientDashboard() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout } = useAuth();
  const { addToast } = useToast();

  const [records, setRecords] = useState([]);
  const [billing, setBilling] = useState([]);
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [chatInput, setChatInput] = useState("");
  const [chatResponse, setChatResponse] = useState(null);
  const [chatLoading, setChatLoading] = useState(false);
  const [mrnCopied, setMrnCopied] = useState(false);

  useEffect(() => {
    fetchData();
  }, []);

  async function fetchData() {
    try {
      const [recordsData, billingData, profileData] = await Promise.allSettled([
        patientAPI.getRecords(),
        patientAPI.getBilling(),
        patientAPI.getProfile(),
      ]);
      setRecords(recordsData.status === "fulfilled" ? (Array.isArray(recordsData.value) ? recordsData.value : []) : []);
      setBilling(billingData.status === "fulfilled" ? (Array.isArray(billingData.value) ? billingData.value : []) : []);
      setProfile(profileData.status === "fulfilled" ? profileData.value : null);
    } catch {
      addToast("Failed to load dashboard data", "error");
    } finally {
      setLoading(false);
    }
  }

  function copyMRN() {
    if (!profile?.medicalRecordNumber) return;
    navigator.clipboard.writeText(profile.medicalRecordNumber);
    setMrnCopied(true);
    setTimeout(() => setMrnCopied(false), 1500);
  }

  async function handleQuickChat(e) {
    e.preventDefault();
    if (!chatInput.trim()) return;
    setChatLoading(true);
    try {
      const res = await patientAPI.chat(chatInput, crypto.randomUUID());
      setChatResponse(res.response || res.answer || JSON.stringify(res));
    } catch {
      addToast("Chat failed", "error");
    } finally {
      setChatLoading(false);
    }
  }

  const totalBilling = billing.reduce((sum, b) => sum + (b.totalCost || b.amount || 0), 0);

  const stats = [
    { label: "Medical Records", value: records.length, color: "from-cyan-500 to-blue-600", glow: "shadow-cyan-500/20" },
    { label: "Total Billing", value: `₹${totalBilling.toLocaleString()}`, color: "from-emerald-500 to-teal-600", glow: "shadow-emerald-500/20" },
    { label: "Documents", value: "—", color: "from-violet-500 to-purple-600", glow: "shadow-violet-500/20" },
    { label: "AI Queries", value: "—", color: "from-amber-500 to-orange-600", glow: "shadow-amber-500/20" },
  ];

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  return (
    <div className="flex-1 flex overflow-hidden">
      {/* ── Sidebar ── */}
      <aside className="hidden lg:flex flex-col w-64 shrink-0 bg-slate-900/80 backdrop-blur-xl border-r border-white/10 p-4">
        <div className="flex items-center gap-2 px-3 py-4 mb-4">
          <motion.div
            animate={{ scale: [1, 1.1, 1] }}
            transition={{ duration: 2, repeat: Infinity }}
            className="w-8 h-8 rounded-lg bg-gradient-to-br from-cyan-500 to-blue-600 flex items-center justify-center text-white text-sm"
          >
            🧑‍💻
          </motion.div>
          <span className="text-sm font-bold text-white" style={{ fontFamily: "Orbitron, sans-serif" }}>
            Patient Portal
          </span>
        </div>

        <nav className="flex-1 flex flex-col gap-1">
          {SIDEBAR_LINKS.map((link) => {
            const active = location.pathname === link.path;
            return (
              <Link
                key={link.path}
                to={link.path}
                className={`flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-colors ${
                  active
                    ? "bg-white/10 text-cyan-400 border-l-2 border-cyan-400"
                    : "text-slate-400 hover:text-white hover:bg-white/5"
                }`}
              >
                <span>{link.icon}</span>
                {link.label}
              </Link>
            );
          })}
        </nav>

        <div className="mt-auto pt-4 border-t border-white/10">
          <div className="px-3 py-2">
            <div className="text-sm font-medium text-white">{user?.username}</div>
            <div className="text-xs text-slate-400">Patient</div>
          </div>
          <button
            onClick={() => { logout(); navigate("/"); }}
            className="w-full mt-2 px-3 py-2 rounded-xl text-sm text-red-400 hover:bg-red-500/10 text-left transition-colors"
          >
            Logout
          </button>
        </div>
      </aside>

      {/* Mobile sidebar toggle */}
      <button
        onClick={() => setSidebarOpen(!sidebarOpen)}
        className="lg:hidden fixed bottom-4 left-4 z-50 w-12 h-12 rounded-full bg-gradient-to-r from-cyan-500 to-blue-600 text-white shadow-lg shadow-cyan-500/30 flex items-center justify-center"
      >
        ☰
      </button>

      {/* ── Main content ── */}
      <main className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8">
        {/* Welcome Hero */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="relative mb-8 p-6 sm:p-8 rounded-2xl bg-gradient-to-br from-cyan-500/10 to-blue-600/10 border border-white/10 overflow-hidden"
        >
          <div className="absolute inset-0 bg-[radial-gradient(circle_at_30%_50%,_rgba(6,182,212,0.08),_transparent_60%)]" />
          <div className="relative flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <div>
              <h1 className="text-2xl sm:text-3xl font-bold text-white mb-1" style={{ fontFamily: "Orbitron, sans-serif" }}>
                Welcome back, {user?.username}
              </h1>
              <p className="text-slate-400 text-sm">Your health overview at a glance</p>

              {/* Medical Record Number — give this to your doctor */}
              {profile?.medicalRecordNumber && (
                <button
                  onClick={copyMRN}
                  className="mt-3 inline-flex items-center gap-2 px-3 py-1.5 rounded-lg bg-cyan-500/10 border border-cyan-500/30 hover:bg-cyan-500/20 transition-colors group"
                  title="Click to copy"
                >
                  <span className="text-[10px] uppercase tracking-wider text-slate-400">MRN</span>
                  <span className="text-sm font-mono tracking-wider text-cyan-400 font-semibold">
                    {profile.medicalRecordNumber}
                  </span>
                  <span className="text-xs text-slate-500 group-hover:text-cyan-400 transition-colors">
                    {mrnCopied ? "✓ Copied" : "📋"}
                  </span>
                </button>
              )}
            </div>
            {/* Decorative health score ring */}
            <div className="relative w-20 h-20 shrink-0">
              <svg className="w-20 h-20 -rotate-90" viewBox="0 0 80 80">
                <circle cx="40" cy="40" r="34" fill="none" stroke="rgba(255,255,255,0.05)" strokeWidth="6" />
                <motion.circle
                  cx="40" cy="40" r="34" fill="none" stroke="url(#scoreGrad)" strokeWidth="6" strokeLinecap="round"
                  strokeDasharray={`${85 * 2.136} ${(100 - 85) * 2.136}`}
                  initial={{ strokeDashoffset: 213.6 }}
                  animate={{ strokeDashoffset: 0 }}
                  transition={{ duration: 1.5, ease: "easeOut" }}
                />
                <defs>
                  <linearGradient id="scoreGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" stopColor="#06b6d4" />
                    <stop offset="100%" stopColor="#3b82f6" />
                  </linearGradient>
                </defs>
              </svg>
              <div className="absolute inset-0 flex items-center justify-center">
                <span className="text-white text-lg font-bold" style={{ fontFamily: "Orbitron, sans-serif" }}>85</span>
              </div>
            </div>
          </div>
        </motion.div>

        {/* Stats */}
        <motion.div
          variants={container}
          initial="hidden"
          animate="show"
          className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 mb-8"
        >
          {stats.map((stat) => (
            <motion.div
              key={stat.label}
              variants={item}
              whileHover={{ scale: 1.02, y: -2 }}
              className={`bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-6 shadow-lg ${stat.glow}`}
            >
              <p className="text-xs font-semibold tracking-[0.2em] uppercase text-slate-400 mb-2" style={{ fontFamily: "Orbitron, sans-serif" }}>
                {stat.label}
              </p>
              <p className={`text-3xl font-bold bg-gradient-to-r ${stat.color} bg-clip-text text-transparent`} style={{ fontFamily: "Orbitron, sans-serif" }}>
                {stat.value}
              </p>
            </motion.div>
          ))}
        </motion.div>

        {/* Recent Records */}
        <div className="mb-8">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-xs font-semibold tracking-[0.2em] uppercase text-cyan-400" style={{ fontFamily: "Orbitron, sans-serif" }}>
              Recent Records
            </h2>
            <Link to="/patient/records" className="text-sm text-slate-400 hover:text-cyan-400 transition-colors">View All →</Link>
          </div>

          {records.length === 0 ? (
            <div className="bg-white/5 border border-white/10 rounded-2xl p-8 text-center">
              <p className="text-slate-400 text-sm">No medical records yet</p>
            </div>
          ) : (
            <motion.div variants={container} initial="hidden" animate="show" className="space-y-3">
              {records.slice(0, 3).map((rec, i) => (
                <motion.div
                  key={rec.id || i}
                  variants={item}
                  whileHover={{ scale: 1.01 }}
                  className="bg-white/5 border border-white/10 rounded-xl p-4 flex items-center justify-between gap-4 hover:border-cyan-500/30 transition-colors"
                >
                  <div className="min-w-0">
                    <p className="text-white font-medium text-sm truncate">{rec.diagnosis || "No diagnosis"}</p>
                    <p className="text-slate-400 text-xs mt-0.5">
                      {rec.doctorName || "Doctor"} • {rec.createdAt || "—"}
                    </p>
                  </div>
                  <Link
                    to="/patient/records"
                    className="shrink-0 text-xs px-3 py-1.5 rounded-lg bg-cyan-500/10 border border-cyan-500/30 text-cyan-400 hover:bg-cyan-500/20 transition-colors"
                  >
                    Details
                  </Link>
                </motion.div>
              ))}
            </motion.div>
          )}
        </div>

        {/* Billing Summary */}
        <div className="mb-8">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-xs font-semibold tracking-[0.2em] uppercase text-cyan-400" style={{ fontFamily: "Orbitron, sans-serif" }}>
              Billing Summary
            </h2>
            <Link to="/patient/billing" className="text-sm text-slate-400 hover:text-cyan-400 transition-colors">View All →</Link>
          </div>

          {billing.length === 0 ? (
            <div className="bg-white/5 border border-white/10 rounded-2xl p-8 text-center">
              <p className="text-slate-400 text-sm">No billing entries yet</p>
            </div>
          ) : (
            <div className="space-y-3">
              {billing.slice(0, 3).map((b, i) => {
                const maxCost = Math.max(...billing.map((x) => x.totalCost || x.amount || 0), 1);
                const pct = ((b.totalCost || b.amount || 0) / maxCost) * 100;
                return (
                  <div key={b.id || i} className="bg-white/5 border border-white/10 rounded-xl p-4">
                    <div className="flex justify-between items-center mb-2">
                      <span className="text-white text-sm">{b.description || "Billing entry"}</span>
                      <span className="text-white font-bold" style={{ fontFamily: "Orbitron, sans-serif" }}>
                        ₹{(b.totalCost || b.amount || 0).toLocaleString()}
                      </span>
                    </div>
                    <div className="w-full h-1.5 bg-white/5 rounded-full overflow-hidden">
                      <motion.div
                        initial={{ width: 0 }}
                        animate={{ width: `${pct}%` }}
                        transition={{ duration: 0.8, delay: i * 0.1 }}
                        className="h-full bg-gradient-to-r from-cyan-500 to-blue-600 rounded-full"
                      />
                    </div>
                  </div>
                );
              })}

              <button
                onClick={() => navigate("/patient/chat")}
                className="w-full mt-2 py-2.5 rounded-xl bg-gradient-to-r from-cyan-500/10 to-blue-600/10 border border-cyan-500/20 text-cyan-400 text-sm font-medium hover:bg-cyan-500/15 transition-colors"
              >
                🤖 AI Coverage Analysis
              </button>
            </div>
          )}
        </div>

        {/* Quick AI Chat Widget */}
        <div className="bg-white/5 border border-white/10 rounded-2xl p-5">
          <h2 className="text-xs font-semibold tracking-[0.2em] uppercase text-cyan-400 mb-3" style={{ fontFamily: "Orbitron, sans-serif" }}>
            Quick AI Chat
          </h2>

          {chatResponse && (
            <div className="mb-3 p-3 rounded-xl bg-white/5 border border-white/10 text-slate-300 text-sm">
              {chatResponse}
            </div>
          )}

          <form onSubmit={handleQuickChat} className="flex gap-2">
            <input
              type="text"
              value={chatInput}
              onChange={(e) => setChatInput(e.target.value)}
              placeholder="Ask about your records or insurance..."
              className="flex-1 bg-white/5 border border-white/20 rounded-xl px-4 py-2.5 text-white text-sm placeholder-slate-500 focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500 outline-none transition"
            />
            <button
              type="submit"
              disabled={chatLoading}
              className="px-4 py-2.5 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 text-white text-sm font-semibold hover:scale-105 transition-transform shadow-lg shadow-cyan-500/25 disabled:opacity-50"
            >
              {chatLoading ? "..." : "→"}
            </button>
          </form>
          <Link to="/patient/chat" className="text-xs text-slate-400 hover:text-cyan-400 mt-2 inline-block transition-colors">
            Open full chat →
          </Link>
        </div>
      </main>
    </div>
  );
}
