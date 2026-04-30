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

    private static final ReasoningPath[] TREATMENT_PATH_ORDER = {
            ReasoningPath.TREATMENT_PLAIN,
            ReasoningPath.TREATMENT_DRUGS,
            ReasoningPath.TREATMENT_NEXT_STEPS
    };

    /**
     * Patient-data-focused reasoning paths for TREATMENT_EXPLANATION /
     * MEDICAL_INTERPRETATION queries. Same parallel infrastructure, different prompts.
     */
    public List<ReasoningCandidate> generateTreatmentReasoningPaths(
            String query, String patientContext, QueryIntent intent) {

        int pathCount = Math.min(reasoningPaths, TREATMENT_PATH_ORDER.length);

        List<CompletableFuture<ReasoningCandidate>> futures = new ArrayList<>();
        for (int i = 0; i < pathCount; i++) {
            final ReasoningPath path = TREATMENT_PATH_ORDER[i];
            CompletableFuture<ReasoningCandidate> future = CompletableFuture.supplyAsync(() -> {
                try {
                    String prompt = buildTreatmentPathPrompt(query, patientContext, intent, path);
                    String response = llmService.generateCompletionWithTemperature(prompt, reasoningTemperature);
                    log.debug("Generated treatment reasoning path: {}", path);
                    return parseCandidate(path, response);
                } catch (Exception e) {
                    log.warn("Failed to generate treatment path {}: {}", path, e.getMessage());
                    return null;
                }
            });
            futures.add(future);
        }

        List<ReasoningCandidate> candidates = futures.stream()
                .map(CompletableFuture::join)
                .filter(c -> c != null)
                .collect(java.util.stream.Collectors.toList());

        log.info("Generated {}/{} treatment paths (parallel)", candidates.size(), pathCount);
        return candidates;
    }

    private String buildTreatmentPathPrompt(String query, String patientContext, QueryIntent intent, ReasoningPath path) {
        String focusInstruction = switch (path) {
            case TREATMENT_PLAIN ->
                    "Restate the patient's diagnosis and treatment plan in plain, layperson English. " +
                    "Avoid jargon. Be concise and reassuring.";
            case TREATMENT_DRUGS ->
                    "Focus on the prescribed drugs: what each one is for, the dosage given, " +
                    "and how they relate to the diagnosis. Do NOT recommend new drugs or change dosages.";
            case TREATMENT_NEXT_STEPS ->
                    "Focus on what the patient should do next based on the recorded plan: " +
                    "follow-up timing, lifestyle, when to seek help. Do NOT invent steps not in the record.";
            default -> "Restate the plan accurately from the record.";
        };

        return String.format("""
                [SYSTEM: PATIENT TREATMENT EXPLAINER]
                You are explaining a patient's OWN clinical record back to them.

                STRICT RULES:
                1. Use ONLY the data in the PATIENT CONTEXT below. If a fact (drug, dose, follow-up date,
                   diagnosis) is not in the context, do NOT mention it. Never invent specifics.
                2. If the patient context has no records, say so plainly and stop.
                3. Do not give new medical advice. Only restate / explain what is recorded.
                4. Speak directly to the patient ("you", "your") in a calm, clear tone.
                5. No insurance/policy/coverage talk unless the patient asked about it.

                [FOCUS]: %s

                [PATIENT CONTEXT]:
                %s

                [USER QUESTION]: %s
                [INTENT]: %s

                Output format:
                APPROACH: 1 sentence describing your angle.
                REASONING: Cite the exact diagnosis / drug / treatment text from context that you used.
                ANSWER: 3-6 sentences in plain English, addressed to the patient.
                """,
                focusInstruction, patientContext, query, intent.name());
    }

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
                [SYSTEM: PATIENT CASE ANALYZER | %s]
                You are a Senior Healthcare Reasoning Model. Your goal is to analyze the gap between medical records and insurance terms.
                
                CRITICAL INSTRUCTIONS:
                1. Differentiate between SECTIONS (e.g., Section 4 Pharmacy vs Section 7 Surgery). 
                2. If the query is about SURGERY, do NOT use "Tier 3" or co-pay logic from the PHARMACY section.
                3. QUANTITATIVE COMPARISON: If a policy requires 'X weeks' of therapy, and medical records show 'Y weeks', you MUST explicitly calculate if Y is greater than or equal to X.
                4. FORMULARY VS SURGERY: Do not confuse drug names with procedure names.
                5. NO HALLUCinations: Only use information from the provided context.
                
                [FOCUS: %s]
                
                [CONTEXT]:
                %s
                
                [USER QUERY]: %s
                [QUERY INTENT]: %s
                
                Desired Output Format:
                APPROACH: 1 sentence.
                REASONING: Explain the exact numbers found (e.g., "Policy requires 6 wks; Records show 8 wks"). 
                ANSWER: State "Requirement Met" or "Coverage Gap Found" followed by a concise explanation.
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