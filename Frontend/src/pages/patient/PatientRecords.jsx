import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { motion, AnimatePresence } from "framer-motion";
import { useToast } from "../../context/ToastContext";
import { patientAPI } from "../../services/api";
import LoadingSpinner from "../../components/ui/LoadingSpinner";
import EmptyState from "../../components/ui/EmptyState";

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.07 } } };
const item = { hidden: { opacity: 0, y: 20 }, show: { opacity: 1, y: 0 } };

export default function PatientRecords() {
  const navigate = useNavigate();
  const { addToast } = useToast();
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);
  const [expandedId, setExpandedId] = useState(null);

  useEffect(() => {
    fetchRecords();
  }, []);

  async function fetchRecords() {
    try {
      const data = await patientAPI.getRecords();
      setRecords(Array.isArray(data) ? data : []);
    } catch (err) {
      addToast("Failed to load records", "error");
    } finally {
      setLoading(false);
    }
  }

  function toggleExpand(id) {
    setExpandedId(expandedId === id ? null : id);
  }

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  return (
    <div className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8">
      <div className="max-w-4xl mx-auto">
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-white" style={{ fontFamily: "Orbitron, sans-serif" }}>
            My Medical Records
          </h1>
          <p className="text-slate-400 text-sm mt-1">{records.length} records on file</p>
        </div>

        {records.length === 0 ? (
          <EmptyState
            icon="📋"
            title="No medical records"
            subtitle="Your medical records will appear here when your doctor creates them."
          />
        ) : (
          <motion.div variants={container} initial="hidden" animate="show" className="space-y-4">
            {records.map((rec, i) => {
              const isExpanded = expandedId === (rec.id || i);
              return (
                <motion.div key={rec.id || i} variants={item} layout>
                  {/* Collapsed header */}
                  <div
                    onClick={() => toggleExpand(rec.id || i)}
                    className={`bg-white/5 backdrop-blur-xl border rounded-2xl p-5 cursor-pointer transition-all ${
                      isExpanded ? "border-cyan-500/30" : "border-white/10 hover:border-white/20"
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-3 mb-1">
                          <h3 className="text-white font-semibold text-sm truncate">
                            {rec.diagnosis || "No diagnosis"}
                          </h3>
                          {rec.drugs && rec.drugs.length > 0 && (
                            <span className="shrink-0 text-[10px] px-2 py-0.5 rounded-full bg-cyan-500/10 border border-cyan-500/20 text-cyan-400">
                              {rec.drugs.length} drugs
                            </span>
                          )}
                        </div>
                        <p className="text-slate-400 text-xs">
                          {rec.doctorName || "Doctor"} • {rec.createdAt || "—"}
                        </p>
                      </div>
                      <motion.span
                        animate={{ rotate: isExpanded ? 180 : 0 }}
                        className="text-slate-400 text-lg ml-3"
                      >
                        ▾
                      </motion.span>
                    </div>

                    {/* Expanded content */}
                    <AnimatePresence>
                      {isExpanded && (
                        <motion.div
                          initial={{ opacity: 0, height: 0 }}
                          animate={{ opacity: 1, height: "auto" }}
                          exit={{ opacity: 0, height: 0 }}
                          transition={{ duration: 0.3 }}
                          className="overflow-hidden"
                        >
                          <div className="mt-4 pt-4 border-t border-white/10 space-y-4">
                            {/* Treatment Plan */}
                            {rec.treatmentPlan && (
                              <div>
                                <span className="text-xs font-semibold tracking-[0.2em] uppercase text-cyan-400" style={{ fontFamily: "Orbitron, sans-serif" }}>
                                  Treatment Plan
                                </span>
                                <p className="text-slate-300 text-sm mt-1 whitespace-pre-wrap">{rec.treatmentPlan}</p>
                              </div>
                            )}

                            {/* Drug table */}
                            {rec.drugs && rec.drugs.length > 0 && (
                              <div>
                                <span className="text-xs font-semibold tracking-[0.2em] uppercase text-cyan-400" style={{ fontFamily: "Orbitron, sans-serif" }}>
                                  Prescribed Drugs
                                </span>
                                {/* Desktop table */}
                                <div className="hidden sm:block mt-2 bg-white/5 border border-cyan-500/10 rounded-xl overflow-hidden">
                                  <table className="w-full">
                                    <thead>
                                      <tr className="border-b border-cyan-500/10">
                                        <th className="text-left px-4 py-2 text-xs text-cyan-400/70 uppercase font-semibold">Name</th>
                                        <th className="text-left px-4 py-2 text-xs text-cyan-400/70 uppercase font-semibold">Dosage</th>
                                        <th className="text-left px-4 py-2 text-xs text-cyan-400/70 uppercase font-semibold">Purpose</th>
                                      </tr>
                                    </thead>
                                    <tbody>
                                      {rec.drugs.map((d, j) => (
                                        <tr key={j} className="border-b border-white/5">
                                          <td className="px-4 py-2.5 text-white text-sm font-medium">{d.name}</td>
                                          <td className="px-4 py-2.5 text-slate-300 text-sm">{d.dosage || "—"}</td>
                                          <td className="px-4 py-2.5 text-slate-300 text-sm">{d.purpose || "—"}</td>
                                        </tr>
                                      ))}
                                    </tbody>
                                  </table>
                                </div>
                                {/* Mobile drug cards */}
                                <div className="sm:hidden mt-2 space-y-2">
                                  {rec.drugs.map((d, j) => (
                                    <div key={j} className="bg-white/5 border border-white/10 rounded-lg p-3">
                                      <div className="text-white text-sm font-medium">{d.name}</div>
                                      <div className="text-slate-400 text-xs">{d.dosage || "—"} • {d.purpose || "—"}</div>
                                    </div>
                                  ))}
                                </div>
                              </div>
                            )}

                            {/* Doctor info */}
                            {rec.doctorName && (
                              <div>
                                <span className="text-xs font-semibold tracking-[0.2em] uppercase text-cyan-400" style={{ fontFamily: "Orbitron, sans-serif" }}>
                                  Doctor
                                </span>
                                <p className="text-slate-300 text-sm mt-1">{rec.doctorName}</p>
                              </div>
                            )}

                            {/* Ask AI */}
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                navigate(`/patient/chat?context=record_${rec.id}`);
                              }}
                              className="px-4 py-2 rounded-xl bg-gradient-to-r from-cyan-500/10 to-blue-600/10 border border-cyan-500/20 text-cyan-400 text-sm font-medium hover:bg-cyan-500/15 transition-colors"
                            >
                              🤖 Ask AI about this record
                            </button>
                          </div>
                        </motion.div>
                      )}
                    </AnimatePresence>
                  </div>
                </motion.div>
              );
            })}
          </motion.div>
        )}
      </div>
    </div>
  );
}
