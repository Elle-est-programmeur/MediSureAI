import { useState, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { patientAPI } from "../services/api";

const METHODS = [
  { id: "UPI", label: "UPI", icon: "📱", subtitle: "Pay via any UPI app" },
  { id: "CARD", label: "Card", icon: "💳", subtitle: "Credit / debit card" },
  { id: "NET_BANKING", label: "Net Banking", icon: "🏦", subtitle: "Direct bank transfer" },
];

/**
 * Mock payment modal. Three stages:
 *   1. select method        — user picks UPI / Card / Net Banking
 *   2. processing (2s)      — cosmetic spinner; API call runs in parallel
 *   3. receipt              — shows the receipt returned by the backend
 *
 * The backend transitions the bill PENDING → PROCESSING → PAID and issues a
 * Receipt in a single endpoint call; this UI just paces the visual feedback.
 */
export default function PaymentModal({ bill, open, onClose, onPaid }) {
  const [stage, setStage] = useState("select"); // select | processing | receipt | error
  const [method, setMethod] = useState("UPI");
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (open) {
      setStage("select");
      setMethod("UPI");
      setResult(null);
      setError(null);
    }
  }, [open, bill?.id]);

  async function confirmPayment() {
    setStage("processing");
    setError(null);

    const minDelay = new Promise((r) => setTimeout(r, 2000));
    try {
      const [updated] = await Promise.all([
        patientAPI.payBilling(bill.id, method),
        minDelay,
      ]);
      setResult(updated);
      setStage("receipt");
      if (onPaid) onPaid(updated);
    } catch (err) {
      setError(err.response?.data?.message || "Payment failed. Please try again.");
      setStage("error");
    }
  }

  if (!open || !bill) return null;

  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm"
        onClick={() => stage !== "processing" && onClose()}
      >
        <motion.div
          initial={{ scale: 0.95, opacity: 0, y: 20 }}
          animate={{ scale: 1, opacity: 1, y: 0 }}
          exit={{ scale: 0.95, opacity: 0, y: 20 }}
          transition={{ type: "spring", stiffness: 300, damping: 25 }}
          onClick={(e) => e.stopPropagation()}
          className="w-full max-w-md bg-slate-900 border border-white/10 rounded-2xl shadow-2xl overflow-hidden"
        >
          {/* ── Header ── */}
          <div className="px-6 py-4 border-b border-white/10 flex items-center justify-between">
            <div>
              <h2 className="text-base font-bold text-white" style={{ fontFamily: "Orbitron, sans-serif" }}>
                {stage === "receipt" ? "Payment Receipt" : "Pay Now"}
              </h2>
              <p className="text-[10px] uppercase tracking-wider text-slate-500 mt-0.5">
                {stage === "receipt" ? "Transaction completed" : `Bill #${bill.id}`}
              </p>
            </div>
            {stage !== "processing" && (
              <button onClick={onClose} className="text-slate-400 hover:text-white text-xl leading-none">×</button>
            )}
          </div>

          {/* ── Body ── */}
          <div className="p-6">
            {stage === "select" && (
              <SelectStage bill={bill} method={method} setMethod={setMethod} onConfirm={confirmPayment} />
            )}
            {stage === "processing" && <ProcessingStage method={method} amount={bill.totalCost} />}
            {stage === "receipt" && <ReceiptStage bill={result} onClose={onClose} />}
            {stage === "error" && (
              <ErrorStage error={error} onRetry={() => setStage("select")} onClose={onClose} />
            )}
          </div>
        </motion.div>
      </motion.div>
    </AnimatePresence>
  );
}

function SelectStage({ bill, method, setMethod, onConfirm }) {
  return (
    <>
      {/* Amount banner */}
      <div className="mb-6 text-center p-4 rounded-xl bg-gradient-to-br from-cyan-500/10 to-blue-600/10 border border-cyan-500/20">
        <p className="text-[10px] uppercase tracking-[0.2em] text-slate-400 mb-1" style={{ fontFamily: "Orbitron, sans-serif" }}>
          Amount Due
        </p>
        <p className="text-4xl font-bold bg-gradient-to-r from-cyan-400 to-blue-500 bg-clip-text text-transparent" style={{ fontFamily: "Orbitron, sans-serif" }}>
          ₹{Number(bill.totalCost || 0).toLocaleString()}
        </p>
        {bill.description && <p className="text-slate-400 text-xs mt-2">{bill.description}</p>}
      </div>

      {/* Method picker */}
      <p className="text-[10px] uppercase tracking-[0.2em] text-cyan-400 font-semibold mb-3" style={{ fontFamily: "Orbitron, sans-serif" }}>
        Choose Payment Method
      </p>
      <div className="space-y-2 mb-6">
        {METHODS.map((m) => (
          <button
            key={m.id}
            onClick={() => setMethod(m.id)}
            className={`w-full p-3 rounded-xl border text-left transition-all flex items-center gap-3 ${
              method === m.id
                ? "bg-cyan-500/10 border-cyan-500/50 ring-1 ring-cyan-500/30"
                : "bg-white/5 border-white/10 hover:border-white/20"
            }`}
          >
            <span className="text-2xl shrink-0">{m.icon}</span>
            <div className="flex-1 min-w-0">
              <p className="text-white text-sm font-medium">{m.label}</p>
              <p className="text-slate-400 text-xs">{m.subtitle}</p>
            </div>
            <div
              className={`w-4 h-4 rounded-full border-2 shrink-0 ${
                method === m.id ? "border-cyan-400 bg-cyan-400" : "border-slate-500"
              }`}
            >
              {method === m.id && <div className="w-full h-full rounded-full bg-cyan-400 ring-2 ring-cyan-400/30" />}
            </div>
          </button>
        ))}
      </div>

      <button
        onClick={onConfirm}
        className="w-full py-3 rounded-xl bg-gradient-to-r from-emerald-500 to-cyan-600 text-white font-semibold hover:scale-[1.02] transition-transform shadow-lg shadow-emerald-500/25"
      >
        Pay ₹{Number(bill.totalCost || 0).toLocaleString()}
      </button>
      <p className="mt-3 text-[10px] text-slate-500 text-center">Mock gateway — no real charge is made.</p>
    </>
  );
}

function ProcessingStage({ method, amount }) {
  return (
    <div className="py-8 text-center">
      <motion.div
        animate={{ rotate: 360 }}
        transition={{ duration: 1.2, repeat: Infinity, ease: "linear" }}
        className="w-16 h-16 mx-auto mb-6 border-4 border-cyan-500/20 border-t-cyan-400 rounded-full"
      />
      <h3 className="text-white text-base font-bold mb-1" style={{ fontFamily: "Orbitron, sans-serif" }}>
        Processing payment…
      </h3>
      <p className="text-slate-400 text-xs">
        ₹{Number(amount || 0).toLocaleString()} via {method.replace("_", " ")}
      </p>
      <motion.div
        initial={{ width: 0 }}
        animate={{ width: "100%" }}
        transition={{ duration: 2, ease: "linear" }}
        className="mt-6 h-1 bg-gradient-to-r from-cyan-400 to-blue-500 rounded-full mx-auto"
        style={{ maxWidth: 240 }}
      />
      <p className="mt-4 text-[10px] uppercase tracking-[0.2em] text-slate-500">Please do not close this window</p>
    </div>
  );
}

function ReceiptStage({ bill, onClose }) {
  const receipt = bill?.receipt;
  return (
    <div>
      <motion.div
        initial={{ scale: 0 }}
        animate={{ scale: 1 }}
        transition={{ type: "spring", stiffness: 300, damping: 18 }}
        className="w-16 h-16 mx-auto mb-4 rounded-full bg-emerald-500/20 border border-emerald-500/40 flex items-center justify-center"
      >
        <span className="text-3xl">✓</span>
      </motion.div>
      <h3 className="text-center text-white text-base font-bold mb-1" style={{ fontFamily: "Orbitron, sans-serif" }}>
        Payment Successful
      </h3>
      <p className="text-center text-slate-400 text-xs mb-5">Thank you. Your receipt is below.</p>

      <div className="bg-white/5 border border-white/10 rounded-xl p-4 space-y-2 text-sm">
        <Row label="Receipt #" value={receipt?.receiptNumber || "—"} mono />
        <Row label="Txn ID" value={receipt?.transactionRef || "—"} mono />
        <Row label="Amount" value={`₹${Number(receipt?.amount || bill?.totalCost || 0).toLocaleString()}`} strong />
        <Row label="Method" value={(receipt?.paymentMethod || bill?.paymentMethod || "—").replace("_", " ")} />
        <Row label="Date" value={(receipt?.issuedAt || bill?.paidAt || "").toString().slice(0, 19).replace("T", " ")} />
        {receipt?.patientName && <Row label="Patient" value={receipt.patientName} />}
        {receipt?.description && <Row label="For" value={receipt.description} />}
      </div>

      <button
        onClick={onClose}
        className="mt-5 w-full py-3 rounded-xl bg-cyan-500/10 border border-cyan-500/30 text-cyan-400 font-medium hover:bg-cyan-500/20 transition-colors"
      >
        Done
      </button>
    </div>
  );
}

function Row({ label, value, mono, strong }) {
  return (
    <div className="flex justify-between items-center gap-3">
      <span className="text-slate-400 text-xs uppercase tracking-wider">{label}</span>
      <span
        className={`text-white text-xs text-right truncate ${strong ? "font-bold text-base" : ""}`}
        style={mono || strong ? { fontFamily: "Orbitron, sans-serif" } : undefined}
      >
        {value}
      </span>
    </div>
  );
}

function ErrorStage({ error, onRetry, onClose }) {
  return (
    <div className="text-center py-4">
      <div className="w-14 h-14 mx-auto mb-3 rounded-full bg-red-500/20 border border-red-500/40 flex items-center justify-center text-2xl">!</div>
      <h3 className="text-white text-base font-bold mb-1">Payment failed</h3>
      <p className="text-slate-400 text-xs mb-5">{error}</p>
      <div className="flex gap-2">
        <button onClick={onClose} className="flex-1 py-2.5 rounded-xl bg-white/5 border border-white/10 text-slate-300 text-sm">Close</button>
        <button onClick={onRetry} className="flex-1 py-2.5 rounded-xl bg-cyan-500/10 border border-cyan-500/30 text-cyan-400 text-sm font-medium">Try again</button>
      </div>
    </div>
  );
}
