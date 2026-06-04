import { useState, useEffect } from "react";
import { useNavigate, Link } from "react-router-dom";
import { motion } from "framer-motion";
import { useToast } from "../../context/ToastContext";
import { patientAPI } from "../../services/api";
import LoadingSpinner from "../../components/ui/LoadingSpinner";
import EmptyState from "../../components/ui/EmptyState";
import PaymentModal from "../../components/PaymentModal";

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.07 } } };
const item = { hidden: { opacity: 0, y: 20 }, show: { opacity: 1, y: 0 } };

function StatusPill({ status }) {
  const s = status || "PENDING";
  if (s === "PAID") {
    return <span className="inline-flex items-center gap-1 text-[10px] px-2.5 py-1 rounded-full bg-emerald-500/15 text-emerald-400 border border-emerald-500/30 font-semibold">✓ Paid</span>;
  }
  if (s === "PROCESSING") {
    return <span className="inline-flex items-center gap-1 text-[10px] px-2.5 py-1 rounded-full bg-blue-500/15 text-blue-400 border border-blue-500/30 font-semibold">⟳ Processing</span>;
  }
  return <span className="inline-flex items-center gap-1 text-[10px] px-2.5 py-1 rounded-full bg-amber-500/15 text-amber-400 border border-amber-500/30 font-semibold">Pending</span>;
}

export default function PatientBilling() {
  const navigate = useNavigate();
  const { addToast } = useToast();
  const [billing, setBilling] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeBill, setActiveBill] = useState(null);

  useEffect(() => {
    fetchBilling();
  }, []);

  async function fetchBilling() {
    try {
      const data = await patientAPI.getBilling();
      setBilling(Array.isArray(data) ? data : []);
    } catch {
      addToast("Failed to load billing", "error");
    } finally {
      setLoading(false);
    }
  }

  function analyzeWithAI(entry) {
    const query = `Explain billing entry: "${entry.description || "Medical billing"}" worth ₹${entry.totalCost || entry.amount || 0} and check my insurance coverage`;
    navigate(`/patient/chat?prefill=${encodeURIComponent(query)}`);
  }

  function handlePaid(updated) {
    setBilling((prev) => prev.map((b) => (b.id === updated.id ? updated : b)));
    addToast("Payment successful", "success");
  }

  async function viewReceipt(entry) {
    try {
      const receipt = await patientAPI.getReceipt(entry.id);
      setActiveBill({ ...entry, receipt });
    } catch (err) {
      addToast(err.response?.data?.message || "Could not load receipt", "error");
    }
  }

  const totalAmount = billing.reduce((sum, b) => sum + Number(b.totalCost || 0), 0);
  const paidAmount = billing.filter((b) => b.status === "PAID").reduce((sum, b) => sum + Number(b.totalCost || 0), 0);
  const unpaidAmount = totalAmount - paidAmount;

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
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-white" style={{ fontFamily: "Orbitron, sans-serif" }}>
            My Billing
          </h1>
          <p className="text-slate-400 text-sm mt-1">{billing.length} billing entries</p>
        </div>

        {/* Coverage banner */}
        <div className="mb-6 p-4 rounded-xl bg-gradient-to-r from-cyan-500/10 to-blue-600/10 border border-cyan-500/20">
          <div className="flex items-center gap-3">
            <span className="text-2xl">📄</span>
            <div className="flex-1">
              <p className="text-white text-sm font-medium">Upload your insurance policy for AI coverage analysis</p>
              <p className="text-slate-400 text-xs mt-0.5">Get AI-powered cost estimation and coverage analysis</p>
            </div>
            <Link
              to="/patient/documents"
              className="shrink-0 px-4 py-2 rounded-xl bg-cyan-500/10 border border-cyan-500/30 text-cyan-400 text-sm font-medium hover:bg-cyan-500/20 transition-colors"
            >
              Upload
            </Link>
          </div>
        </div>

        {/* Summary cards */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-8 grid grid-cols-1 sm:grid-cols-3 gap-3"
        >
          <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-5 text-center">
            <p className="text-[10px] font-semibold tracking-[0.2em] uppercase text-slate-400 mb-1" style={{ fontFamily: "Orbitron, sans-serif" }}>Total</p>
            <p className="text-2xl font-bold bg-gradient-to-r from-cyan-500 to-blue-600 bg-clip-text text-transparent" style={{ fontFamily: "Orbitron, sans-serif" }}>
              ₹{totalAmount.toLocaleString()}
            </p>
          </div>
          <div className="bg-white/5 backdrop-blur-xl border border-emerald-500/20 rounded-2xl p-5 text-center">
            <p className="text-[10px] font-semibold tracking-[0.2em] uppercase text-slate-400 mb-1" style={{ fontFamily: "Orbitron, sans-serif" }}>Paid</p>
            <p className="text-2xl font-bold text-emerald-400" style={{ fontFamily: "Orbitron, sans-serif" }}>
              ₹{paidAmount.toLocaleString()}
            </p>
          </div>
          <div className="bg-white/5 backdrop-blur-xl border border-amber-500/20 rounded-2xl p-5 text-center">
            <p className="text-[10px] font-semibold tracking-[0.2em] uppercase text-slate-400 mb-1" style={{ fontFamily: "Orbitron, sans-serif" }}>Outstanding</p>
            <p className="text-2xl font-bold text-amber-400" style={{ fontFamily: "Orbitron, sans-serif" }}>
              ₹{unpaidAmount.toLocaleString()}
            </p>
          </div>
        </motion.div>

        {billing.length === 0 ? (
          <EmptyState
            icon="💳"
            title="No billing entries"
            subtitle="Your billing history will appear here."
          />
        ) : (
          <>
            {/* Desktop table */}
            <div className="hidden sm:block">
              <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl overflow-hidden">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-white/10">
                      <th className="text-left px-6 py-3 text-xs font-semibold text-slate-400 uppercase tracking-wider">Date</th>
                      <th className="text-left px-6 py-3 text-xs font-semibold text-slate-400 uppercase tracking-wider">Description</th>
                      <th className="text-right px-6 py-3 text-xs font-semibold text-slate-400 uppercase tracking-wider">Amount</th>
                      <th className="text-center px-6 py-3 text-xs font-semibold text-slate-400 uppercase tracking-wider">Status</th>
                      <th className="text-right px-6 py-3 text-xs font-semibold text-slate-400 uppercase tracking-wider">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {billing.map((b, i) => (
                      <motion.tr
                        key={b.id || i}
                        variants={item}
                        className="border-b border-white/5 hover:bg-white/5 transition-colors"
                      >
                        <td className="px-6 py-4 text-slate-400 text-sm">{b.createdAt ? String(b.createdAt).slice(0, 10) : "—"}</td>
                        <td className="px-6 py-4 text-white text-sm">{b.description || b.recordDiagnosis || "Billing entry"}</td>
                        <td className="px-6 py-4 text-right">
                          <span className="text-white font-bold" style={{ fontFamily: "Orbitron, sans-serif" }}>
                            ₹{Number(b.totalCost || 0).toLocaleString()}
                          </span>
                        </td>
                        <td className="px-6 py-4 text-center"><StatusPill status={b.status} /></td>
                        <td className="px-6 py-4 text-right">
                          <div className="flex items-center gap-2 justify-end">
                            {b.status === "PAID" ? (
                              <button
                                onClick={() => viewReceipt(b)}
                                className="text-xs px-3 py-1.5 rounded-lg bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 hover:bg-emerald-500/20 transition-colors"
                              >
                                Receipt
                              </button>
                            ) : (
                              <button
                                onClick={() => setActiveBill(b)}
                                className="text-xs px-3 py-1.5 rounded-lg bg-gradient-to-r from-emerald-500 to-cyan-600 text-white font-medium hover:scale-105 transition-transform"
                              >
                                Pay Now
                              </button>
                            )}
                            <button
                              onClick={() => analyzeWithAI(b)}
                              className="text-xs px-3 py-1.5 rounded-lg bg-cyan-500/10 border border-cyan-500/30 text-cyan-400 hover:bg-cyan-500/20 transition-colors"
                            >
                              Analyze
                            </button>
                          </div>
                        </td>
                      </motion.tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Mobile cards */}
            <motion.div variants={container} initial="hidden" animate="show" className="sm:hidden space-y-3">
              {billing.map((b, i) => (
                <motion.div
                  key={b.id || i}
                  variants={item}
                  className="bg-white/5 border border-white/10 rounded-xl p-4"
                >
                  <div className="flex justify-between items-start mb-2">
                    <div className="min-w-0">
                      <p className="text-white text-sm font-medium truncate">{b.description || b.recordDiagnosis || "Billing entry"}</p>
                      <p className="text-slate-400 text-xs">{b.createdAt ? String(b.createdAt).slice(0, 10) : "—"}</p>
                    </div>
                    <span className="text-white font-bold text-sm" style={{ fontFamily: "Orbitron, sans-serif" }}>
                      ₹{Number(b.totalCost || 0).toLocaleString()}
                    </span>
                  </div>
                  <div className="flex items-center justify-between gap-2 mt-2">
                    <StatusPill status={b.status} />
                    <div className="flex items-center gap-2">
                      {b.status === "PAID" ? (
                        <button
                          onClick={() => viewReceipt(b)}
                          className="py-2 px-3 rounded-lg bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-medium"
                        >
                          Receipt
                        </button>
                      ) : (
                        <button
                          onClick={() => setActiveBill(b)}
                          className="py-2 px-3 rounded-lg bg-gradient-to-r from-emerald-500 to-cyan-600 text-white text-xs font-semibold"
                        >
                          Pay Now
                        </button>
                      )}
                      <button
                        onClick={() => analyzeWithAI(b)}
                        className="py-2 px-3 rounded-lg bg-cyan-500/10 border border-cyan-500/30 text-cyan-400 text-xs"
                      >
                        🤖
                      </button>
                    </div>
                  </div>
                </motion.div>
              ))}
            </motion.div>
          </>
        )}
      </div>

      <PaymentModal
        bill={activeBill}
        open={!!activeBill}
        onClose={() => setActiveBill(null)}
        onPaid={handlePaid}
      />
    </div>
  );
}
