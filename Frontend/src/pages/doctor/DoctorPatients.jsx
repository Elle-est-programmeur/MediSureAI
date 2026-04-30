import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { motion, AnimatePresence } from "framer-motion";
import { useToast } from "../../context/ToastContext";
import { doctorAPI } from "../../services/api";
import LoadingSpinner from "../../components/ui/LoadingSpinner";
import EmptyState from "../../components/ui/EmptyState";

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.07 } } };
const item = { hidden: { opacity: 0, y: 20 }, show: { opacity: 1, y: 0 } };

export default function DoctorPatients() {
  const navigate = useNavigate();
  const { addToast } = useToast();
  const [patients, setPatients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [selectedPatient, setSelectedPatient] = useState(null);
  const [patientRecords, setPatientRecords] = useState([]);
  const [recordsLoading, setRecordsLoading] = useState(false);

  useEffect(() => {
    fetchPatients();
  }, []);

  async function fetchPatients() {
    try {
      const data = await doctorAPI.getPatients();
      setPatients(Array.isArray(data) ? data : []);
    } catch (err) {
      addToast("Failed to load patients", "error");
    } finally {
      setLoading(false);
    }
  }

  async function viewRecords(patient) {
    setSelectedPatient(patient);
    setRecordsLoading(true);
    try {
      // Try fetching records for this patient — depends on backend support
      const data = await doctorAPI.getPatients();
      // Filter records for this patient from local data if backend returns them inline
      setPatientRecords(patient.records || []);
    } catch {
      setPatientRecords([]);
    } finally {
      setRecordsLoading(false);
    }
  }

  const filtered = patients.filter((p) =>
    (p.name || p.username || "").toLowerCase().includes(search.toLowerCase())
  );

  function getInitials(name) {
    if (!name) return "?";
    return name.split(" ").map((w) => w[0]).join("").toUpperCase().slice(0, 2);
  }

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  return (
    <div className="flex-1 overflow-hidden flex">
      <main className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
          <div>
            <h1 className="text-2xl font-bold text-white" style={{ fontFamily: "Orbitron, sans-serif" }}>
              My Patients
            </h1>
            <p className="text-sm text-slate-400">{patients.length} patients total</p>
          </div>
          <button
            onClick={() => navigate("/doctor/records/new")}
            className="self-start px-4 py-2 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 text-white text-sm font-semibold hover:scale-105 transition-transform shadow-lg shadow-cyan-500/25"
          >
            + New Record
          </button>
        </div>

        {/* Search */}
        <div className="mb-6">
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search patients by name..."
            className="w-full max-w-md bg-white/5 border border-white/20 rounded-xl px-4 py-3 text-white placeholder-slate-500 focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500 outline-none transition"
          />
        </div>

        {/* Patient Grid */}
        {filtered.length === 0 ? (
          <EmptyState
            icon="🔍"
            title={search ? "No patients match your search" : "No patients yet"}
            subtitle={search ? "Try a different search term" : "Create a medical record to add your first patient"}
            actionLabel={!search ? "Create Record" : undefined}
            onAction={!search ? () => navigate("/doctor/records/new") : undefined}
          />
        ) : (
          <motion.div
            variants={container}
            initial="hidden"
            animate="show"
            className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4"
          >
            {filtered.map((p, i) => (
              <motion.div
                key={p.id || i}
                variants={item}
                whileHover={{ scale: 1.02, y: -2 }}
                className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-5 hover:border-cyan-500/50 transition-colors cursor-pointer"
                onClick={() => viewRecords(p)}
              >
                <div className="flex items-start gap-4">
                  <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-cyan-500 to-blue-600 flex items-center justify-center text-white text-sm font-bold shrink-0" style={{ fontFamily: "Orbitron, sans-serif" }}>
                    {getInitials(p.name || p.username)}
                  </div>
                  <div className="flex-1 min-w-0">
                    <h3 className="text-white font-semibold text-sm truncate">{p.name || p.username || "Unknown"}</h3>
                    {p.medicalRecordNumber && (
                      <div className="text-[10px] font-mono tracking-wider text-cyan-400 mt-0.5">
                        {p.medicalRecordNumber}
                      </div>
                    )}
                    <div className="flex items-center gap-2 mt-1 flex-wrap">
                      {p.age && (
                        <span className="text-xs px-2 py-0.5 rounded-full bg-white/10 text-slate-300">{p.age} yrs</span>
                      )}
                      {p.gender && (
                        <span className="text-xs px-2 py-0.5 rounded-full bg-white/10 text-slate-300">{p.gender}</span>
                      )}
                    </div>
                  </div>
                </div>
                <button
                  className="mt-4 w-full py-2 rounded-xl bg-cyan-500/10 border border-cyan-500/30 text-cyan-400 text-sm font-medium hover:bg-cyan-500/20 transition-colors"
                  onClick={(e) => { e.stopPropagation(); viewRecords(p); }}
                >
                  View Records
                </button>
              </motion.div>
            ))}
          </motion.div>
        )}
      </main>

      {/* ── Right drawer: patient records ── */}
      <AnimatePresence>
        {selectedPatient && (
          <motion.aside
            initial={{ x: 400, opacity: 0 }}
            animate={{ x: 0, opacity: 1 }}
            exit={{ x: 400, opacity: 0 }}
            transition={{ type: "spring", stiffness: 300, damping: 30 }}
            className="w-full sm:w-96 shrink-0 bg-slate-900/95 backdrop-blur-xl border-l border-white/10 overflow-y-auto"
          >
            <div className="p-6">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-lg font-bold text-white" style={{ fontFamily: "Space Grotesk, sans-serif" }}>
                  {selectedPatient.name || selectedPatient.username}'s Records
                </h2>
                <button
                  onClick={() => setSelectedPatient(null)}
                  className="text-slate-400 hover:text-white transition text-xl"
                >
                  ×
                </button>
              </div>

              {recordsLoading ? (
                <LoadingSpinner size="md" />
              ) : patientRecords.length === 0 ? (
                <EmptyState
                  icon="📋"
                  title="No records found"
                  subtitle="Create a new record for this patient"
                  actionLabel="Create Record"
                  onAction={() => navigate("/doctor/records/new")}
                />
              ) : (
                <div className="space-y-3">
                  {patientRecords.map((rec, i) => (
                    <div
                      key={rec.id || i}
                      onClick={() => navigate(`/doctor/records/${rec.id}`)}
                      className="bg-white/5 border border-white/10 rounded-xl p-4 hover:border-cyan-500/30 cursor-pointer transition-colors"
                    >
                      <div className="text-white text-sm font-medium">{rec.diagnosis || "No diagnosis"}</div>
                      <div className="text-slate-400 text-xs mt-1">{rec.createdAt || "—"}</div>
                      {rec.drugs && (
                        <div className="mt-2 flex flex-wrap gap-1">
                          {rec.drugs.slice(0, 3).map((d, j) => (
                            <span key={j} className="text-[10px] px-2 py-0.5 rounded-full bg-cyan-500/10 text-cyan-400 border border-cyan-500/20">
                              {d.name}
                            </span>
                          ))}
                          {rec.drugs.length > 3 && (
                            <span className="text-[10px] text-slate-400">+{rec.drugs.length - 3}</span>
                          )}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          </motion.aside>
        )}
      </AnimatePresence>
    </div>
  );
}
