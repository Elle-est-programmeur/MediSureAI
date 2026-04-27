import { createContext, useContext, useState, useCallback } from "react";
import { AnimatePresence, motion } from "framer-motion";

const ToastContext = createContext(null);

let toastId = 0;

const TOAST_COLORS = {
  success: { bg: "rgba(16, 185, 129, 0.15)", border: "rgba(16, 185, 129, 0.4)", icon: "✓", color: "#10b981" },
  error: { bg: "rgba(239, 68, 68, 0.15)", border: "rgba(239, 68, 68, 0.4)", icon: "✕", color: "#ef4444" },
  info: { bg: "rgba(6, 182, 212, 0.15)", border: "rgba(6, 182, 212, 0.4)", icon: "ℹ", color: "#06b6d4" },
  warning: { bg: "rgba(245, 158, 11, 0.15)", border: "rgba(245, 158, 11, 0.4)", icon: "⚠", color: "#f59e0b" },
};

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const addToast = useCallback((message, type = "info", duration = 3000) => {
    const id = ++toastId;
    setToasts((prev) => [...prev.slice(-2), { id, message, type }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, duration);
  }, []);

  const removeToast = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  return (
    <ToastContext.Provider value={{ addToast }}>
      {children}
      {/* Toast container */}
      <div className="fixed top-4 right-4 z-[9999] flex flex-col gap-3 pointer-events-none">
        <AnimatePresence>
          {toasts.map((toast) => {
            const style = TOAST_COLORS[toast.type] || TOAST_COLORS.info;
            return (
              <motion.div
                key={toast.id}
                initial={{ opacity: 0, x: 80, scale: 0.95 }}
                animate={{ opacity: 1, x: 0, scale: 1 }}
                exit={{ opacity: 0, x: 80, scale: 0.95 }}
                transition={{ duration: 0.3, ease: [0.22, 1, 0.36, 1] }}
                className="pointer-events-auto flex items-center gap-3 min-w-[300px] max-w-[420px] px-4 py-3 rounded-xl backdrop-blur-xl shadow-2xl"
                style={{
                  background: style.bg,
                  border: `1px solid ${style.border}`,
                }}
              >
                <span
                  className="flex items-center justify-center w-6 h-6 rounded-full text-xs font-bold"
                  style={{ background: style.color, color: "#fff" }}
                >
                  {style.icon}
                </span>
                <span className="flex-1 text-sm text-white font-medium">{toast.message}</span>
                <button
                  onClick={() => removeToast(toast.id)}
                  className="text-white/50 hover:text-white transition text-lg leading-none"
                >
                  ×
                </button>
              </motion.div>
            );
          })}
        </AnimatePresence>
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error("useToast must be used within a ToastProvider");
  }
  return context;
}
