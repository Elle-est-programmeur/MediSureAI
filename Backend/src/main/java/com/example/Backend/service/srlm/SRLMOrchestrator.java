package com.example.Backend.service.srlm;

import com.example.Backend.dto.*;
import com.example.Backend.exception.AgentException;
import com.example.Backend.model.QueryIntent;
import com.example.Backend.service.agent.IntentDetectionService;
import com.example.Backend.service.agent.ToolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Phase 5 orchestrator.
 *
 * Pipeline:
 *   IntentDetection → VectorSearch → MultiReasoning → SelfReflection → Scoring → Synthesis → Response
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

    @Value("${openai.model.completion}")
    private String model;

    public SRLMResponse processQuery(QueryRequest request) {
        long start = System.currentTimeMillis();

        try {
            log.info("SRLM processing: '{}'", request.getQuery());

            // ── Step 1: Intent Detection ─────────────────────────────────────
            IntentDetectionResult intentResult =
                    intentDetectionService.detectIntent(request.getQuery());
            QueryIntent intent = intentResult.getIntent();
            log.debug("Intent: {} (confidence={})", intent, intentResult.getConfidence());

            // ── Step 2: Vector Search ────────────────────────────────────────
            String context = toolExecutor.executeVectorSearch(
                    request.getQuery(), Map.of(), request.getDocumentId());

            if (context.isBlank()) {
                return noContextResponse(request.getQuery(), intent, intentResult.getConfidence(),
                        System.currentTimeMillis() - start);
            }

            // ── Step 3: Multi-Path Reasoning ─────────────────────────────────
            List<ReasoningCandidate> candidates =
                    multiReasoningService.generateReasoningPaths(request.getQuery(), context, intent);

            if (candidates.isEmpty()) {
                throw new AgentException("All reasoning paths failed", null);
            }

            // ── Step 4: Self-Reflection ──────────────────────────────────────
            List<ReflectionResult> reflections =
                    selfReflectionService.reflectOnCandidates(candidates, request.getQuery(), context);

            // ── Step 5: Scoring ──────────────────────────────────────────────
            List<ScoringResult> scores =
                    scoringService.scoreCandidates(candidates, reflections, request.getQuery(), context);

            // ── Step 6: Synthesis ────────────────────────────────────────────
            SynthesisService.SynthesisOutput synthesis =
                    synthesisService.synthesize(candidates, scores, request.getQuery(), intent);

            // Best score for confidence reporting
            double bestScore = scores.stream()
                    .mapToDouble(ScoringResult::getOverallScore)
                    .max()
                    .orElse(0.0);

            ScoringResult bestScoringResult = scores.stream()
                    .max(Comparator.comparingDouble(ScoringResult::getOverallScore))
                    .orElse(null);

            long elapsed = System.currentTimeMillis() - start;
            log.info("SRLM completed in {}ms [intent={}, paths={}, bestScore={}]",
                    elapsed, intent, candidates.size(), bestScore);

            return SRLMResponse.builder()
                    .query(request.getQuery())
                    .finalAnswer(synthesis.answer())
                    .selectedPath(bestScoringResult != null ? bestScoringResult.getPath() : null)
                    .confidenceScore(bestScore)
                    .detectedIntent(intent)
                    .allCandidates(request.getIncludeSteps() != null && request.getIncludeSteps()
                            ? candidates : null)
                    .reflections(request.getIncludeSteps() != null && request.getIncludeSteps()
                            ? reflections : null)
                    .scores(request.getIncludeSteps() != null && request.getIncludeSteps()
                            ? scores : null)
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

    private SRLMResponse noContextResponse(
            String query, QueryIntent intent, double confidence, long elapsed) {
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