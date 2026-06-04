package com.example.Backend.service.srlm;

import com.example.Backend.dto.ContradictionReport;
import com.example.Backend.dto.ReasoningCandidate;
import com.example.Backend.dto.ReflectionResult;
import com.example.Backend.dto.ScoringResult;
import com.example.Backend.service.llm.CritiqueLLMService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Advanced LLM scoring with deterministic evidence-grounding and contradiction
 * penalties baked in.
 *
 * The LLM scores relevance / correctness / completeness on [1, 10]. We then:
 *  - compute an evidenceConsistencyScore from the reflection signals
 *    (evidenceGrounded, fabricatedExclusion, unsupportedAssumption)
 *  - apply a contradictionPenalty based on the rule-based ContradictionReport
 *  - combine into an overallScore, and outright reject candidates whose
 *    contradictions are severe (severity >= 0.8) so synthesis can't pick them.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScoringService {

    private final CritiqueLLMService llmService;
    private final ObjectMapper objectMapper;

    @Value("${srlm.scoring.contradiction-hard-reject:0.8}")
    private double hardRejectSeverity;

    public List<ScoringResult> scoreCandidates(
            List<ReasoningCandidate> candidates,
            List<ReflectionResult> reflections,
            String query,
            String context) {
        return scoreCandidates(candidates, reflections, Map.of(), query, context);
    }

    /**
     * Preferred overload — pass the contradiction reports keyed by path so the
     * deterministic penalty applies. Falls back to LLM-only mode if empty.
     */
    public List<ScoringResult> scoreCandidates(
            List<ReasoningCandidate> candidates,
            List<ReflectionResult> reflections,
            Map<Object, ContradictionReport> contradictionsByPath,
            String query,
            String context) {

        log.info("Scoring {} reasoning candidates (parallel) — contradictionReports={}",
                candidates.size(), contradictionsByPath == null ? 0 : contradictionsByPath.size());

        Map<Object, ReflectionResult> reflectionMap = reflections.stream()
                .collect(Collectors.toMap(ReflectionResult::getPath, Function.identity()));

        List<CompletableFuture<ScoringResult>> futures = candidates.stream()
                .map(candidate -> CompletableFuture.supplyAsync(() -> {
                    try {
                        ReflectionResult reflection = reflectionMap.get(candidate.getPath());
                        ContradictionReport contradiction = contradictionsByPath == null
                                ? null : contradictionsByPath.get(candidate.getPath());
                        String prompt = buildScoringPrompt(query, context, candidate, reflection);
                        String response = llmService.generateCritique(prompt);
                        ScoringResult llmScore = parseScoring(candidate, response);
                        return enrichWithEvidenceAndContradiction(llmScore, reflection, contradiction);
                    } catch (Exception e) {
                        log.warn("Scoring failed for path {}: {}", candidate.getPath(), e.getMessage());
                        return defaultScore(candidate);
                    }
                }))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    private ScoringResult enrichWithEvidenceAndContradiction(
            ScoringResult llmScore,
            ReflectionResult reflection,
            ContradictionReport contradiction) {

        // Evidence consistency: start from reflection signals
        double evidence = 7.0;
        if (reflection != null) {
            if (!reflection.isEvidenceGrounded()) evidence -= 3.0;
            if (reflection.isFabricatedExclusion()) evidence -= 3.5;
            if (reflection.isUnsupportedAssumption()) evidence -= 2.0;
            if (!reflection.isFactsCorrect()) evidence -= 1.5;
            int unsupported = reflection.getUnsupportedClaims() == null
                    ? 0 : reflection.getUnsupportedClaims().size();
            evidence -= Math.min(2.0, 0.5 * unsupported);
        }
        evidence = clamp(evidence, 0.0, 10.0);

        // Contradiction penalty
        double penalty = 0.0;
        boolean rejected = false;
        String rejectionReason = null;
        if (contradiction != null) {
            if (contradiction.isContradicted()) penalty += 4.0 * contradiction.getSeverity();
            if (contradiction.isFabricatedExclusion()) penalty += 2.0;
            if (contradiction.getSeverity() >= hardRejectSeverity) {
                rejected = true;
                rejectionReason = "Hard contradiction: " + String.join("; ", contradiction.getConflicts());
            }
        }
        penalty = clamp(penalty, 0.0, 10.0);

        // Overall: weighted blend, then apply penalty
        double base = (
                0.20 * llmScore.getRelevanceScore() +
                0.20 * llmScore.getCorrectnessScore() +
                0.15 * llmScore.getCompletenessScore() +
                0.45 * evidence
        );
        double overall = clamp(base - penalty, 0.0, 10.0);

        return ScoringResult.builder()
                .path(llmScore.getPath())
                .relevanceScore(llmScore.getRelevanceScore())
                .correctnessScore(llmScore.getCorrectnessScore())
                .completenessScore(llmScore.getCompletenessScore())
                .evidenceConsistencyScore(round(evidence))
                .contradictionPenalty(round(penalty))
                .overallScore(rejected ? 0.0 : round(overall))
                .scoringReasoning(llmScore.getScoringReasoning()
                        + (penalty > 0 ? String.format(" | penalty=%.1f", penalty) : "")
                        + (rejected ? " | REJECTED" : ""))
                .rejected(rejected)
                .rejectionReason(rejectionReason)
                .build();
    }

    private String buildScoringPrompt(
            String query, String context, ReasoningCandidate candidate, ReflectionResult reflection) {

        String reflectionText = reflection != null ? reflection.getOverallAssessment() : "None";

        return String.format("""
                You are an evidence-grounding auditor for a healthcare insurance AI.
                Score the candidate answer on 1.0 - 10.0 across THREE metrics, judging
                strictly against the evidence (other metrics are computed deterministically).

                QUESTION: %s
                EVIDENCE: %s
                ANSWER: %s
                REFLECTION SUMMARY: %s

                METRICS:
                1. Relevance:    Does the answer address the user's specific question?
                2. Correctness:  Are factual statements present in the evidence?
                3. Completeness: Does it cover the relevant evidence clauses?

                Return ONLY valid JSON:
                {
                  "relevance": 0.0,
                  "correctness": 0.0,
                  "completeness": 0.0,
                  "reasoning": "one sentence"
                }
                """, query, context, candidate.getAnswer(), reflectionText);
    }

    private ScoringResult parseScoring(ReasoningCandidate candidate, String response) {
        try {
            String json = stripCodeFence(response);
            JsonNode node = objectMapper.readTree(json);
            return ScoringResult.builder()
                    .path(candidate.getPath())
                    .relevanceScore(node.path("relevance").asDouble(5.0))
                    .correctnessScore(node.path("correctness").asDouble(5.0))
                    .completenessScore(node.path("completeness").asDouble(5.0))
                    .scoringReasoning(node.path("reasoning").asText("LLM scored quality"))
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse scoring JSON: {}", response);
            return defaultScore(candidate);
        }
    }

    private ScoringResult defaultScore(ReasoningCandidate candidate) {
        return ScoringResult.builder()
                .path(candidate.getPath())
                .relevanceScore(5.0)
                .correctnessScore(5.0)
                .completenessScore(5.0)
                .evidenceConsistencyScore(5.0)
                .contradictionPenalty(0.0)
                .overallScore(5.0)
                .scoringReasoning("Fallback score due to analysis error")
                .build();
    }

    private double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
    private double round(double v) { return Math.round(v * 10.0) / 10.0; }

    private String stripCodeFence(String text) {
        String stripped = text.trim();
        if (stripped.startsWith("```")) {
            stripped = stripped.replaceFirst("```[a-zA-Z]*\\n?", "");
            int end = stripped.lastIndexOf("```");
            if (end != -1) stripped = stripped.substring(0, end);
        }
        return stripped.trim();
    }
}
