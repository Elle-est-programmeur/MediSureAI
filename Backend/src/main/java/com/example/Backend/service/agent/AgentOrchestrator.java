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