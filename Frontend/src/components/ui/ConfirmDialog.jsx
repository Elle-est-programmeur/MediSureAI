import { motion, AnimatePresence } from "framer-motion";

export default function ConfirmDialog({ open, title, message, onConfirm, onCancel, confirmLabel = "Confirm", danger = false }) {
  return (
    <AnimatePresence>
      {open && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 z-[9998] flex items-center justify-center p-4"
        >
          {/* Backdrop */}
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={onCancel} />

          {/* Dialog */}
          <motion.div
            initial={{ opacity: 0, scale: 0.9, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.9, y: 20 }}
            transition={{ type: "spring", stiffness: 300, damping: 30 }}
            className="relative bg-slate-900/95 backdrop-blur-xl border border-white/10 rounded-2xl p-6 max-w-sm w-full shadow-2xl"
          >
            <h3 className="text-lg font-semibold text-white mb-2" style={{ fontFamily: "Space Grotesk, sans-serif" }}>
              {title}
            </h3>
            <p className="text-sm text-slate-400 mb-6">{message}</p>

            <div className="flex justify-end gap-3">
              <button
                onClick={onCancel}
                className="px-4 py-2 rounded-xl bg-white/10 border border-white/20 text-white text-sm font-medium hover:bg-white/20 transition"
              >
                Cancel
              </button>
              <button
                onClick={onConfirm}
                className={`px-4 py-2 rounded-xl text-white text-sm font-semibold transition hover:scale-105 shadow-lg ${
                  danger
                    ? "bg-gradient-to-r from-red-500 to-red-600 shadow-red-500/25"
                    : "bg-gradient-to-r from-cyan-500 to-blue-600 shadow-cyan-500/25"
                }`}
              >
                {confirmLabel}
              </button>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
