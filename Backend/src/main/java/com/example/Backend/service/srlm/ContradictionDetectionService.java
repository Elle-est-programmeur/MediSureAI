package com.example.Backend.service.srlm;

import com.example.Backend.dto.ContradictionReport;
import com.example.Backend.dto.ReasoningCandidate;
import com.example.Backend.dto.RetrievedChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Deterministic contradiction detector that compares an answer against the
 * retrieved evidence. Designed specifically to catch the canonical failure
 * mode where:
 *
 *   Evidence says: "ICU Charges: Covered up to sum insured"
 *   Answer says:    "ICU treatment is not covered"
 *
 * For every insurance topic the query is about (ICU, Maternity, OPD, …):
 *
 *  1. We classify what the EVIDENCE says about that topic (affirmative /
 *     negative / silent).
 *  2. We classify what the ANSWER says about that topic.
 *  3. If the two disagree we emit a structured conflict.
 *  4. If the answer claims an exclusion that no chunk supports, we flag a
 *     fabricated exclusion — that is the single most damaging hallucination
 *     class in healthcare insurance UX.
 *
 * Severity is in [0, 1] and is consumed by ScoringService as a penalty.
 */
@Service
@Slf4j
public class ContradictionDetectionService {

    public ContradictionReport detect(
            ReasoningCandidate candidate,
            String query,
            List<RetrievedChunk> evidence) {

        String answer = candidate == null ? "" : safe(candidate.getAnswer());
        String reasoning = candidate == null ? "" : safe(candidate.getReasoning());
        String haystack = InsuranceKeywords.normalise(answer + " " + reasoning);

        Set<String> queryTopics = InsuranceKeywords.matchedGroups(query);
        Set<String> answerTopics = InsuranceKeywords.matchedGroups(answer);
        // Topics worth checking: union of the query topics and topics the answer brings up
        java.util.LinkedHashSet<String> topics = new java.util.LinkedHashSet<>();
        topics.addAll(queryTopics);
        topics.addAll(answerTopics);

        List<String> conflicts = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        boolean fabricatedExclusion = false;
        double severity = 0.0;

        for (String topic : topics) {
            Stance evidenceStance = stanceFromEvidence(topic, evidence);
            Stance answerStance = stanceAboutTopic(topic, haystack);

            if (answerStance == Stance.NEGATIVE && evidenceStance == Stance.AFFIRMATIVE) {
                conflicts.add(String.format(
                        "Answer says %s is NOT covered, but evidence states it IS covered.", topic));
                severity = Math.max(severity, 1.0);
            } else if (answerStance == Stance.AFFIRMATIVE && evidenceStance == Stance.NEGATIVE) {
                conflicts.add(String.format(
                        "Answer says %s IS covered, but evidence states it is NOT.", topic));
                severity = Math.max(severity, 1.0);
            } else if (answerStance == Stance.NEGATIVE && evidenceStance == Stance.SILENT) {
                fabricatedExclusion = true;
                conflicts.add(String.format(
                        "Answer asserts %s is excluded, but no retrieved clause supports that exclusion.", topic));
                severity = Math.max(severity, 0.8);
            } else if (answerStance == Stance.AFFIRMATIVE && evidenceStance == Stance.SILENT) {
                warnings.add(String.format(
                        "Answer asserts %s is covered, but evidence is silent — claim is unsupported.", topic));
                severity = Math.max(severity, 0.4);
            }
        }

        // Unsupported-claim heuristic: the answer mentions exclusions without quoting evidence
        if (containsAny(haystack, InsuranceKeywords.NEGATION_CUES)) {
            boolean anyEvidenceNegation = evidence != null && evidence.stream().anyMatch(c ->
                    containsAny(InsuranceKeywords.normalise(c.getContent()), InsuranceKeywords.NEGATION_CUES));
            if (!anyEvidenceNegation) {
                fabricatedExclusion = true;
                if (conflicts.isEmpty()) {
                    conflicts.add("Answer uses exclusion language but no retrieved clause contains any negation.");
                }
                severity = Math.max(severity, 0.7);
            }
        }

        ContradictionReport report = ContradictionReport.builder()
                .path(candidate != null ? candidate.getPath() : null)
                .contradicted(!conflicts.isEmpty())
                .fabricatedExclusion(fabricatedExclusion)
                .conflicts(conflicts)
                .warnings(warnings)
                .severity(round(severity))
                .build();

        if (report.isContradicted() || report.isFabricatedExclusion()) {
            log.warn("Contradiction detected [path={}, severity={}]: {}",
                    report.getPath(), report.getSeverity(), conflicts);
        }
        return report;
    }

    private Stance stanceFromEvidence(String topic, List<RetrievedChunk> evidence) {
        if (evidence == null || evidence.isEmpty()) return Stance.SILENT;
        boolean affirmative = false;
        boolean negative = false;
        String topicNorm = InsuranceKeywords.normalise(topic);
        for (RetrievedChunk c : evidence) {
            String text = InsuranceKeywords.normalise(c.getContent());
            // Look only at sentences mentioning the topic
            for (String sentence : splitSentences(text)) {
                if (!sentence.contains(topicNorm)) continue;
                if (containsAny(sentence, InsuranceKeywords.NEGATION_CUES)) negative = true;
                else if (containsAny(sentence, InsuranceKeywords.AFFIRMATIVE_CUES)) affirmative = true;
            }
        }
        if (affirmative && !negative) return Stance.AFFIRMATIVE;
        if (negative && !affirmative) return Stance.NEGATIVE;
        if (affirmative) return Stance.AFFIRMATIVE; // mixed → prefer affirmative
        return Stance.SILENT;
    }

    private Stance stanceAboutTopic(String topic, String normAnswer) {
        String topicNorm = InsuranceKeywords.normalise(topic);
        Stance stance = Stance.SILENT;
        for (String sentence : splitSentences(normAnswer)) {
            if (!sentence.contains(topicNorm)) continue;
            if (containsAny(sentence, InsuranceKeywords.NEGATION_CUES)) return Stance.NEGATIVE;
            if (containsAny(sentence, InsuranceKeywords.AFFIRMATIVE_CUES)) stance = Stance.AFFIRMATIVE;
        }
        return stance;
    }

    private List<String> splitSentences(String text) {
        if (text == null || text.isEmpty()) return List.of();
        String[] parts = text.split("(?<=[.!?])\\s+|\\n+");
        return List.of(parts);
    }

    private boolean containsAny(String haystack, Set<String> needles) {
        for (String n : needles) {
            if (haystack.contains(n)) return true;
        }
        return false;
    }

    private String safe(String s) { return s == null ? "" : s; }
    private double round(double v) { return Math.round(v * 100.0) / 100.0; }

    private enum Stance { AFFIRMATIVE, NEGATIVE, SILENT }
}
