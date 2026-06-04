package com.example.Backend.service.srlm;

import com.example.Backend.dto.RetrievedChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Gatekeeper between retrieval and reasoning.
 *
 * Returns a {@link Result} that the orchestrator can branch on:
 * accept the context, retry retrieval with a refined query, or short-circuit
 * with a "policy does not contain enough information" response.
 *
 * Heuristics:
 *  - At least one query keyword group must appear in at least one chunk.
 *  - At least one chunk must clear a minimum finalScore.
 *  - Total context length must be over a floor.
 */
@Service
@Slf4j
public class ContextValidationService {

    @Value("${srlm.validation.min-final-score:0.45}")
    private double minFinalScore;

    @Value("${srlm.validation.min-context-chars:120}")
    private int minContextChars;

    public Result validate(String query, List<RetrievedChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return new Result(false, true, "No chunks retrieved", List.of(), List.of());
        }

        Set<String> queryGroups = InsuranceKeywords.matchedGroups(query);
        int totalChars = 0;
        double maxScore = 0.0;
        Set<String> seenGroups = new HashSet<>();
        for (RetrievedChunk c : chunks) {
            totalChars += c.getContent() == null ? 0 : c.getContent().length();
            maxScore = Math.max(maxScore, c.getFinalScore());
            seenGroups.addAll(InsuranceKeywords.matchedGroups(c.getContent()));
        }

        // Which expected query groups DID make it into context
        List<String> requiredMissing = new ArrayList<>();
        for (String g : queryGroups) {
            if (!seenGroups.contains(g)) requiredMissing.add(g);
        }

        boolean enoughEvidence = maxScore >= minFinalScore;
        boolean enoughText = totalChars >= minContextChars;
        // If the query had recognised insurance terms but NONE appear in any chunk,
        // the retrieval is almost certainly off-target — ask for a retry.
        boolean topicCovered = queryGroups.isEmpty() || requiredMissing.size() < queryGroups.size();

        boolean ok = enoughEvidence && enoughText && topicCovered;
        boolean retry = !topicCovered || maxScore < minFinalScore;

        String reason = ok ? "OK"
                : !enoughText ? "Context too short (" + totalChars + " chars)"
                : !topicCovered ? "No query keywords (" + queryGroups + ") found in any retrieved chunk"
                : "Top finalScore " + maxScore + " < " + minFinalScore;

        if (!ok) {
            log.warn("Context validation failed: {} — queryGroups={}, seenGroups={}, missing={}",
                    reason, queryGroups, seenGroups, requiredMissing);
        }
        return new Result(ok, retry, reason,
                new ArrayList<>(queryGroups),
                requiredMissing);
    }

    public record Result(
            boolean valid,
            boolean shouldRetry,
            String reason,
            List<String> queryEntities,
            List<String> missingEntities) {}
}
