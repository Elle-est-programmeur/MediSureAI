package com.example.Backend.service.srlm;

import com.example.Backend.dto.ReasoningCandidate;
import com.example.Backend.model.QueryIntent;
import com.example.Backend.model.ReasoningPath;
import com.example.Backend.service.llm.LLMService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Generates N independent reasoning paths over the same context.
 * Higher temperature (0.7) produces diverse answers that the reflection
 * and scoring stages then evaluate.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MultiReasoningService {

    private final LLMService llmService;

    @Value("${srlm.reasoning.paths:3}")
    private int reasoningPaths;

    @Value("${srlm.reasoning.temperature:0.7}")
    private double reasoningTemperature;

    private static final ReasoningPath[] PATH_ORDER = {
            ReasoningPath.POLICY_FOCUSED,
            ReasoningPath.COVERAGE_FOCUSED,
            ReasoningPath.BALANCED,
            ReasoningPath.CLAIM_ORIENTED,
            ReasoningPath.PATIENT_BENEFIT
    };

    public List<ReasoningCandidate> generateReasoningPaths(
            String query, String context, QueryIntent intent) {

        int pathCount = Math.min(reasoningPaths, PATH_ORDER.length);

        // Run all reasoning paths in PARALLEL to cut total time from N*latency → 1*latency
        List<CompletableFuture<ReasoningCandidate>> futures = new ArrayList<>();
        for (int i = 0; i < pathCount; i++) {
            final ReasoningPath path = PATH_ORDER[i];
            CompletableFuture<ReasoningCandidate> future = CompletableFuture.supplyAsync(() -> {
                try {
                    String prompt = buildPathPrompt(query, context, intent, path);
                    String response = llmService.generateCompletionWithTemperature(prompt, reasoningTemperature);
                    log.debug("Generated reasoning path: {}", path);
                    return parseCandidate(path, response);
                } catch (Exception e) {
                    log.warn("Failed to generate reasoning path {}: {}", path, e.getMessage());
                    return null;
                }
            });
            futures.add(future);
        }

        // Wait for all paths to complete and collect non-null results
        List<ReasoningCandidate> candidates = futures.stream()
                .map(CompletableFuture::join)
                .filter(c -> c != null)
                .collect(java.util.stream.Collectors.toList());

        log.info("Generated {}/{} reasoning paths (parallel)", candidates.size(), pathCount);
        return candidates;
    }

    private String buildPathPrompt(String query, String context, QueryIntent intent, ReasoningPath path) {
        String focusInstruction = switch (path) {
            case POLICY_FOCUSED ->
                    "Focus strictly on what the insurance policy document states. " +
                    "Quote relevant policy clauses and terms directly.";
            case COVERAGE_FOCUSED ->
                    "Focus on what is and is not covered. " +
                    "Enumerate covered services, exclusions, and coverage limits explicitly.";
            case BALANCED ->
                    "Provide a balanced answer considering both policy terms and patient needs. " +
                    "Weigh benefits against limitations objectively.";
            case CLAIM_ORIENTED ->
                    "Focus on the claims process, required documentation, and approval criteria. " +
                    "Explain the steps a patient must take.";
            case PATIENT_BENEFIT ->
                    "Focus on practical patient benefit: what does this mean for the patient's " +
                    "out-of-pocket costs, access to care, and next steps?";
        };

        return String.format("""
                [SYSTEM: %s | %s]
                [CONTEXT: %s]
                [QUERY: %s]
                [INTENT: %s]
                Short Format: 
                APPROACH: 1 sentence.
                REASONING: 2 sentences.
                ANSWER: Concisely answer.
                """,
                path.name(), focusInstruction, context, query, intent.name());
    }

    private ReasoningCandidate parseCandidate(ReasoningPath path, String response) {
        String approach = extractSection(response, "APPROACH:");
        String reasoning = extractSection(response, "REASONING:");
        String answer = extractSection(response, "ANSWER:");

        if (answer.isBlank()) {
            answer = response.trim();
        }

        return ReasoningCandidate.builder()
                .path(path)
                .approach(approach)
                .reasoning(reasoning)
                .answer(answer)
                .build();
    }

    private String extractSection(String text, String label) {
        int start = text.indexOf(label);
        if (start == -1) return "";
        start += label.length();
        int end = findNextSectionStart(text, start);
        return text.substring(start, end).trim();
    }

    private int findNextSectionStart(String text, int from) {
        String[] labels = {"APPROACH:", "REASONING:", "ANSWER:"};
        int earliest = text.length();
        for (String label : labels) {
            int idx = text.indexOf(label, from);
            if (idx != -1 && idx < earliest) {
                earliest = idx;
            }
        }
        return earliest;
    }
}