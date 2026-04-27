import { motion } from "framer-motion";

const SIZES = {
  sm: { outer: "w-6 h-6", inner: "w-4 h-4", border: "border-2" },
  md: { outer: "w-10 h-10", inner: "w-7 h-7", border: "border-2" },
  lg: { outer: "w-16 h-16", inner: "w-11 h-11", border: "border-[3px]" },
};

export default function LoadingSpinner({ size = "md" }) {
  const s = SIZES[size] || SIZES.md;

  return (
    <div className="flex items-center justify-center">
      <div className="relative">
        {/* Outer ring — clockwise */}
        <motion.div
          className={`${s.outer} rounded-full ${s.border} border-transparent border-t-cyan-400 border-r-cyan-400/50`}
          animate={{ rotate: 360 }}
          transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
        />
        {/* Inner ring — counter-clockwise */}
        <motion.div
          className={`absolute inset-0 m-auto ${s.inner} rounded-full ${s.border} border-transparent border-b-blue-500 border-l-blue-500/50`}
          animate={{ rotate: -360 }}
          transition={{ duration: 0.75, repeat: Infinity, ease: "linear" }}
        />
      </div>
    </div>
  );
}
