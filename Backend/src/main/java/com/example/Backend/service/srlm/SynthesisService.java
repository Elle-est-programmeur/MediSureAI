package com.example.Backend.service.srlm;

import com.example.Backend.dto.ReasoningCandidate;
import com.example.Backend.dto.ScoringResult;
import com.example.Backend.model.QueryIntent;
import com.example.Backend.service.llm.CritiqueLLMService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Merges the top-scoring reasoning candidates into a single, coherent answer.
 * When synthesis is disabled it simply returns the best-scoring candidate's answer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SynthesisService {

    private final CritiqueLLMService llmService;

    @Value("${srlm.synthesis.enabled:true}")
    private boolean synthesisEnabled;

    @Value("${srlm.scoring.min-confidence:6.0}")
    private double minConfidence;

    public record SynthesisOutput(String answer, String reasoning) {}

    public SynthesisOutput synthesize(
            List<ReasoningCandidate> candidates,
            List<ScoringResult> scores,
            String query,
            QueryIntent intent) {

        // Sort scores descending
        List<ScoringResult> sorted = scores.stream()
                .sorted(Comparator.comparingDouble(ScoringResult::getOverallScore).reversed())
                .toList();

        if (sorted.isEmpty()) {
            return new SynthesisOutput(
                    "Unable to generate an answer from the available documents.", "No candidates");
        }

        ScoringResult best = sorted.get(0);
        Map<Object, ReasoningCandidate> candidateMap = candidates.stream()
                .collect(Collectors.toMap(c -> c.getPath(), Function.identity()));

        ReasoningCandidate bestCandidate = candidateMap.get(best.getPath());

        if (!synthesisEnabled || best.getOverallScore() < minConfidence) {
            log.info("Synthesis skipped — using best candidate: {} (score={})",
                    best.getPath(), best.getOverallScore());
            String reasoning = String.format(
                    "Selected %s path with score %.1f (synthesis disabled or below threshold)",
                    best.getPath(), best.getOverallScore());
            return new SynthesisOutput(bestCandidate.getAnswer(), reasoning);
        }

        // Take top 2 for synthesis
        List<ScoringResult> topTwo = sorted.stream().limit(2).toList();
        List<ReasoningCandidate> topCandidates = topTwo.stream()
                .map(s -> candidateMap.get(s.getPath()))
                .filter(c -> c != null)
                .toList();

        if (topCandidates.size() < 2) {
            return new SynthesisOutput(bestCandidate.getAnswer(),
                    "Only one valid candidate — no synthesis needed");
        }

        try {
            String prompt = buildSynthesisPrompt(query, intent, topCandidates, topTwo);
            String synthesized = llmService.generateCritique(prompt);
            String reasoning = String.format(
                    "Synthesized from %s (%.1f) and %s (%.1f)",
                    topTwo.get(0).getPath(), topTwo.get(0).getOverallScore(),
                    topTwo.get(1).getPath(), topTwo.get(1).getOverallScore());
            log.info("Synthesis complete for intent={}", intent);
            return new SynthesisOutput(synthesized, reasoning);
        } catch (Exception e) {
            log.warn("Synthesis LLM call failed, using best candidate: {}", e.getMessage());
            return new SynthesisOutput(bestCandidate.getAnswer(),
                    "Synthesis failed — fallback to best candidate");
        }
    }

    private String buildSynthesisPrompt(
            String query, QueryIntent intent,
            List<ReasoningCandidate> candidates, List<ScoringResult> scores) {

        StringBuilder sb = new StringBuilder();
        sb.append("You are a healthcare insurance expert. Synthesize the two best answers below ")
          .append("into one clear, comprehensive, and accurate final answer.\n\n")
          .append("USER QUESTION: ").append(query).append("\n")
          .append("INTENT: ").append(intent.name()).append("\n\n");

        for (int i = 0; i < candidates.size(); i++) {
            ReasoningCandidate c = candidates.get(i);
            ScoringResult s = scores.get(i);
            sb.append(String.format("CANDIDATE %d (path=%s, score=%.1f):\n%s\n\n",
                    i + 1, c.getPath().name(), s.getOverallScore(), c.getAnswer()));
        }

        sb.append("Synthesize these into ONE final answer. ")
          .append("Keep it concise (3-5 sentences), accurate, and actionable. ")
          .append("Do not mention reasoning paths or scores — speak directly to the patient.");

        return sb.toString();
    }
}