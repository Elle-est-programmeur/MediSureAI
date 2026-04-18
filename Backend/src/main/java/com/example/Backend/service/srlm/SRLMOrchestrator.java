package com.example.Backend.service.srlm;

import com.example.Backend.dto.*;
import com.example.Backend.exception.AgentException;
import com.example.Backend.model.QueryIntent;
import com.example.Backend.service.agent.IntentDetectionService;
import com.example.Backend.service.agent.ToolExecutor;
import com.example.Backend.service.confidence.ConfidenceCalibrationService;
import com.example.Backend.service.memory.SessionMemoryService;
import com.example.Backend.service.safety.GuardrailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Phase 5 + Phase 6 SRLM orchestrator.
 *
 * Pipeline:
 *   IntentDetection → VectorSearch → [FeedbackLoop] → MultiReasoning
 *   → SelfReflection → Scoring → Synthesis → Guardrails → SessionMemory → Response
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SRLMOrchestrator {

    private final IntentDetectionService intentDetectionService;
    private final ToolExecutor toolExecutor;
    private final MultiReasoningService multiReasoningService;
    private final SelfReflectionService selfReflectionService;
    private final ScoringService scoringService;
    private final SynthesisService synthesisService;
    private final ConfidenceCalibrationService confidenceService;
    private final GuardrailService guardrailService;
    private final SessionMemoryService sessionMemory;

    @Value("${openai.model.completion}")
    private String model;

    @Value("${srlm.feedback.enabled:true}")
    private boolean feedbackEnabled;

    @Value("${srlm.feedback.confidence-threshold:6.0}")
    private double feedbackThreshold;

    public SRLMResponse processQuery(QueryRequest request) {
        long start = System.currentTimeMillis();

        try {
            log.info("SRLM processing: '{}'", request.getQuery());

            // ── Session ID ────────────────────────────────────────────────────
            String sessionId = request.getSessionId() != null
                    ? request.getSessionId()
                    : UUID.randomUUID().toString();

            // ── Step 1: Intent Detection ──────────────────────────────────────
            IntentDetectionResult intentResult =
                    intentDetectionService.detectIntent(request.getQuery());
            QueryIntent intent = intentResult.getIntent();

            // ── Step 2: Vector Search ─────────────────────────────────────────
            String context = toolExecutor.executeVectorSearch(
                    request.getQuery(), Map.of("topK", 10), request.getDocumentId());

            if (context.trim().length() < 50) { // If very little context found
                return noContextResponse(request.getQuery(), intent,
                        System.currentTimeMillis() - start);
            }

            // ── Step 3: Confidence check + Retrieval Feedback Loop ────────────
            ConfidenceCalibrationService.ConfidenceScore initialConf =
                    confidenceService.calculateConfidence(
                            intentResult.getConfidence(), List.of(), context.length());

            context = applyRetrievalFeedback(request, context, initialConf.score());

            // ── Step 4: Multi-Path Reasoning ──────────────────────────────────
            List<ReasoningCandidate> candidates =
                    multiReasoningService.generateReasoningPaths(
                            request.getQuery(), context, intent);

            if (candidates.isEmpty()) {
                throw new AgentException("All reasoning paths failed", null);
            }

            // ── Step 5: Self-Reflection ───────────────────────────────────────
            List<ReflectionResult> reflections =
                    selfReflectionService.reflectOnCandidates(
                            candidates, request.getQuery(), context);

            // ── Step 6: Scoring ───────────────────────────────────────────────
            List<ScoringResult> scores =
                    scoringService.scoreCandidates(
                            candidates, reflections, request.getQuery(), context);

            // ── Step 7: Synthesis ─────────────────────────────────────────────
            SynthesisService.SynthesisOutput synthesis =
                    synthesisService.synthesize(candidates, scores, request.getQuery(), intent);

            // ── Step 8: Safety Guardrails ─────────────────────────────────────
            double bestScore = scores.stream()
                    .mapToDouble(ScoringResult::getOverallScore)
                    .max()
                    .orElse(0.0);

            GuardrailService.SafetyCheckResult safety =
                    guardrailService.checkAnswer(synthesis.answer(), bestScore);

            ScoringResult bestScoringResult = scores.stream()
                    .max(Comparator.comparingDouble(ScoringResult::getOverallScore))
                    .orElse(null);

            // ── Step 9: Session Memory ────────────────────────────────────────
            sessionMemory.saveQueryContext(sessionId, request.getQuery(), safety.safeAnswer());

            long elapsed = System.currentTimeMillis() - start;
            log.info("SRLM completed in {}ms [intent={}, paths={}, bestScore={}, blocked={}]",
                    elapsed, intent, candidates.size(), bestScore, safety.blocked());

            boolean includeTrace = Boolean.TRUE.equals(request.getIncludeSteps());

            return SRLMResponse.builder()
                    .query(request.getQuery())
                    .finalAnswer(safety.safeAnswer())
                    .selectedPath(bestScoringResult != null ? bestScoringResult.getPath() : null)
                    .confidenceScore(bestScore)
                    .detectedIntent(intent)
                    .allCandidates(includeTrace ? candidates : null)
                    .reflections(includeTrace ? reflections : null)
                    .scores(includeTrace ? scores : null)
                    .synthesisReasoning(synthesis.reasoning())
                    .processingTimeMs(elapsed)
                    .model(model)
                    .build();

        } catch (AgentException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("SRLM processing failed: {}", ex.getMessage(), ex);
            throw new AgentException("SRLM failed to process query: " + ex.getMessage(), ex);
        }
    }

    // ── Retrieval Feedback Loop ───────────────────────────────────────────────

    private String applyRetrievalFeedback(
            QueryRequest request, String initialContext, Double initialConfidence) {

        if (!feedbackEnabled) {
            return initialContext;
        }
        if (initialConfidence >= feedbackThreshold) {
            log.debug("Confidence {}/10 sufficient — feedback loop skipped", initialConfidence);
            return initialContext;
        }

        log.warn("Low confidence ({}/10) — applying retrieval feedback loop", initialConfidence);

        try {
            String refinedQuery = refineQuery(request.getQuery());
            String enhancedContext = toolExecutor.executeVectorSearch(
                    refinedQuery, Map.of("topK", 8), request.getDocumentId());

            if (enhancedContext.length() > initialContext.length()) {
                log.info("Feedback loop improved context: {} → {} chars",
                        initialContext.length(), enhancedContext.length());
                return enhancedContext;
            }
        } catch (Exception ex) {
            log.warn("Feedback loop failed: {}", ex.getMessage());
        }

        return initialContext;
    }

    private String refineQuery(String originalQuery) {
        return originalQuery + " policy coverage terms conditions";
    }

    // ── Fallback ──────────────────────────────────────────────────────────────

    private SRLMResponse noContextResponse(String query, QueryIntent intent, long elapsed) {
        return SRLMResponse.builder()
                .query(query)
                .finalAnswer("I don't have any relevant documents to answer this question. "
                        + "Please upload your insurance policy or medical documents first, "
                        + "then try again.")
                .detectedIntent(intent)
                .confidenceScore(0.0)
                .synthesisReasoning("No context retrieved from vector store")
                .processingTimeMs(elapsed)
                .model(model)
                .build();
    }
}