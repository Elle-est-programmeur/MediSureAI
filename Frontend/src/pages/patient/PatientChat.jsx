import { useState, useEffect, useRef } from "react";
import { useSearchParams } from "react-router-dom";
import { motion, AnimatePresence } from "framer-motion";
import { useToast } from "../../context/ToastContext";
import { patientAPI } from "../../services/api";
import LoadingSpinner from "../../components/ui/LoadingSpinner";

const QUICK_PROMPTS = [
  "Explain my latest billing",
  "Does my insurance cover my diagnosis?",
  "What are the side effects of my prescribed drugs?",
  "Summarize my health timeline",
];

function formatAIText(text) {
  if (!text) return "";
  return text
    .replace(/\*\*(.*?)\*\*/g, "<strong>$1</strong>")
    .replace(/^- (.+)$/gm, "<li>$1</li>")
    .replace(/(<li>.*<\/li>)/gs, "<ul class='list-disc list-inside space-y-1 my-2'>$1</ul>")
    .replace(/\n/g, "<br />");
}

export default function PatientChat() {
  const { addToast } = useToast();
  const [searchParams] = useSearchParams();
  const messagesEndRef = useRef(null);
  const textareaRef = useRef(null);

  const [sessionId] = useState(() => crypto.randomUUID());
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const [isTyping, setIsTyping] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(true);

  // Pre-fill from URL params
  useEffect(() => {
    const context = searchParams.get("context");
    const prefill = searchParams.get("prefill");
    if (context?.startsWith("record_")) {
      setInput("Tell me about my medical record and treatment details");
    } else if (prefill) {
      setInput(decodeURIComponent(prefill));
    }
  }, [searchParams]);

  // Auto-scroll to bottom
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, isTyping]);

  // Auto-resize textarea
  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = "auto";
      textareaRef.current.style.height = Math.min(textareaRef.current.scrollHeight, 120) + "px";
    }
  }, [input]);

  async function sendMessage(text) {
    const query = (text || input).trim();
    if (!query) return;

    const userMsg = { role: "user", content: query, id: Date.now() };
    setMessages((prev) => [...prev, userMsg]);
    setInput("");
    setIsTyping(true);

    try {
      const res = await patientAPI.chat(query, sessionId);
      const aiMsg = {
        role: "ai",
        content: res.response || res.answer || res.message || JSON.stringify(res),
        confidence: res.confidenceScore || res.confidence,
        intent: res.detectedIntent || res.intent,
        id: Date.now() + 1,
      };
      setMessages((prev) => [...prev, aiMsg]);
    } catch (err) {
      addToast("Failed to get AI response", "error");
      setMessages((prev) => [
        ...prev,
        { role: "ai", content: "I'm sorry, I couldn't process that request. Please try again.", id: Date.now() + 1 },
      ]);
    } finally {
      setIsTyping(false);
    }
  }

  function handleKeyDown(e) {
    if (e.key === "Enter" && e.ctrlKey) {
      e.preventDefault();
      sendMessage();
    }
  }

  return (
    <div className="flex-1 flex overflow-hidden">
      {/* ── Sidebar: prompts ── */}
      <AnimatePresence>
        {sidebarOpen && (
          <motion.aside
            initial={{ x: -300, opacity: 0 }}
            animate={{ x: 0, opacity: 1 }}
            exit={{ x: -300, opacity: 0 }}
            transition={{ type: "spring", stiffness: 300, damping: 30 }}
            className="hidden sm:flex w-72 shrink-0 flex-col bg-slate-900/80 backdrop-blur-xl border-r border-white/10 p-4"
          >
            <div className="mb-6">
              <h2 className="text-xs font-semibold tracking-[0.2em] uppercase text-cyan-400 mb-1" style={{ fontFamily: "Orbitron, sans-serif" }}>
                AI Chat
              </h2>
              <p className="text-slate-400 text-xs">Ask about your health records, billing, and insurance</p>
            </div>

            {/* Quick Prompts */}
            <div className="mb-6">
              <h3 className="text-[10px] font-semibold uppercase text-slate-500 tracking-wider mb-3">Quick Prompts</h3>
              <div className="space-y-2">
                {QUICK_PROMPTS.map((prompt, i) => (
                  <button
                    key={i}
                    onClick={() => sendMessage(prompt)}
                    className="w-full text-left px-3 py-2.5 rounded-xl bg-white/5 border border-white/10 text-slate-300 text-sm hover:bg-white/10 hover:border-cyan-500/20 hover:text-white transition-all"
                  >
                    {prompt}
                  </button>
                ))}
              </div>
            </div>

            {/* Session info */}
            <div className="mt-auto pt-4 border-t border-white/10">
              <p className="text-[10px] text-slate-500 uppercase tracking-wider">Session</p>
              <p className="text-xs text-slate-400 font-mono truncate">{sessionId.slice(0, 8)}...</p>
            </div>
          </motion.aside>
        )}
      </AnimatePresence>

      {/* ── Main chat area ── */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Toggle sidebar button */}
        <div className="hidden sm:flex shrink-0 items-center px-4 h-10 border-b border-white/5">
          <button
            onClick={() => setSidebarOpen(!sidebarOpen)}
            className="text-slate-400 hover:text-white text-sm transition-colors"
          >
            {sidebarOpen ? "◀ Hide" : "▶ Prompts"}
          </button>
        </div>

        {/* Messages */}
        <div className="flex-1 overflow-y-auto p-4 sm:p-6 space-y-4">
          {messages.length === 0 && (
            <div className="flex-1 flex flex-col items-center justify-center text-center py-16">
              <motion.div
                animate={{ y: [0, -8, 0] }}
                transition={{ duration: 3, repeat: Infinity }}
                className="text-5xl mb-4"
              >
                🤖
              </motion.div>
              <h2 className="text-xl font-bold text-white mb-2" style={{ fontFamily: "Orbitron, sans-serif" }}>
                MediSure AI Chat
              </h2>
              <p className="text-slate-400 text-sm max-w-md">
                Ask me about your medical records, billing entries, insurance coverage, drug side effects, or anything related to your health data.
              </p>

              {/* Mobile quick prompts */}
              <div className="sm:hidden mt-6 flex flex-wrap gap-2 justify-center">
                {QUICK_PROMPTS.map((prompt, i) => (
                  <button
                    key={i}
                    onClick={() => sendMessage(prompt)}
                    className="px-3 py-2 rounded-xl bg-white/5 border border-white/10 text-slate-300 text-xs hover:bg-white/10 transition-colors"
                  >
                    {prompt}
                  </button>
                ))}
              </div>
            </div>
          )}

          {messages.map((msg) => (
            <motion.div
              key={msg.id}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              className={`flex ${msg.role === "user" ? "justify-end" : "justify-start"}`}
            >
              <div
                className={`max-w-[85%] sm:max-w-[70%] px-4 py-3 text-sm ${
                  msg.role === "user"
                    ? "bg-gradient-to-br from-cyan-500/20 to-blue-600/20 border border-cyan-500/20 rounded-2xl rounded-tr-sm text-white"
                    : "bg-white/5 border border-white/10 rounded-2xl rounded-tl-sm text-slate-200"
                }`}
              >
                {msg.role === "ai" ? (
                  <div
                    className="prose prose-sm prose-invert max-w-none [&_strong]:text-cyan-300 [&_li]:text-slate-300"
                    dangerouslySetInnerHTML={{ __html: formatAIText(msg.content) }}
                  />
                ) : (
                  <p>{msg.content}</p>
                )}

                {/* AI meta badges */}
                {msg.role === "ai" && (msg.confidence || msg.intent) && (
                  <div className="flex gap-2 mt-2 pt-2 border-t border-white/5">
                    {msg.confidence && (
                      <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">
                        Confidence: {Math.round(msg.confidence * 100) || msg.confidence}%
                      </span>
                    )}
                    {msg.intent && (
                      <span className="text-[10px] px-2 py-0.5 rounded-full bg-violet-500/10 border border-violet-500/20 text-violet-400">
                        {msg.intent}
                      </span>
                    )}
                  </div>
                )}
              </div>
            </motion.div>
          ))}

          {/* Typing indicator */}
          {isTyping && (
            <div className="flex justify-start">
              <div className="bg-white/5 border border-white/10 rounded-2xl rounded-tl-sm px-5 py-3 flex items-center gap-1.5">
                {[0, 1, 2].map((i) => (
                  <motion.div
                    key={i}
                    className="w-2 h-2 rounded-full bg-cyan-400"
                    animate={{ y: [0, -6, 0] }}
                    transition={{ duration: 0.6, repeat: Infinity, delay: i * 0.15 }}
                  />
                ))}
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* ── Input bar ── */}
        <div className="shrink-0 p-4 border-t border-white/10 bg-slate-900/50 backdrop-blur-xl">
          <div className="flex gap-2 max-w-4xl mx-auto">
            <textarea
              ref={textareaRef}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Ask about your records or insurance... (Ctrl+Enter to send)"
              rows={1}
              className="flex-1 bg-white/5 border border-white/20 rounded-xl px-4 py-3 text-white text-sm placeholder-slate-500 focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500 outline-none transition resize-none"
            />
            <button
              onClick={() => sendMessage()}
              disabled={isTyping || !input.trim()}
              className="self-end px-4 py-3 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 text-white font-semibold hover:scale-105 transition-transform shadow-lg shadow-cyan-500/25 disabled:opacity-50 disabled:hover:scale-100"
            >
              {isTyping ? <LoadingSpinner size="sm" /> : "✈"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
