import { Link, useLocation } from "react-router-dom";
import { motion, AnimatePresence } from "framer-motion";
import { useChat } from "../context/ChatContext";

/**
 * Floating "AI is thinking..." chip. Visible whenever a chat request is in flight
 * AND the user is NOT currently on the chat page. Click to jump back to the chat.
 */
export default function ChatThinkingBadge() {
  const { isTyping } = useChat();
  const location = useLocation();
  const onChatPage = location.pathname === "/patient/chat";
  const show = isTyping && !onChatPage && location.pathname.startsWith("/patient");

  return (
    <AnimatePresence>
      {show && (
        <motion.div
          initial={{ opacity: 0, y: 20, scale: 0.95 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          exit={{ opacity: 0, y: 20, scale: 0.95 }}
          transition={{ type: "spring", stiffness: 320, damping: 28 }}
          className="fixed bottom-5 right-5 z-50"
        >
          <Link
            to="/patient/chat"
            className="flex items-center gap-3 px-4 py-2.5 rounded-full bg-slate-900/95 backdrop-blur-xl border border-cyan-500/30 shadow-lg shadow-cyan-500/20 hover:border-cyan-500/60 transition-colors"
          >
            <span className="flex gap-1">
              {[0, 1, 2].map((i) => (
                <motion.span
                  key={i}
                  className="w-1.5 h-1.5 rounded-full bg-cyan-400"
                  animate={{ y: [0, -4, 0] }}
                  transition={{ duration: 0.6, repeat: Infinity, delay: i * 0.15 }}
                />
              ))}
            </span>
            <span className="text-xs text-white font-medium">AI is thinking…</span>
            <span className="text-[10px] text-cyan-400">tap to view</span>
          </Link>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
