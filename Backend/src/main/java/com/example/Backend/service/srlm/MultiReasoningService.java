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
            ReasoningPath.FINANCIAL_FOCUSED,
            ReasoningPath.RISK_AWARE,
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
                    "Focus on the prescribed drugs in the record: name them explicitly, state what each " +
                    "is for at the dosage given, and explain the COMMON, well-established side effects, " +
                    "key interactions, and red-flag symptoms the patient should watch for. Tie this to " +
                    "the patient's diagnosis. Do NOT recommend new drugs or change dosages.";
            case TREATMENT_NEXT_STEPS ->
                    "Focus on what the patient should do next based on the recorded plan: " +
                    "follow-up timing, lifestyle, when to seek help. Do NOT invent steps not in the record.";
            default -> "Restate the plan accurately from the record.";
        };

        return String.format("""
                [SYSTEM: PATIENT TREATMENT EXPLAINER]
                You are explaining a patient's OWN clinical record back to them, and answering
                questions about the specific drugs and treatments listed there.

                RULES:
                1. The DRUGS, DOSAGES, DIAGNOSIS, and TREATMENT PLAN must come ONLY from the PATIENT
                   CONTEXT below. Do not invent prescriptions, dosages, dates, or diagnoses that are
                   not in the context.
                2. The PATIENT CONTEXT may contain a [VERIFIED DRUG KNOWLEDGE] block. When it does,
                   that block is the AUTHORITATIVE source for any pharmacological claim — uses,
                   common side effects, serious warnings, contraindications, interactions.
                   You MAY ONLY state pharmacological facts that appear in that block.
                   Do NOT supplement it with facts from general knowledge — even if the fact is
                   "commonly known". If a side effect, interaction, or use is not in the verified
                   block, you do NOT mention it.
                3. For drugs that ARE listed in the context, you MAY and SHOULD explain the verified
                   facts above (uses, common side effects, etc.) so the patient understands their
                   prescription. Do NOT refuse on the grounds of "not being a doctor"; do NOT tell
                   the patient to consult a pharmacist as the sole answer when verified facts exist.
                4. You must NOT: recommend new drugs, change dosages, diagnose new conditions, or
                   override the doctor's plan. If the patient asks whether to stop, skip, increase,
                   or change a medication, REDIRECT them to their doctor or pharmacist without
                   giving a directive answer of your own.
                5. If the patient context has NO records or NO drugs at all, say so plainly: tell
                   them no prescriptions are on file yet and to ask their doctor to add the record.
                6. Speak directly to the patient ("you", "your") in a calm, clear, helpful tone.
                7. No insurance/policy/coverage talk unless the patient asked about it.

                [FOCUS]: %s

                [PATIENT CONTEXT]:
                %s

                [USER QUESTION]: %s
                [INTENT]: %s

                Output format:
                APPROACH: 1 sentence describing your angle.
                REASONING: Cite the exact diagnosis / drug / dosage text from context that you used.
                ANSWER: 3-7 sentences in plain English, addressed to the patient. If the question is
                about side effects / interactions / what a drug does, name the drug(s) from the record
                and explain the well-known facts directly. Never reply with a flat refusal.
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
            case FINANCIAL_FOCUSED ->
                    "Focus on monetary limits, caps, percentages, sum insured, and payable " +
                    "amounts. If the user mentions an amount and the policy states a cap, " +
                    "compute: covered = min(actual, cap); payable = actual − covered. Show " +
                    "the arithmetic explicitly. Do NOT invent caps that are not in the evidence.";
            case RISK_AWARE ->
                    "Focus on what could reduce or block coverage: explicit exclusions, " +
                    "waiting periods, conditions, sub-limits, and process requirements. " +
                    "An exclusion is ONLY valid if the evidence states it. Silence in the " +
                    "evidence is NOT an exclusion.";
            default ->
                    "Provide a clear, accurate answer grounded strictly in the provided context.";
        };

        return String.format("""
                [SYSTEM: EVIDENCE-GROUNDED INSURANCE ANALYST | %s]
                You answer ONLY from the [EVIDENCE CLAUSES] block below. Treat every clause as
                authoritative and treat anything not in the evidence as unknown.

                NON-NEGOTIABLE RULES (these override the focus instruction below):
                1. EVIDENCE-ONLY: Every factual statement in your ANSWER must be directly
                   supported by one or more clauses in [EVIDENCE CLAUSES]. If a fact is not
                   in the evidence, you must NOT state it.
                2. NEVER FABRICATE EXCLUSIONS: Do not say a service is "not covered",
                   "excluded", "not payable", or "denied" unless that exact stance is stated
                   in the evidence. Silence in the evidence is NOT an exclusion.
                3. AFFIRMATIVE CLAUSES ARE BINDING: If a clause says a service is covered
                   (e.g. "ICU Charges: Covered up to sum insured"), your answer MUST treat
                   it as covered. You cannot override an affirmative clause with reasoning
                   about other sections (daycare, OPD, etc.).
                4. CITE EVERY CLAIM: Every claim in your ANSWER must end with a citation tag
                   of the form [CITE: <chunkId> | "<short quote>"] using the chunk IDs given
                   in [EVIDENCE CLAUSES]. If you cannot cite, you cannot claim.
                5. SECTION DISCIPLINE: When the evidence has multiple sections (Pharmacy,
                   Surgery, ICU, OPD…), reason ONLY about the section(s) that match the
                   user question. Do not import logic from an unrelated section.
                6. NO INVENTED LIMITS: Do not invent tier numbers, co-pay percentages, or
                   waiting periods that are not present in the evidence.
                7. INSUFFICIENT EVIDENCE: If the evidence does not contain enough information
                   to answer, your ANSWER must be exactly: "The uploaded policy does not
                   contain enough information to answer this question."

                [FOCUS]: %s

                [EVIDENCE CLAUSES]:
                %s

                [USER QUERY]: %s
                [QUERY INTENT]: %s

                Required output format (these labels are parsed verbatim):
                APPROACH: 1 sentence describing your angle.
                REASONING: Walk through the exact clauses you used, by chunkId. Quote them.
                  If a clause uses affirmative coverage language for the asked topic, you
                  MUST conclude coverage; never silently flip it.
                ANSWER: 2-5 sentences. Each sentence must end with a [CITE: ...] tag pointing
                  to the supporting chunkId, or the sentence must be the insufficient-evidence
                  sentence verbatim from rule 7.
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