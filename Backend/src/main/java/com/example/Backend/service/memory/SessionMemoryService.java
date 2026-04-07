package com.example.Backend.service.memory;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SessionMemoryService {

    @Value("${session.memory.max-queries:10}")
    private int maxQueries;

    private final Map<String, SessionContext> sessions = new ConcurrentHashMap<>();

    public void saveQueryContext(String sessionId, String query, String answer) {
        SessionContext context = sessions.computeIfAbsent(sessionId, k -> new SessionContext());
        context.addQuery(query, answer, maxQueries);
        log.debug("Session {}: saved query (total={})", sessionId, context.getQueryHistory().size());
    }

    public List<QueryRecord> getSessionHistory(String sessionId) {
        SessionContext context = sessions.get(sessionId);
        return context != null ? context.getQueryHistory() : List.of();
    }

    /**
     * Returns the last {@code lastN} Q&A turns formatted as plain text,
     * ready to be prepended to an LLM prompt.
     */
    public String getSessionContext(String sessionId, int lastN) {
        SessionContext context = sessions.get(sessionId);
        if (context == null || context.getQueryHistory().isEmpty()) {
            return "";
        }

        List<QueryRecord> history = context.getQueryHistory();
        int start = Math.max(0, history.size() - lastN);

        StringBuilder sb = new StringBuilder("Previous conversation:\n");
        for (int i = start; i < history.size(); i++) {
            QueryRecord record = history.get(i);
            sb.append("Q: ").append(record.getQuery()).append("\n")
              .append("A: ").append(record.getAnswer()).append("\n\n");
        }
        return sb.toString().trim();
    }

    public void clearSession(String sessionId) {
        sessions.remove(sessionId);
        log.info("Cleared session: {}", sessionId);
    }

    // ── Inner types ───────────────────────────────────────────────────────────

    @Data
    private static class SessionContext {
        private final List<QueryRecord> queryHistory = new ArrayList<>();
        private LocalDateTime lastActivity = LocalDateTime.now();

        void addQuery(String query, String answer, int maxQueries) {
            queryHistory.add(new QueryRecord(query, answer, LocalDateTime.now()));
            lastActivity = LocalDateTime.now();
            if (queryHistory.size() > maxQueries) {
                queryHistory.remove(0);
            }
        }
    }

    @Data
    public static class QueryRecord {
        private final String query;
        private final String answer;
        private final LocalDateTime timestamp;
    }
}