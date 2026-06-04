import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { useToast } from "../../context/ToastContext";
import { doctorAPI } from "../../services/api";
import LoadingSpinner from "../../components/ui/LoadingSpinner";

export default function CreateBilling() {
  const navigate = useNavigate();
  const { addToast } = useToast();
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);
  const [records, setRecords] = useState([]);
  const [recordsLoading, setRecordsLoading] = useState(true);

  const [form, setForm] = useState({
    recordId: "",
    totalCost: "",
    description: "",
  });

  useEffect(() => {
    loadRecords();
  }, []);

  async function loadRecords() {
    try {
      const data = await doctorAPI.getRecords();
      setRecords(Array.isArray(data) ? data : []);
    } catch (err) {
      addToast(err.response?.data?.message || "Failed to load records", "error");
    } finally {
      setRecordsLoading(false);
    }
  }

  function formatCost(value) {
    return value.replace(/[^0-9.]/g, "");
  }

  async function handleSubmit(e) {
    e.preventDefault();
    if (!form.recordId) { addToast("Select a medical record", "warning"); return; }
    if (!form.totalCost || parseFloat(form.totalCost) <= 0) { addToast("Enter a valid cost", "warning"); return; }

    setSubmitting(true);
    try {
      await doctorAPI.createBilling({
        recordId: Number(form.recordId),
        totalCost: parseFloat(form.totalCost),
        description: form.description,
      });
      setSuccess(true);
      addToast("Billing entry created successfully!", "success");
      setTimeout(() => navigate("/doctor/dashboard"), 1500);
    } catch (err) {
      addToast(err.response?.data?.message || "Failed to create billing", "error");
    } finally {
      setSubmitting(false);
    }
  }

  const selectedRecord = records.find((r) => String(r.id) === String(form.recordId));
  const inputClass = "w-full bg-white/5 border border-white/20 rounded-xl px-4 py-3 text-white placeholder-slate-500 focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500 outline-none transition";

  return (
    <div className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8">
      <div className="max-w-lg mx-auto">
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-white" style={{ fontFamily: "Orbitron, sans-serif" }}>
            Create Billing Entry
          </h1>
          <p className="text-slate-400 text-sm mt-1">Add a billing entry for a medical record</p>
        </div>

        {success ? (
          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-white/5 backdrop-blur-xl border border-emerald-500/30 rounded-2xl p-8 text-center"
          >
            <motion.div
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              transition={{ type: "spring", stiffness: 300, damping: 20 }}
              className="w-16 h-16 mx-auto mb-4 rounded-full bg-emerald-500/20 flex items-center justify-center"
            >
              <span className="text-3xl">✓</span>
            </motion.div>
            <h2 className="text-lg font-bold text-white mb-2">Billing Entry Created</h2>
            <p className="text-slate-400 text-sm">Redirecting to dashboard...</p>
          </motion.div>
        ) : (
          <motion.form
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            onSubmit={handleSubmit}
            className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-6 space-y-5"
          >
            {/* Record selector */}
            <div>
              <label className="block text-xs font-semibold tracking-[0.2em] uppercase text-cyan-400 mb-2" style={{ fontFamily: "Orbitron, sans-serif" }}>
                Medical Record
              </label>
              {recordsLoading ? (
                <div className="flex items-center gap-2 text-slate-400 text-sm py-3"><LoadingSpinner size="sm" /> Loading your records…</div>
              ) : records.length === 0 ? (
                <div className="p-3 rounded-xl bg-red-500/10 border border-red-500/30 text-red-300 text-sm">
                  No medical records yet — create one before billing.
                </div>
              ) : (
                <>
                  <select
                    value={form.recordId}
                    onChange={(e) => setForm((p) => ({ ...p, recordId: e.target.value }))}
                    className={inputClass}
                    required
                  >
                    <option value="" className="bg-slate-900">Select a record…</option>
                    {records.map((r) => (
                      <option key={r.id} value={r.id} className="bg-slate-900">
                        #{r.id} — {r.diagnosis || "Untitled"}{r.createdAt ? ` (${String(r.createdAt).slice(0, 10)})` : ""}
                      </option>
                    ))}
                  </select>
                  {selectedRecord && (
                    <p className="mt-2 text-xs text-slate-400">
                      Treatment: {selectedRecord.treatmentPlan || "—"}
                    </p>
                  )}
                </>
              )}
            </div>

            {/* Total Cost */}
            <div>
              <label className="block text-xs font-semibold tracking-[0.2em] uppercase text-cyan-400 mb-2" style={{ fontFamily: "Orbitron, sans-serif" }}>
                Total Cost
              </label>
              <div className="relative">
                <span className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 font-semibold">₹</span>
                <input
                  type="text"
                  value={form.totalCost}
                  onChange={(e) => setForm((p) => ({ ...p, totalCost: formatCost(e.target.value) }))}
                  placeholder="0.00"
                  className={inputClass + " pl-8 text-2xl font-bold"}
                  style={{ fontFamily: "Orbitron, sans-serif" }}
                  required
                />
              </div>
            </div>

            {/* Description */}
            <div>
              <label className="block text-xs font-semibold tracking-[0.2em] uppercase text-cyan-400 mb-2" style={{ fontFamily: "Orbitron, sans-serif" }}>
                Description
              </label>
              <textarea
                value={form.description}
                onChange={(e) => setForm((p) => ({ ...p, description: e.target.value }))}
                placeholder="Describe the billing entry..."
                rows={3}
                className={inputClass + " resize-none"}
              />
            </div>

            <button
              type="submit"
              disabled={submitting || recordsLoading || records.length === 0}
              className="w-full py-3 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 text-white font-semibold hover:scale-[1.02] transition-transform shadow-lg shadow-cyan-500/25 disabled:opacity-50 flex items-center justify-center gap-2"
            >
              {submitting ? <><LoadingSpinner size="sm" /> Submitting...</> : "Create Billing Entry"}
            </button>
          </motion.form>
        )}
      </div>
    </div>
  );
}
