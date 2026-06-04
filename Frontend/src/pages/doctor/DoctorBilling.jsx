import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { useToast } from "../../context/ToastContext";
import { doctorAPI } from "../../services/api";
import LoadingSpinner from "../../components/ui/LoadingSpinner";
import EmptyState from "../../components/ui/EmptyState";

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.06 } } };
const item = { hidden: { opacity: 0, y: 16 }, show: { opacity: 1, y: 0 } };

function StatusPill({ status }) {
  if (status === "PAID") {
    return (
      <span className="inline-flex items-center gap-1 text-[10px] px-2.5 py-1 rounded-full bg-emerald-500/15 text-emerald-400 border border-emerald-500/30 font-semibold">
        ✓ Payment Received
      </span>
    );
  }
  if (status === "PROCESSING") {
    return (
      <span className="inline-flex items-center gap-1 text-[10px] px-2.5 py-1 rounded-full bg-blue-500/15 text-blue-400 border border-blue-500/30 font-semibold">
        ⟳ Processing
      </span>
    );
  }
  return (
    <span className="inline-flex items-center gap-1 text-[10px] px-2.5 py-1 rounded-full bg-amber-500/15 text-amber-400 border border-amber-500/30 font-semibold">
      Awaiting Payment
    </span>
  );
}

export default function DoctorBilling() {
  const { addToast } = useToast();
  const [billing, setBilling] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      try {
        const data = await doctorAPI.getBilling();
        if (!cancelled) setBilling(Array.isArray(data) ? data : []);
      } catch (err) {
        if (!cancelled) addToast(err.response?.data?.message || "Failed to load billing", "error");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    // Auto-refresh every 10s so paid bills surface without a manual reload
    const t = setInterval(load, 10000);
    return () => { cancelled = true; clearInterval(t); };
  }, [addToast]);

  const total = billing.reduce((s, b) => s + Number(b.totalCost || 0), 0);
  const paid = billing.filter((b) => b.status === "PAID").reduce((s, b) => s + Number(b.totalCost || 0), 0);
  const pending = total - paid;

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  return (
    <div className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8">
      <div className="max-w-5xl mx-auto">
        <div className="mb-6 flex items-center justify-between flex-wrap gap-3">
          <div>
            <h1 className="text-2xl font-bold text-white" style={{ fontFamily: "Orbitron, sans-serif" }}>
              Billing
            </h1>
            <p className="text-slate-400 text-sm mt-1">{billing.length} entries across your records</p>
          </div>
          <Link
            to="/doctor/billing/new"
            className="px-4 py-2 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 text-white text-sm font-semibold hover:scale-[1.02] transition-transform shadow-lg shadow-cyan-500/25"
          >
            + New Entry
          </Link>
        </div>

        {/* Summary */}
        <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} className="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-8">
          <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-5 text-center">
            <p className="text-[10px] uppercase tracking-[0.2em] text-slate-400 mb-1" style={{ fontFamily: "Orbitron, sans-serif" }}>Total Billed</p>
            <p className="text-2xl font-bold bg-gradient-to-r from-cyan-500 to-blue-600 bg-clip-text text-transparent" style={{ fontFamily: "Orbitron, sans-serif" }}>
              ₹{total.toLocaleString()}
            </p>
          </div>
          <div className="bg-white/5 border border-emerald-500/20 rounded-2xl p-5 text-center">
            <p className="text-[10px] uppercase tracking-[0.2em] text-slate-400 mb-1" style={{ fontFamily: "Orbitron, sans-serif" }}>Collected</p>
            <p className="text-2xl font-bold text-emerald-400" style={{ fontFamily: "Orbitron, sans-serif" }}>
              ₹{paid.toLocaleString()}
            </p>
          </div>
          <div className="bg-white/5 border border-amber-500/20 rounded-2xl p-5 text-center">
            <p className="text-[10px] uppercase tracking-[0.2em] text-slate-400 mb-1" style={{ fontFamily: "Orbitron, sans-serif" }}>Pending</p>
            <p className="text-2xl font-bold text-amber-400" style={{ fontFamily: "Orbitron, sans-serif" }}>
              ₹{pending.toLocaleString()}
            </p>
          </div>
        </motion.div>

        {billing.length === 0 ? (
          <EmptyState
            icon="💳"
            title="No billing entries"
            subtitle="Bills you create will appear here. Their status updates automatically when patients pay."
          />
        ) : (
          <motion.div variants={container} initial="hidden" animate="show" className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl overflow-hidden">
            <table className="w-full hidden sm:table">
              <thead>
                <tr className="border-b border-white/10">
                  <th className="text-left px-6 py-3 text-xs font-semibold text-slate-400 uppercase tracking-wider">Created</th>
                  <th className="text-left px-6 py-3 text-xs font-semibold text-slate-400 uppercase tracking-wider">Patient</th>
                  <th className="text-left px-6 py-3 text-xs font-semibold text-slate-400 uppercase tracking-wider">Diagnosis / Description</th>
                  <th className="text-right px-6 py-3 text-xs font-semibold text-slate-400 uppercase tracking-wider">Amount</th>
                  <th className="text-center px-6 py-3 text-xs font-semibold text-slate-400 uppercase tracking-wider">Status</th>
                  <th className="text-left px-6 py-3 text-xs font-semibold text-slate-400 uppercase tracking-wider">Payment</th>
                </tr>
              </thead>
              <tbody>
                {billing.map((b) => (
                  <motion.tr key={b.id} variants={item} className="border-b border-white/5 hover:bg-white/5">
                    <td className="px-6 py-4 text-slate-400 text-sm">{b.createdAt ? String(b.createdAt).slice(0, 10) : "—"}</td>
                    <td className="px-6 py-4 text-white text-sm">{b.patientName || "—"}</td>
                    <td className="px-6 py-4 text-slate-300 text-sm truncate max-w-xs">{b.description || b.recordDiagnosis || "—"}</td>
                    <td className="px-6 py-4 text-right text-white font-bold" style={{ fontFamily: "Orbitron, sans-serif" }}>
                      ₹{Number(b.totalCost || 0).toLocaleString()}
                    </td>
                    <td className="px-6 py-4 text-center"><StatusPill status={b.status} /></td>
                    <td className="px-6 py-4 text-xs text-slate-400">
                      {b.status === "PAID" ? (
                        <div>
                          <div className="text-emerald-400 font-medium">{(b.paymentMethod || "—").replace("_", " ")}</div>
                          <div className="text-slate-500">{b.paidAt ? String(b.paidAt).slice(0, 16).replace("T", " ") : ""}</div>
                          {b.receipt?.receiptNumber && (
                            <div className="text-slate-500 font-mono mt-0.5">{b.receipt.receiptNumber}</div>
                          )}
                        </div>
                      ) : (
                        <span className="text-slate-500">—</span>
                      )}
                    </td>
                  </motion.tr>
                ))}
              </tbody>
            </table>

            {/* Mobile list */}
            <div className="sm:hidden divide-y divide-white/5">
              {billing.map((b) => (
                <div key={b.id} className="p-4">
                  <div className="flex items-start justify-between mb-2">
                    <div className="min-w-0">
                      <div className="text-white text-sm font-medium truncate">{b.patientName || "—"}</div>
                      <div className="text-slate-400 text-xs">{b.description || b.recordDiagnosis || "—"}</div>
                    </div>
                    <div className="text-white font-bold text-sm shrink-0 ml-3" style={{ fontFamily: "Orbitron, sans-serif" }}>
                      ₹{Number(b.totalCost || 0).toLocaleString()}
                    </div>
                  </div>
                  <div className="flex items-center justify-between">
                    <StatusPill status={b.status} />
                    {b.status === "PAID" && (
                      <div className="text-[10px] text-slate-500 text-right">
                        <div className="text-emerald-400">{(b.paymentMethod || "—").replace("_", " ")}</div>
                        {b.receipt?.receiptNumber && <div className="font-mono">{b.receipt.receiptNumber}</div>}
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </motion.div>
        )}

        <p className="mt-4 text-[10px] text-slate-500 text-center">Updates automatically every 10 seconds.</p>
      </div>
    </div>
  );
}
