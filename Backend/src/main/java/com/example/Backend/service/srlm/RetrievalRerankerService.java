package com.example.Backend.service.srlm;

import com.example.Backend.dto.RetrievedChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Re-ranks retrieved chunks before they reach the reasoning stage.
 *
 * Boosts:
 *  - direct clause matches (e.g. the chunk literally contains "ICU Charges")
 *  - explicit affirmative coverage cues ("covered up to", "is covered")
 *  - explicit exclusion cues when the query is about exclusions
 *
 * Penalises:
 *  - very short chunks (likely fragments)
 *  - chunks with no overlap with query keywords
 */
@Service
@Slf4j
public class RetrievalRerankerService {

    @Value("${srlm.rerank.direct-match-boost:0.15}")
    private double directMatchBoost;

    @Value("${srlm.rerank.cue-boost:0.08}")
    private double cueBoost;

    @Value("${srlm.rerank.min-content-chars:60}")
    private int minContentChars;

    public List<RetrievedChunk> rerank(String query, List<RetrievedChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) return List.of();

        String normQuery = InsuranceKeywords.normalise(query);
        Set<String> queryGroups = InsuranceKeywords.matchedGroups(query);
        boolean queryAsksExclusion = containsAny(normQuery, InsuranceKeywords.NEGATION_CUES)
                || normQuery.contains("exclusion") || normQuery.contains("excluded");

        for (RetrievedChunk c : chunks) {
            String content = c.getContent() == null ? "" : c.getContent();
            String norm = InsuranceKeywords.normalise(content);
            double boost = 0.0;

            // Direct clause match: every query group occurs literally
            for (String g : queryGroups) {
                String gNorm = InsuranceKeywords.normalise(g);
                if (norm.contains(gNorm)) boost += directMatchBoost / Math.max(1, queryGroups.size());
            }

            // Cue boost
            if (containsAny(norm, InsuranceKeywords.AFFIRMATIVE_CUES)) boost += cueBoost;
            if (queryAsksExclusion && containsAny(norm, InsuranceKeywords.NEGATION_CUES)) boost += cueBoost;

            // Penalise tiny fragments
            if (content.length() < minContentChars) boost -= 0.10;

            double rerankedScore = clamp01(c.getFinalScore() + boost);
            c.setFinalScore(rerankedScore);
        }

        chunks.sort(Comparator.comparingDouble(RetrievedChunk::getFinalScore).reversed());
        if (log.isDebugEnabled()) {
            log.debug("Reranked top-3 chunks: {}",
                    chunks.stream().limit(3).map(c -> c.getChunkId() + "=" + c.getFinalScore()).toList());
        }
        return chunks;
    }

    private boolean containsAny(String haystack, Set<String> needles) {
        String h = haystack.toLowerCase(Locale.ROOT);
        for (String n : needles) {
            if (h.contains(n)) return true;
        }
        return false;
    }

    private double clamp01(double v) { return Math.max(0.0, Math.min(1.0, v)); }
}
