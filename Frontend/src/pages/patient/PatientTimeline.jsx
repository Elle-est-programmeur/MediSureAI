import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { useToast } from "../../context/ToastContext";
import { patientAPI } from "../../services/api";
import LoadingSpinner from "../../components/ui/LoadingSpinner";
import EmptyState from "../../components/ui/EmptyState";

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.1 } } };
const item = { hidden: { opacity: 0, x: -20 }, show: { opacity: 1, x: 0 } };

const TYPE_ICON = {
  RECORD: "📋",
  DOCUMENT: "📄",
  BILLING: "💳",
};

function formatDate(d) {
  if (!d) return "—";
  const s = String(d);
  return s.length > 10 ? s.slice(0, 10) : s;
}

export default function PatientTimeline() {
  const { addToast } = useToast();
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchTimeline();
  }, []);

  async function fetchTimeline() {
    try {
      const data = await patientAPI.getTimeline();
      setEvents(Array.isArray(data) ? data : data?.events || data?.timeline || []);
    } catch {
      addToast("Failed to load timeline", "error");
    } finally {
      setLoading(false);
    }
  }

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  return (
    <div className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8">
      <div className="max-w-3xl mx-auto">
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-white" style={{ fontFamily: "Orbitron, sans-serif" }}>
            Health Timeline
          </h1>
          <p className="text-slate-400 text-sm mt-1">A chronological view of your health events</p>
        </div>

        {events.length === 0 ? (
          <EmptyState
            icon="📅"
            title="No timeline events"
            subtitle="Your health timeline will populate as records and documents are added."
          />
        ) : (
          <motion.div variants={container} initial="hidden" animate="show" className="relative">
            {/* Vertical line */}
            <div className="absolute left-5 sm:left-8 top-0 bottom-0 w-px bg-gradient-to-b from-cyan-500/50 via-blue-500/30 to-transparent" />

            {events.map((event, i) => {
              const type = event.type || event.eventType;
              const icon = TYPE_ICON[type] || "📌";
              return (
                <motion.div
                  key={event.id || i}
                  variants={item}
                  className="relative pl-14 sm:pl-20 pb-8 last:pb-0"
                >
                  {/* Dot on timeline */}
                  <div className="absolute left-3.5 sm:left-6.5 top-1.5 w-3 h-3 rounded-full bg-cyan-400 shadow-lg shadow-cyan-500/30 ring-4 ring-slate-900" />

                  {/* Date label */}
                  <div className="text-[10px] font-semibold uppercase tracking-wider text-slate-500 mb-1">
                    {formatDate(event.date || event.createdAt || event.timestamp)} {type && <span className="ml-2 text-cyan-400/80">{type}</span>}
                  </div>

                  {/* Event card */}
                  <motion.div
                    whileHover={{ scale: 1.01, y: -1 }}
                    className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-xl p-4 hover:border-cyan-500/20 transition-colors"
                  >
                    <div className="flex items-start gap-3">
                      <span className="text-xl shrink-0">{icon}</span>
                      <div className="min-w-0 flex-1">
                        <h3 className="text-white font-medium text-sm">
                          {event.title || event.description || event.diagnosis || "Event"}
                        </h3>
                        {event.details && (
                          <p className="text-slate-400 text-xs mt-1 whitespace-pre-wrap line-clamp-3">{event.details}</p>
                        )}
                        {event.doctorName && (
                          <p className="text-slate-500 text-xs mt-1">Dr. {event.doctorName}</p>
                        )}
                      </div>
                    </div>

                    {/* Tags */}
                    {Array.isArray(event.tags) && event.tags.length > 0 && (
                      <div className="mt-3 flex flex-wrap gap-1.5">
                        {event.tags.slice(0, 6).map((tag, j) => (
                          <span
                            key={j}
                            className="text-[10px] px-2 py-0.5 rounded-full bg-cyan-500/10 border border-cyan-500/20 text-cyan-400"
                          >
                            {typeof tag === "string" ? tag : tag.name || tag}
                          </span>
                        ))}
                      </div>
                    )}
                  </motion.div>
                </motion.div>
              );
            })}
          </motion.div>
        )}
      </div>
    </div>
  );
}
