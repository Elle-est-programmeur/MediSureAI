import { createContext, useContext, useState, useCallback, useRef } from "react";
import { patientAPI } from "../services/api";

const ChatContext = createContext(null);

export function ChatProvider({ children }) {
  const [sessionId, setSessionId] = useState(() => crypto.randomUUID());
  const [messages, setMessages] = useState([]);
  const [isTyping, setIsTyping] = useState(false);
  const inFlightRef = useRef(0);

  const sendMessage = useCallback(async (text) => {
    const query = (text || "").trim();
    if (!query) return;

    const userMsg = { role: "user", content: query, id: Date.now() };
    setMessages((prev) => [...prev, userMsg]);
    setIsTyping(true);
    inFlightRef.current += 1;

    try {
      const res = await patientAPI.chat(query, sessionId);
      const aiMsg = {
        role: "ai",
        content: res.answer || res.finalAnswer || res.response || "(no response)",
        confidence: res.overallConfidence,           // 0-10 scale from backend
        confidenceLevel: res.confidenceLevel,        // LOW / MEDIUM / HIGH
        confidenceFactors: res.confidenceFactors,
        intent: res.detectedIntent,
        processingTimeMs: res.processingTimeMs,
        id: Date.now() + Math.random(),
      };
      setMessages((prev) => [...prev, aiMsg]);
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        {
          role: "ai",
          content: "Sorry, I couldn't process that request. Please try again.",
          id: Date.now() + Math.random(),
          error: true,
        },
      ]);
    } finally {
      inFlightRef.current -= 1;
      if (inFlightRef.current === 0) setIsTyping(false);
    }
  }, [sessionId]);

  const resetChat = useCallback(() => {
    setMessages([]);
    setSessionId(crypto.randomUUID());
  }, []);

  return (
    <ChatContext.Provider value={{ messages, isTyping, sessionId, sendMessage, resetChat }}>
      {children}
    </ChatContext.Provider>
  );
}

export function useChat() {
  const ctx = useContext(ChatContext);
  if (!ctx) throw new Error("useChat must be used within a ChatProvider");
  return ctx;
}
