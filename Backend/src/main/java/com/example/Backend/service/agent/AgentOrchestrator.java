package com.example.Backend.service.agent;

import com.example.Backend.dto.*;
import com.example.Backend.exception.AgentException;
import com.example.Backend.model.AgentStep;
import com.example.Backend.service.llm.LLMService;
import com.example.Backend.service.llm.PromptTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central agent brain for Phase 4.
 *
 * Pipeline:
 *   IntentDetection → TaskPlanning → ToolExecution → LLM Reasoning → Response
 *
 * Each step's result is captured in an execution trace when the caller
 * requests {@code includeSteps: true} (debug mode).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentOrchestrator {

    private final IntentDetectionService intentDetectionService;
    private final TaskPlanningService taskPlanningService;
    private final ToolExecutor toolExecutor;
    private final LLMService llmService;
    private final PromptTemplateService promptTemplateService;

    @Value("${openai.model.completion}")
    private String model;

    @Value("${agent.context.max-chunks:5}")
    private int maxChunks;

    public QueryResponse processQuery(QueryRequest request) {
        long start = System.currentTimeMillis();
        List<Map<String, Object>> trace = new ArrayList<>();
        boolean traceEnabled = Boolean.TRUE.equals(request.getIncludeSteps());

        try {
            log.info("Agent processing: '{}'", request.getQuery());

            // ── Step 1: Intent Detection ──────────────────────────────────────
            IntentDetectionResult intentResult =
                    intentDetectionService.detectIntent(request.getQuery());

            if (traceEnabled) trace.add(traceEntry(AgentStep.INTENT_DETECTION, intentResult));

            // ── Step 2: Task Planning ─────────────────────────────────────────
            TaskPlan plan = taskPlanningService.createPlan(request.getQuery(), intentResult.getIntent());

            if (traceEnabled) trace.add(traceEntry(AgentStep.TASK_PLANNING, plan));

            // ── Step 3: Tool Execution ────────────────────────────────────────
            String context = "";

            if (plan.getSteps().contains(AgentStep.VECTOR_SEARCH)) {
                context = toolExecutor.executeVectorSearch(
                        request.getQuery(),
                        plan.getParameters(),
                        request.getDocumentId()
                );

                int chunkCount = context.isBlank() ? 0 : context.split("---").length;
                if (traceEnabled) trace.add(traceEntry(AgentStep.VECTOR_SEARCH,
                        Map.of("chunksRetrieved", chunkCount, "contextLength", context.length())));
            }

            // ── Step 4: LLM Reasoning ─────────────────────────────────────────
            String answer;
            if (context.isBlank()) {
                answer = "I don't have any relevant documents to answer this question. "
                       + "Please upload your insurance policy or medical documents first, "
                       + "then try again.";
                log.warn("No context retrieved for query — skipping LLM call");
            } else {
                String prompt = promptTemplateService.buildAnswerPrompt(
                        request.getQuery(), context, intentResult.getIntent());

                answer = llmService.generateCompletion(prompt);

                if (traceEnabled) trace.add(traceEntry(AgentStep.LLM_REASONING,
                        Map.of("promptLength", prompt.length(), "answerLength", answer.length())));
            }

            long elapsed = System.currentTimeMillis() - start;
            log.info("Agent completed in {}ms [intent={}, confidence={}]",
                    elapsed, intentResult.getIntent(), intentResult.getConfidence());

            return QueryResponse.builder()
                    .query(request.getQuery())
                    .answer(answer)
                    .detectedIntent(intentResult.getIntent())
                    .intentConfidence(intentResult.getConfidence())
                    .citations(new ArrayList<>())  // Phase 5: extract chunk IDs from context
                    .executionSteps(traceEnabled ? trace : null)
                    .processingTimeMs(elapsed)
                    .model(model)
                    .build();

        } catch (Exception ex) {
            log.error("Agent processing failed: {}", ex.getMessage(), ex);
            throw new AgentException("Failed to process query: " + ex.getMessage(), ex);
        }
    }

    private Map<String, Object> traceEntry(AgentStep step, Object result) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("step", step.name());
        entry.put("result", result);
        entry.put("timestamp", System.currentTimeMillis());
        return entry;
    }
}