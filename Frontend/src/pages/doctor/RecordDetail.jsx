import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { useToast } from "../../context/ToastContext";
import { doctorAPI } from "../../services/api";
import LoadingSpinner from "../../components/ui/LoadingSpinner";

export default function RecordDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addToast } = useToast();
  const [record, setRecord] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchRecord();
  }, [id]);

  async function fetchRecord() {
    try {
      const data = await doctorAPI.getRecord(id);
      setRecord(data);
    } catch (err) {
      addToast("Failed to load record", "error");
    } finally {
      setLoading(false);
    }
  }

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (!record) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center gap-4">
        <p className="text-slate-400">Record not found</p>
        <button
          onClick={() => navigate("/doctor/dashboard")}
          className="px-4 py-2 rounded-xl bg-white/10 border border-white/20 text-white text-sm hover:bg-white/20 transition"
        >
          ← Back to Dashboard
        </button>
      </div>
    );
  }

  return (
    <div className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8">
      <div className="max-w-3xl mx-auto">
        {/* Back */}
        <button
          onClick={() => navigate(-1)}
          className="text-sm text-slate-400 hover:text-cyan-400 transition-colors mb-6 flex items-center gap-1"
        >
          ← Back
        </button>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-8"
        >
          {/* Header */}
          <div className="mb-6 pb-6 border-b border-white/10">
            <h1 className="text-xl font-bold text-white mb-1" style={{ fontFamily: "Orbitron, sans-serif" }}>
              Medical Record #{id}
            </h1>
            <p className="text-slate-400 text-sm">{record.createdAt || "—"}</p>
          </div>

          {/* Patient info */}
          <div className="mb-6">
            <span className="text-xs font-semibold tracking-[0.2em] uppercase text-cyan-400" style={{ fontFamily: "Orbitron, sans-serif" }}>
              Patient
            </span>
            <p className="text-white font-medium mt-1">{record.patientName || record.patientUserId || "—"}</p>
          </div>

          {/* Diagnosis */}
          <div className="mb-6">
            <span className="text-xs font-semibold tracking-[0.2em] uppercase text-cyan-400" style={{ fontFamily: "Orbitron, sans-serif" }}>
              Diagnosis
            </span>
            <p className="text-white text-lg font-bold mt-1" style={{ fontFamily: "Orbitron, sans-serif" }}>
              {record.diagnosis || "—"}
            </p>
          </div>

          {/* Treatment Plan */}
          <div className="mb-6">
            <span className="text-xs font-semibold tracking-[0.2em] uppercase text-cyan-400" style={{ fontFamily: "Orbitron, sans-serif" }}>
              Treatment Plan
            </span>
            <p className="text-slate-300 mt-1 whitespace-pre-wrap">{record.treatmentPlan || "—"}</p>
          </div>

          {/* Drugs */}
          {record.drugs && record.drugs.length > 0 && (
            <div className="mb-6">
              <span className="text-xs font-semibold tracking-[0.2em] uppercase text-cyan-400" style={{ fontFamily: "Orbitron, sans-serif" }}>
                Prescribed Drugs
              </span>
              <div className="mt-3 bg-white/5 border border-white/10 rounded-xl overflow-hidden">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-white/10">
                      <th className="text-left px-4 py-2 text-xs text-slate-400 uppercase font-semibold">Drug</th>
                      <th className="text-left px-4 py-2 text-xs text-slate-400 uppercase font-semibold">Dosage</th>
                      <th className="text-left px-4 py-2 text-xs text-slate-400 uppercase font-semibold">Purpose</th>
                    </tr>
                  </thead>
                  <tbody>
                    {record.drugs.map((d, i) => (
                      <tr key={i} className="border-b border-white/5">
                        <td className="px-4 py-3 text-white text-sm">{d.name}</td>
                        <td className="px-4 py-3 text-slate-300 text-sm">{d.dosage}</td>
                        <td className="px-4 py-3 text-slate-300 text-sm">{d.purpose}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Actions */}
          <div className="flex gap-3 pt-4 border-t border-white/10">
            <button
              onClick={() => navigate("/doctor/billing/new")}
              className="px-4 py-2 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 text-white text-sm font-semibold hover:scale-105 transition-transform shadow-lg shadow-cyan-500/25"
            >
              Add Billing
            </button>
            <button
              onClick={() => navigate("/doctor/dashboard")}
              className="px-4 py-2 rounded-xl bg-white/10 border border-white/20 text-white text-sm hover:bg-white/20 transition"
            >
              Back to Dashboard
            </button>
          </div>
        </motion.div>
      </div>
    </div>
  );
}
