package com.example.Backend.service.safety;

import com.example.Backend.dto.RetrievedChunk;
import com.example.Backend.service.srlm.InsuranceKeywords;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Final safety net before an answer is returned to the user.
 *
 * Detects:
 *  - uncertain / hedging language ("I think", "probably")
 *  - fabricated exclusions: the answer claims a service is not covered, but
 *    no retrieved clause supports that claim
 *  - extremely short / empty answers
 *  - low-confidence answers (replaced with a safe fallback)
 *
 * Returns a {@link SafetyCheckResult} that callers act on: either return the
 * safe answer or trigger a retrieval retry.
 */
@Service
@Slf4j
public class GuardrailService {

    private static final List<String> HEDGING_INDICATORS = List.of(
            "i believe", "i think", "probably", "might be", "could be", "as far as i know"
    );

    private static final String LOW_CONFIDENCE_FALLBACK =
            "I don't have enough information in your documents to answer this confidently. " +
            "Please upload more relevant documents or rephrase your question.";

    private static final String INSUFFICIENT_EVIDENCE_FALLBACK =
            "The uploaded policy does not contain enough information to answer this question.";

    @Value("${safety.min-confidence:4.0}")
    private double minConfidence;

    public SafetyCheckResult checkAnswer(String answer, Double confidence) {
        return check(answer, confidence, false, null);
    }

    /**
     * Lenient mode for TREATMENT_EXPLANATION / MEDICAL_INTERPRETATION where the
     * source-of-truth is the patient's own record.
     */
    public SafetyCheckResult checkAnswerLenient(String answer, Double confidence) {
        return check(answer, confidence, true, null);
    }

    /**
     * Evidence-aware check: cross-references the answer against the retrieved
     * chunks to catch fabricated exclusions. Use for policy/coverage flows.
     */
    public SafetyCheckResult checkAnswerWithEvidence(
            String answer, Double confidence, List<RetrievedChunk> evidence) {
        return check(answer, confidence, false, evidence);
    }

    private SafetyCheckResult check(
            String answer, Double confidence, boolean lenient, List<RetrievedChunk> evidence) {

        List<String> warnings = new ArrayList<>();
        boolean shouldBlock = false;
        boolean retrieveRetry = false;

        String norm = answer == null ? "" : answer.toLowerCase(Locale.ROOT);

        boolean hedging = HEDGING_INDICATORS.stream().anyMatch(norm::contains);
        if (hedging) warnings.add("Hedging / uncertain language detected");

        if (confidence != null && confidence < minConfidence) {
            warnings.add(String.format("Low confidence score: %.1f/10", confidence));
            if (!lenient) {
                shouldBlock = true;
                retrieveRetry = true;
            }
        }

        if (answer == null || answer.length() < 50) {
            warnings.add("Answer too short, may be incomplete");
            shouldBlock = true;
        }

        // Fabricated-exclusion detection against retrieved evidence
        boolean fabricatedExclusion = false;
        if (evidence != null && !evidence.isEmpty() && answer != null) {
            String normAns = InsuranceKeywords.normalise(answer);
            boolean answerHasNegation = containsAny(normAns, InsuranceKeywords.NEGATION_CUES);
            if (answerHasNegation) {
                boolean evidenceHasNegation = evidence.stream().anyMatch(c ->
                        containsAny(InsuranceKeywords.normalise(c.getContent()),
                                InsuranceKeywords.NEGATION_CUES));
                if (!evidenceHasNegation) {
                    fabricatedExclusion = true;
                    warnings.add("Fabricated exclusion: answer uses 'not covered / excluded' but no retrieved clause does");
                    shouldBlock = true;
                    retrieveRetry = true;
                }
            }
        }

        if (answer != null
                && !norm.contains("according to")
                && !norm.contains("based on")
                && !norm.contains("[cite")
                && !norm.contains("(source")) {
            warnings.add("No explicit source citations");
        }

        if (!warnings.isEmpty()) {
            log.warn("Safety warnings (lenient={}, fabricated={}): {}", lenient, fabricatedExclusion, warnings);
        }

        String safeAnswer = answer;
        if (shouldBlock) {
            safeAnswer = fabricatedExclusion ? INSUFFICIENT_EVIDENCE_FALLBACK : LOW_CONFIDENCE_FALLBACK;
        }

        return new SafetyCheckResult(safeAnswer, warnings, shouldBlock, retrieveRetry, fabricatedExclusion);
    }

    private boolean containsAny(String haystack, Set<String> needles) {
        for (String n : needles) {
            if (haystack.contains(n)) return true;
        }
        return false;
    }

    public record SafetyCheckResult(
            String safeAnswer,
            List<String> warnings,
            boolean blocked,
            boolean shouldRetry,
            boolean fabricatedExclusion) {

        public boolean isBlocked() { return blocked; }
    }
}
