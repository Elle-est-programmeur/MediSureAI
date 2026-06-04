package com.example.Backend.service.agent;

import com.example.Backend.dto.*;
import com.example.Backend.exception.AgentException;
import com.example.Backend.model.AgentStep;
import com.example.Backend.model.QueryIntent;
import com.example.Backend.model.ToolType;
import com.example.Backend.service.confidence.ConfidenceCalibrationService;
import com.example.Backend.service.llm.LLMService;
import com.example.Backend.service.llm.PromptTemplateService;
import com.example.Backend.service.memory.SessionMemoryService;
import com.example.Backend.service.safety.GuardrailService;
import com.example.Backend.service.srlm.SRLMOrchestrator;
import com.example.Backend.service.tools.ToolOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Central agent brain — Phase 4 + Phase 6 production enhancements.
 *
 * Pipeline:
 *   IntentDetection → TaskPlanning → ToolOrchestration → ConfidenceCalibration
 *   → LLM Reasoning → SafetyGuardrails → SessionMemory → Response
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentOrchestrator {

    private final IntentDetectionService intentDetectionService;
    private final TaskPlanningService taskPlanningService;
    private final ToolOrchestrator toolOrchestrator;
    private final LLMService llmService;
    private final PromptTemplateService promptTemplateService;
    private final ConfidenceCalibrationService confidenceService;
    private final GuardrailService guardrailService;
    private final SessionMemoryService sessionMemory;
    private final SRLMOrchestrator srlmOrchestrator;
    private final com.example.Backend.service.medical.DrugKnowledgeService drugKnowledgeService;
    private final com.example.Backend.service.medical.MedicalResponseSafetyService medicalResponseSafetyService;
    private final com.example.Backend.service.tools.DrugKnowledgeTool drugKnowledgeTool;

    @Value("${openai.model.completion}")
    private String model;

    public QueryResponse processQuery(QueryRequest request) {
        long start = System.currentTimeMillis();
        List<Map<String, Object>> trace = new ArrayList<>();
        boolean traceEnabled = Boolean.TRUE.equals(request.getIncludeSteps());

        try {
            log.info("Agent processing: '{}'", request.getQuery());

            // ── Session ID ────────────────────────────────────────────────────
            String sessionId = request.getSessionId() != null
                    ? request.getSessionId()
                    : UUID.randomUUID().toString();

            // ── Step 1: Intent Detection ──────────────────────────────────────
            IntentDetectionResult intentResult =
                    intentDetectionService.detectIntent(request.getQuery());

            if (traceEnabled) trace.add(traceEntry(AgentStep.INTENT_DETECTION, intentResult));

            // ── Branch: high-stakes patient-data intents go through SRLM ──────
            if (isSrlmIntent(intentResult.getIntent())) {
                return runSrlmTreatmentPath(request, intentResult, sessionId, start);
            }

            // ── Step 2: Task Planning ─────────────────────────────────────────
            TaskPlan plan = taskPlanningService.createPlan(
                    request.getQuery(), intentResult.getIntent());

            if (traceEnabled) trace.add(traceEntry(AgentStep.TASK_PLANNING, plan));

            // ── Step 3: Tool Orchestration ────────────────────────────────────
            ToolContext toolContext = ToolContext.builder()
                    .query(request.getQuery())
                    .intent(intentResult.getIntent())
                    .documentId(request.getDocumentId())
                    .parameters(plan.getParameters())
                    .sessionId(sessionId)
                    .patientUserId(request.getPatientUserId())
                    .build();

            List<ToolType> toolsToExecute = determineTools(plan, intentResult.getIntent());
            List<ToolResult> toolResults = toolOrchestrator.executeTools(toolContext, toolsToExecute);
            String context = toolOrchestrator.combineToolResults(toolResults);


            if (traceEnabled) {

                int chunkCount = context.isBlank() ? 0 : context.split("---").length;
                trace.add(traceEntry(AgentStep.VECTOR_SEARCH,
                        Map.of("chunksRetrieved", chunkCount,
                               "contextLength", context.length(),
                               "toolsExecuted", toolResults.size())));
            }

            // ── Step 4: Confidence Calibration ────────────────────────────────
            ConfidenceCalibrationService.ConfidenceScore confidence =
                    confidenceService.calculateConfidence(
                            intentResult.getConfidence(), toolResults, context.length());

            // ── Step 5: LLM Reasoning ─────────────────────────────────────────
            String answer;
            if (context.isBlank()) {
                answer = "I don't have any relevant documents to answer this question. "
                       + "Please upload your insurance policy or medical documents first, "
                       + "then try again.";
                log.warn("No context retrieved — skipping LLM call");
            } else {
                String prompt = promptTemplateService.buildAnswerPrompt(
                        request.getQuery(), context, intentResult.getIntent());
                answer = llmService.generateCompletion(prompt);

                if (traceEnabled) trace.add(traceEntry(AgentStep.LLM_REASONING,
                        Map.of("promptLength", prompt.length(), "answerLength", answer.length())));
            }

            // ── Step 6: Safety Guardrails ─────────────────────────────────────
            GuardrailService.SafetyCheckResult safety =
                    guardrailService.checkAnswer(answer, confidence.score());

            // ── Step 7: Session Memory ────────────────────────────────────────
            sessionMemory.saveQueryContext(sessionId, request.getQuery(), safety.safeAnswer());

            long elapsed = System.currentTimeMillis() - start;
            log.info("Agent completed in {}ms [intent={}, confidence={}/10 ({}), blocked={}]",
                    elapsed, intentResult.getIntent(),
                    confidence.score(), confidence.level(), safety.blocked());

            return QueryResponse.builder()
                    .query(request.getQuery())
                    .answer(safety.safeAnswer())
                    .detectedIntent(intentResult.getIntent())
                    .intentConfidence(intentResult.getConfidence())
                    .overallConfidence(confidence.score())
                    .confidenceLevel(confidence.level())
                    .confidenceFactors(confidence.factors())
                    .safetyWarnings(safety.warnings().isEmpty() ? null : safety.warnings())
                    .citations(new ArrayList<>())
                    .executionSteps(traceEnabled ? trace : null)
                    .processingTimeMs(elapsed)
                    .model(model)
                    .build();

        } catch (Exception ex) {
            log.error("Agent processing failed: {}", ex.getMessage(), ex);
            throw new AgentException("Failed to process query: " + ex.getMessage(), ex);
        }
    }

    /**
     * Intent-driven tool selection. PATIENT_DATA is added when the query needs
     * personal clinical/billing context; VECTOR_SEARCH is added when the query
     * needs document grounding. Falls back to plan-derived tools for intents
     * with no explicit rule (GENERAL_INFO, UNKNOWN, CLAIM_STATUS, …).
     */
    /** Intents whose source of truth is the patient's own record — route via SRLM. */
    private boolean isSrlmIntent(QueryIntent intent) {
        return intent == QueryIntent.TREATMENT_EXPLANATION
                || intent == QueryIntent.MEDICAL_INTERPRETATION;
    }

    /**
     * SRLM path: pull patient data, run multi-path reasoning + reflection + synthesis,
     * then convert the SRLMResponse to a QueryResponse so the frontend shape stays stable.
     */
    private QueryResponse runSrlmTreatmentPath(
            QueryRequest request, IntentDetectionResult intentResult, String sessionId, long start) {

        QueryIntent intent = intentResult.getIntent();
        log.info("Routing intent={} through SRLM (treatment path)", intent);

        // Pull patient data only — no vector search noise
        ToolContext toolContext = ToolContext.builder()
                .query(request.getQuery())
                .intent(intent)
                .sessionId(sessionId)
                .patientUserId(request.getPatientUserId())
                .build();

        List<ToolResult> toolResults =
                toolOrchestrator.executeTools(toolContext, List.of(ToolType.PATIENT_DATA));
        String patientContext = toolOrchestrator.combineToolResults(toolResults);

        // ── Inject verified drug knowledge ──────────────────────────────────
        // Drugs are matched from both the patient's prescription (which lives
        // inside patientContext) and the user's query. The verified block is
        // appended so the SRLM prompt has authoritative pharmacological data.
        java.util.LinkedHashSet<com.example.Backend.dto.DrugInfo> drugs =
                new java.util.LinkedHashSet<>();
        drugs.addAll(drugKnowledgeService.extractMentionedDrugs(patientContext));
        drugs.addAll(drugKnowledgeService.extractMentionedDrugs(request.getQuery()));
        String enrichedContext = patientContext;
        if (!drugs.isEmpty()) {
            String knowledgeBlock = drugKnowledgeTool.formatBlock(new java.util.ArrayList<>(drugs));
            enrichedContext = patientContext + "\n\n" + knowledgeBlock;
            log.info("AgentOrchestrator: injected verified knowledge for {} drug(s) into treatment context", drugs.size());
        }

        // SRLM call (parallel reasoning + reflection + synthesis)
        com.example.Backend.dto.SRLMResponse srlm =
                srlmOrchestrator.processTreatmentQuery(request, enrichedContext, intent);

        // ── Medical safety pass ─────────────────────────────────────────────
        String safeAnswer = srlm.getFinalAnswer();
        java.util.List<String> safetyNotes = new java.util.ArrayList<>();
        if (safeAnswer != null) {
            // Step A: redirect prescribing-directive questions to a doctor
            var directive = medicalResponseSafetyService.handlePrescribingDirective(request.getQuery(), safeAnswer);
            safeAnswer = directive.safeAnswer();
            if (directive.redirected()) safetyNotes.add("Prescribing-directive redirect applied");

            // Step B: validate against the verified drug DB and sanitize unsupported claims
            var validation = medicalResponseSafetyService.validate(safeAnswer, new java.util.ArrayList<>(drugs));
            safeAnswer = validation.safeAnswer();
            if (validation.sanitized()) {
                safetyNotes.add("Unsupported pharmacological claims removed: "
                        + validation.issues().stream()
                                .map(com.example.Backend.service.medical.MedicalResponseSafetyService.SafetyIssue::getClaim)
                                .toList());
            }
        }

        long elapsed = System.currentTimeMillis() - start;

        // Map score (0-10) → confidence level for the UI
        double score = srlm.getConfidenceScore();
        String level = score >= 8.0 ? "HIGH" : score >= 5.0 ? "MEDIUM" : "LOW";

        java.util.List<String> factors = new java.util.ArrayList<>();
        factors.add("SRLM multi-path reasoning");
        factors.add("selected: " + (srlm.getSelectedPath() != null ? srlm.getSelectedPath().name() : "n/a"));
        if (srlm.getSynthesisReasoning() != null) factors.add(srlm.getSynthesisReasoning());
        if (!drugs.isEmpty()) {
            factors.add("verified drugs: " + drugs.stream()
                    .map(com.example.Backend.dto.DrugInfo::getDrugName).toList());
        }
        factors.addAll(safetyNotes);

        return QueryResponse.builder()
                .query(request.getQuery())
                .answer(safeAnswer)
                .detectedIntent(intent)
                .intentConfidence(intentResult.getConfidence())
                .overallConfidence(score)
                .confidenceLevel(level)
                .confidenceFactors(factors)
                .citations(new ArrayList<>())
                .processingTimeMs(elapsed)
                .model(model)
                .build();
    }

    private List<ToolType> determineTools(TaskPlan plan, QueryIntent intent) {
        List<ToolType> tools = new ArrayList<>();
        switch (intent) {
            case BILLING_INQUIRY, COVERAGE_QUESTION -> {
                tools.add(ToolType.PATIENT_DATA);
                tools.add(ToolType.VECTOR_SEARCH);
            }
            case MEDICAL_INTERPRETATION -> tools.add(ToolType.PATIENT_DATA);
            case POLICY_DETAILS -> tools.add(ToolType.VECTOR_SEARCH);
            default -> {
                if (plan.getSteps().contains(AgentStep.VECTOR_SEARCH)) {
                    tools.add(ToolType.VECTOR_SEARCH);
                }
                if (plan.getSteps().contains(AgentStep.SQL_QUERY)) {
                    tools.add(ToolType.METADATA_QUERY);
                }
            }
        }
        return tools;
    }

    private Map<String, Object> traceEntry(AgentStep step, Object result) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("step", step.name());
        entry.put("result", result);
        entry.put("timestamp", System.currentTimeMillis());
        return entry;
    }
}