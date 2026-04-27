const STATUS_MAP = {
  PROCESSED: { bg: "bg-emerald-500/15", text: "text-emerald-400", border: "border-emerald-500/30" },
  ACTIVE: { bg: "bg-emerald-500/15", text: "text-emerald-400", border: "border-emerald-500/30" },
  COMPLETED: { bg: "bg-emerald-500/15", text: "text-emerald-400", border: "border-emerald-500/30" },
  PROCESSING: { bg: "bg-amber-500/15", text: "text-amber-400", border: "border-amber-500/30" },
  PENDING: { bg: "bg-amber-500/15", text: "text-amber-400", border: "border-amber-500/30" },
  FAILED: { bg: "bg-red-500/15", text: "text-red-400", border: "border-red-500/30" },
  ERROR: { bg: "bg-red-500/15", text: "text-red-400", border: "border-red-500/30" },
  UPLOADED: { bg: "bg-blue-500/15", text: "text-blue-400", border: "border-blue-500/30" },
};

const DEFAULT_STYLE = { bg: "bg-slate-500/15", text: "text-slate-400", border: "border-slate-500/30" };

export default function StatusBadge({ status }) {
  const upper = (status || "").toUpperCase();
  const style = STATUS_MAP[upper] || DEFAULT_STYLE;

  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-semibold tracking-wide border ${style.bg} ${style.text} ${style.border}`}
    >
      {status}
    </span>
  );
}
