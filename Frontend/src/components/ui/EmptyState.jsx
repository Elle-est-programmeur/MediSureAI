import { motion } from "framer-motion";

export default function EmptyState({ icon = "📭", title, subtitle, actionLabel, onAction }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5 }}
      className="flex flex-col items-center justify-center py-16 px-6 text-center"
    >
      {/* Decorative grid pattern behind the icon */}
      <div className="relative mb-6">
        <div className="absolute -inset-8 rounded-full bg-gradient-to-br from-cyan-500/5 to-blue-600/5" />
        <motion.span
          className="relative text-6xl block"
          animate={{ y: [0, -6, 0] }}
          transition={{ duration: 3, repeat: Infinity, ease: "easeInOut" }}
        >
          {icon}
        </motion.span>
      </div>

      <h3 className="text-lg font-semibold text-white mb-2" style={{ fontFamily: "Space Grotesk, sans-serif" }}>
        {title}
      </h3>

      {subtitle && (
        <p className="text-sm text-slate-400 max-w-sm">{subtitle}</p>
      )}

      {actionLabel && onAction && (
        <button
          onClick={onAction}
          className="mt-6 px-6 py-2.5 bg-gradient-to-r from-cyan-500 to-blue-600 text-white rounded-xl font-semibold text-sm hover:scale-105 transition-transform shadow-lg shadow-cyan-500/25"
        >
          {actionLabel}
        </button>
      )}
    </motion.div>
  );
}
