import { useState, useEffect } from "react";
import { useNavigate, Link } from "react-router-dom";
import { motion } from "framer-motion";
import { useToast } from "../../context/ToastContext";
import { patientAPI } from "../../services/api";
import LoadingSpinner from "../../components/ui/LoadingSpinner";
import EmptyState from "../../components/ui/EmptyState";

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.07 } } };
const item = { hidden: { opacity: 0, y: 20 }, show: { opacity: 1, y: 0 } };

export default function PatientBilling() {
  const navigate = useNavigate();
  const { addToast } = useToast();
  const [billing, setBilling] = useState([]);
  const [loading, setLoading] = useState(true);

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

  const totalAmount = billing.reduce((sum, b) => sum + (b.totalCost || b.amount || 0), 0);

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

        {/* Total summary card */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-8 bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-8 text-center"
        >
          <p className="text-xs font-semibold tracking-[0.2em] uppercase text-slate-400 mb-2" style={{ fontFamily: "Orbitron, sans-serif" }}>
            Total Billing Amount
          </p>
          <p className="text-4xl font-bold bg-gradient-to-r from-cyan-500 to-blue-600 bg-clip-text text-transparent" style={{ fontFamily: "Orbitron, sans-serif" }}>
            ₹{totalAmount.toLocaleString()}
          </p>
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
                      <th className="text-right px-6 py-3 text-xs font-semibold text-slate-400 uppercase tracking-wider">AI Coverage</th>
                    </tr>
                  </thead>
                  <tbody>
                    <motion.tr>
                      {/* just for stagger context */}
                    </motion.tr>
                    {billing.map((b, i) => (
                      <motion.tr
                        key={b.id || i}
                        variants={item}
                        className="border-b border-white/5 hover:bg-white/5 transition-colors"
                      >
                        <td className="px-6 py-4 text-slate-400 text-sm">{b.createdAt || b.date || "—"}</td>
                        <td className="px-6 py-4 text-white text-sm">{b.description || "Billing entry"}</td>
                        <td className="px-6 py-4 text-right">
                          <span className="text-white font-bold" style={{ fontFamily: "Orbitron, sans-serif" }}>
                            ₹{(b.totalCost || b.amount || 0).toLocaleString()}
                          </span>
                        </td>
                        <td className="px-6 py-4 text-right">
                          <button
                            onClick={() => analyzeWithAI(b)}
                            className="text-xs px-3 py-1.5 rounded-lg bg-cyan-500/10 border border-cyan-500/30 text-cyan-400 hover:bg-cyan-500/20 transition-colors"
                          >
                            Analyze
                          </button>
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
                      <p className="text-white text-sm font-medium truncate">{b.description || "Billing entry"}</p>
                      <p className="text-slate-400 text-xs">{b.createdAt || b.date || "—"}</p>
                    </div>
                    <span className="text-white font-bold text-sm" style={{ fontFamily: "Orbitron, sans-serif" }}>
                      ₹{(b.totalCost || b.amount || 0).toLocaleString()}
                    </span>
                  </div>
                  <button
                    onClick={() => analyzeWithAI(b)}
                    className="w-full mt-2 py-2 rounded-lg bg-cyan-500/10 border border-cyan-500/30 text-cyan-400 text-xs font-medium"
                  >
                    🤖 AI Analysis
                  </button>
                </motion.div>
              ))}
            </motion.div>
          </>
        )}
      </div>
    </div>
  );
}
