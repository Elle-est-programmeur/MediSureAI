import { useState, useEffect } from "react";
import { useNavigate, Link, useLocation } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";
import { doctorAPI } from "../../services/api";
import LoadingSpinner from "../../components/ui/LoadingSpinner";
import EmptyState from "../../components/ui/EmptyState";

const SIDEBAR_LINKS = [
  { label: "Dashboard", path: "/doctor/dashboard", icon: "📊" },
  { label: "Patients", path: "/doctor/patients", icon: "👥" },
  { label: "New Record", path: "/doctor/records/new", icon: "📝" },
  { label: "Billing", path: "/doctor/billing", icon: "💳" },
];

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.07 } } };
const item = { hidden: { opacity: 0, y: 20 }, show: { opacity: 1, y: 0 } };

export default function DoctorDashboard() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout } = useAuth();
  const { addToast } = useToast();

  const [patients, setPatients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [sidebarOpen, setSidebarOpen] = useState(false);

  useEffect(() => {
    fetchData();
  }, []);

  async function fetchData() {
    try {
      const data = await doctorAPI.getPatients();
      setPatients(Array.isArray(data) ? data : []);
    } catch (err) {
      addToast("Failed to load dashboard data", "error");
    } finally {
      setLoading(false);
    }
  }

  const stats = [
    { label: "Total Patients", value: patients.length, color: "from-cyan-500 to-blue-600", glow: "shadow-cyan-500/20" },
    { label: "Records Today", value: "—", color: "from-emerald-500 to-teal-600", glow: "shadow-emerald-500/20" },
    { label: "Total Billing", value: "—", color: "from-violet-500 to-purple-600", glow: "shadow-violet-500/20" },
    { label: "Active Cases", value: patients.length, color: "from-amber-500 to-orange-600", glow: "shadow-amber-500/20" },
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
      {/* ── Sidebar (desktop) ── */}
      <aside className="hidden lg:flex flex-col w-64 shrink-0 bg-slate-900/80 backdrop-blur-xl border-r border-white/10 p-4">
        {/* Brand */}
        <div className="flex items-center gap-2 px-3 py-4 mb-4">
          <motion.div
            animate={{ scale: [1, 1.1, 1] }}
            transition={{ duration: 2, repeat: Infinity }}
            className="w-8 h-8 rounded-lg bg-gradient-to-br from-cyan-500 to-blue-600 flex items-center justify-center text-white text-sm"
          >
            🏥
          </motion.div>
          <span className="text-sm font-bold text-white" style={{ fontFamily: "Orbitron, sans-serif" }}>
            Doctor Portal
          </span>
        </div>

        {/* Nav items */}
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

        {/* Doctor info */}
        <div className="mt-auto pt-4 border-t border-white/10">
          <div className="px-3 py-2">
            <div className="text-sm font-medium text-white">{user?.username}</div>
            <div className="text-xs text-slate-400">Doctor</div>
          </div>
          <button
            onClick={() => { logout(); navigate("/"); }}
            className="w-full mt-2 px-3 py-2 rounded-xl text-sm text-red-400 hover:bg-red-500/10 text-left transition-colors"
          >
            Logout
          </button>
        </div>
      </aside>

      {/* ── Mobile sidebar toggle ── */}
      <button
        onClick={() => setSidebarOpen(!sidebarOpen)}
        className="lg:hidden fixed bottom-4 left-4 z-50 w-12 h-12 rounded-full bg-gradient-to-r from-cyan-500 to-blue-600 text-white shadow-lg shadow-cyan-500/30 flex items-center justify-center"
      >
        ☰
      </button>

      {/* ── Main content ── */}
      <main className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8">
        {/* Welcome */}
        <div className="mb-8">
          <h1
            className="text-2xl sm:text-3xl font-bold text-white mb-1"
            style={{ fontFamily: "Orbitron, sans-serif" }}
          >
            Welcome, Dr. {user?.username}
          </h1>
          <p className="text-slate-400 text-sm">Here's your practice overview</p>
        </div>

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

        {/* Recent Patients */}
        <div className="mb-8">
          <div className="flex items-center justify-between mb-4">
            <h2
              className="text-xs font-semibold tracking-[0.2em] uppercase text-cyan-400"
              style={{ fontFamily: "Orbitron, sans-serif" }}
            >
              Recent Patients
            </h2>
            <Link to="/doctor/patients" className="text-sm text-slate-400 hover:text-cyan-400 transition-colors">
              View All →
            </Link>
          </div>

          {patients.length === 0 ? (
            <EmptyState
              icon="👥"
              title="No patients yet"
              subtitle="Your patients will appear here once you create medical records."
              actionLabel="Create Record"
              onAction={() => navigate("/doctor/records/new")}
            />
          ) : (
            <motion.div variants={container} initial="hidden" animate="show" className="space-y-2">
              {/* Desktop table */}
              <div className="hidden sm:block bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl overflow-hidden">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-white/10">
                      <th className="text-left px-6 py-3 text-xs font-semibold text-slate-400 uppercase tracking-wider">Patient</th>
                      <th className="text-left px-6 py-3 text-xs font-semibold text-slate-400 uppercase tracking-wider">Age</th>
                      <th className="text-left px-6 py-3 text-xs font-semibold text-slate-400 uppercase tracking-wider">Gender</th>
                      <th className="text-right px-6 py-3 text-xs font-semibold text-slate-400 uppercase tracking-wider">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {patients.slice(0, 5).map((p, i) => (
                      <motion.tr key={p.id || i} variants={item} className="border-b border-white/5 hover:bg-white/5 transition-colors">
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-3">
                            <div className="w-8 h-8 rounded-full bg-gradient-to-br from-cyan-500 to-blue-600 flex items-center justify-center text-white text-xs font-bold">
                              {(p.name || p.username || "?")[0].toUpperCase()}
                            </div>
                            <span className="text-white text-sm font-medium">{p.name || p.username || "Unknown"}</span>
                          </div>
                        </td>
                        <td className="px-6 py-4 text-slate-400 text-sm">{p.age || "—"}</td>
                        <td className="px-6 py-4 text-slate-400 text-sm">{p.gender || "—"}</td>
                        <td className="px-6 py-4 text-right">
                          <button
                            onClick={() => navigate(`/doctor/patients`)}
                            className="text-xs px-3 py-1.5 rounded-lg bg-cyan-500/10 border border-cyan-500/30 text-cyan-400 hover:bg-cyan-500/20 transition-colors"
                          >
                            View Records
                          </button>
                        </td>
                      </motion.tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Mobile cards */}
              <div className="sm:hidden space-y-3">
                {patients.slice(0, 5).map((p, i) => (
                  <motion.div key={p.id || i} variants={item} className="bg-white/5 border border-white/10 rounded-xl p-4">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-full bg-gradient-to-br from-cyan-500 to-blue-600 flex items-center justify-center text-white text-xs font-bold">
                          {(p.name || p.username || "?")[0].toUpperCase()}
                        </div>
                        <div>
                          <div className="text-white text-sm font-medium">{p.name || p.username || "Unknown"}</div>
                          <div className="text-slate-400 text-xs">{p.age || "—"} • {p.gender || "—"}</div>
                        </div>
                      </div>
                      <button
                        onClick={() => navigate(`/doctor/patients`)}
                        className="text-xs px-3 py-1.5 rounded-lg bg-cyan-500/10 text-cyan-400"
                      >
                        View
                      </button>
                    </div>
                  </motion.div>
                ))}
              </div>
            </motion.div>
          )}
        </div>

        {/* Quick Actions */}
        <div>
          <h2
            className="text-xs font-semibold tracking-[0.2em] uppercase text-cyan-400 mb-4"
            style={{ fontFamily: "Orbitron, sans-serif" }}
          >
            Quick Actions
          </h2>
          <div className="flex flex-wrap gap-3">
            <button
              onClick={() => navigate("/doctor/records/new")}
              className="px-6 py-3 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 text-white font-semibold text-sm hover:scale-105 transition-transform shadow-lg shadow-cyan-500/25"
            >
              ✏️ Create Medical Record
            </button>
            <button
              onClick={() => navigate("/doctor/billing/new")}
              className="px-6 py-3 rounded-xl bg-white/10 border border-white/20 text-white font-medium text-sm hover:bg-white/20 transition"
            >
              💳 Add Billing Entry
            </button>
          </div>
        </div>
      </main>
    </div>
  );
}
