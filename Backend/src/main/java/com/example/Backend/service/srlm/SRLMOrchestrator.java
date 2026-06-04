package com.example.Backend.service.srlm;

import com.example.Backend.dto.*;
import com.example.Backend.exception.AgentException;
import com.example.Backend.model.QueryIntent;
import com.example.Backend.service.agent.IntentDetectionService;
import com.example.Backend.service.memory.SessionMemoryService;
import com.example.Backend.service.safety.GuardrailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Evidence-grounded SRLM orchestrator.
 *
 * Pipeline (with retry):
 *   IntentDetection
 *     ↓
 *   HybridRetrieval (cosine + keyword boost)
 *     ↓
 *   ContextValidation  ── invalid? → refine query, increase topK, retry once
 *     ↓
 *   RetrievalReranker
 *     ↓
 *   MultiReasoning (evidence-grounded, citation-required prompts)
 *     ↓
 *   Parallel: SelfReflection  +  ContradictionDetection (rule-based)
 *     ↓
 *   Scoring (evidence consistency + contradiction penalty + hard-reject)
 *     ↓
 *   Synthesis (only over non-rejected candidates)
 *     ↓
 *   CitationExtractor
 *     ↓
 *   Guardrail (fabricated-exclusion check) ── retry one more time if blocked
 *     ↓
 *   SRLMResponse with full observability for /debug
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SRLMOrchestrator {

    private final IntentDetectionService intentDetectionService;
    private final HybridRetrievalService hybridRetrievalService;
    private final ContextValidationService contextValidationService;
    private final RetrievalRerankerService retrievalRerankerService;
    private final PolicyClauseClassifier policyClauseClassifier;
    private final ContradictionDetectionService contradictionDetectionService;
    private final ConstraintReasoningService constraintReasoningService;
    private final FinancialCalculationValidator financialCalculationValidator;
    private final StructuredAnswerAssembler structuredAnswerAssembler;
    private final CitationExtractor citationExtractor;
    private final MultiReasoningService multiReasoningService;
    private final SelfReflectionService selfReflectionService;
    private final ScoringService scoringService;
    private final SynthesisService synthesisService;
    private final GuardrailService guardrailService;
    private final SessionMemoryService sessionMemory;

    @Value("${openai.model.completion}")
    private String model;

    @Value("${srlm.retrieval.default-topk:8}")
    private int defaultTopK;

    @Value("${srlm.retrieval.retry-topk:14}")
    private int retryTopK;

    @Value("${srlm.retrieval.max-attempts:2}")
    private int maxAttempts;

    public SRLMResponse processQuery(QueryRequest request) {
        long start = System.currentTimeMillis();
        try {
            String sessionId = request.getSessionId() != null
                    ? request.getSessionId()
                    : UUID.randomUUID().toString();

            log.info("SRLM[start] query='{}', sessionId={}", request.getQuery(), sessionId);

            IntentDetectionResult intentResult =
                    intentDetectionService.detectIntent(request.getQuery());
            QueryIntent intent = intentResult.getIntent();

            // ── Retrieval + validation loop ──────────────────────────────────
            RetrievalAttempt attempt = retrieveWithRetry(request);

            if (attempt.chunks().isEmpty()) {
                return noContextResponse(request.getQuery(), intent,
                        attempt, System.currentTimeMillis() - start);
            }

            // ── Classify policy clauses ──────────────────────────────────────
            List<ClauseClassification> classifications =
                    policyClauseClassifier.classifyAll(attempt.chunks());

            // ── Build evidence prompt with explicit chunk IDs + classifications
            String context = buildEvidenceContext(attempt.chunks(), classifications);

            // ── Reasoning + parallel reflection + contradiction detection ───
            List<ReasoningCandidate> candidates =
                    multiReasoningService.generateReasoningPaths(
                            request.getQuery(), context, intent);
            if (candidates.isEmpty()) {
                throw new AgentException("All reasoning paths failed", null);
            }

            List<ReflectionResult> reflections =
                    selfReflectionService.reflectOnCandidates(candidates, request.getQuery(), context);

            Map<Object, ContradictionReport> contradictionMap = new HashMap<>();
            List<ContradictionReport> contradictionReports = new ArrayList<>();
            for (ReasoningCandidate c : candidates) {
                ContradictionReport r =
                        contradictionDetectionService.detect(c, request.getQuery(), attempt.chunks());
                contradictionMap.put(c.getPath(), r);
                contradictionReports.add(r);
            }

            List<ScoringResult> scores = scoringService.scoreCandidates(
                    candidates, reflections, contradictionMap, request.getQuery(), context);

            List<String> rejectedPaths = scores.stream()
                    .filter(ScoringResult::isRejected)
                    .map(s -> s.getPath() + ": " + s.getRejectionReason())
                    .toList();

            List<ScoringResult> survivingScores = scores.stream()
                    .filter(s -> !s.isRejected())
                    .toList();

            if (survivingScores.isEmpty()) {
                log.warn("All {} candidates rejected by contradiction detection.", scores.size());
                return blockedResponse(request, intent, attempt, candidates, reflections,
                        scores, contradictionReports, rejectedPaths,
                        "All reasoning paths contradicted the retrieved evidence.",
                        System.currentTimeMillis() - start);
            }

            SynthesisService.SynthesisOutput synthesis =
                    synthesisService.synthesize(candidates, survivingScores, request.getQuery(), intent);

            double bestScore = survivingScores.stream()
                    .mapToDouble(ScoringResult::getOverallScore)
                    .max()
                    .orElse(0.0);

            // ── Guardrail with evidence-awareness ────────────────────────────
            GuardrailService.SafetyCheckResult safety =
                    guardrailService.checkAnswerWithEvidence(synthesis.answer(), bestScore, attempt.chunks());

            // One more retrieval attempt if guardrail says retry and we have budget
            if (safety.shouldRetry() && attempt.attemptNumber() < maxAttempts) {
                log.warn("Guardrail requested retrieval retry. Re-running with refined query.");
                QueryRequest refined = cloneWithRefinedQuery(request);
                RetrievalAttempt second = retrieveWithRetry(refined, retryTopK, attempt.attemptNumber() + 1);
                if (!second.chunks().isEmpty()) {
                    attempt = second;
                    context = buildEvidenceContext(attempt.chunks());
                    candidates = multiReasoningService.generateReasoningPaths(
                            refined.getQuery(), context, intent);
                    if (!candidates.isEmpty()) {
                        reflections = selfReflectionService.reflectOnCandidates(
                                candidates, refined.getQuery(), context);
                        contradictionMap = new HashMap<>();
                        contradictionReports = new ArrayList<>();
                        for (ReasoningCandidate c : candidates) {
                            ContradictionReport r = contradictionDetectionService.detect(
                                    c, refined.getQuery(), attempt.chunks());
                            contradictionMap.put(c.getPath(), r);
                            contradictionReports.add(r);
                        }
                        scores = scoringService.scoreCandidates(
                                candidates, reflections, contradictionMap, refined.getQuery(), context);
                        rejectedPaths = scores.stream()
                                .filter(ScoringResult::isRejected)
                                .map(s -> s.getPath() + ": " + s.getRejectionReason())
                                .toList();
                        survivingScores = scores.stream().filter(s -> !s.isRejected()).toList();
                        if (!survivingScores.isEmpty()) {
                            synthesis = synthesisService.synthesize(
                                    candidates, survivingScores, refined.getQuery(), intent);
                            bestScore = survivingScores.stream()
                                    .mapToDouble(ScoringResult::getOverallScore).max().orElse(0.0);
                            safety = guardrailService.checkAnswerWithEvidence(
                                    synthesis.answer(), bestScore, attempt.chunks());
                        }
                    }
                }
            }

            // ── Citations ────────────────────────────────────────────────────
            List<Citation> citations = citationExtractor.extract(safety.safeAnswer(), attempt.chunks());

            // ── Financial reasoning ──────────────────────────────────────────
            FinancialCalculation financial =
                    constraintReasoningService.reason(request.getQuery(), attempt.chunks());
            financialCalculationValidator.validate(financial);

            // ── Structured answer (Phase-15 production format) ───────────────
            StructuredAnswer structured = structuredAnswerAssembler.assemble(
                    safety.safeAnswer(),
                    attempt.chunks(),
                    classifications,
                    reflections,
                    scores,
                    contradictionReports,
                    financial,
                    citations,
                    attempt.attemptNumber());

            ScoringResult bestScoringResult = survivingScores.stream()
                    .max(Comparator.comparingDouble(ScoringResult::getOverallScore))
                    .orElse(null);

            sessionMemory.saveQueryContext(sessionId, request.getQuery(), safety.safeAnswer());

            long elapsed = System.currentTimeMillis() - start;
            log.info("SRLM[done] intent={} chunks={} candidates={} bestScore={} rejected={} blocked={} fabricated={} financial={} elapsed={}ms",
                    intent, attempt.chunks().size(), candidates.size(), bestScore,
                    rejectedPaths.size(), safety.blocked(), safety.fabricatedExclusion(),
                    financial == null ? "n/a" : financial.getStatus(), elapsed);

            boolean trace = Boolean.TRUE.equals(request.getIncludeSteps());

            return SRLMResponse.builder()
                    .query(request.getQuery())
                    .finalAnswer(safety.safeAnswer())
                    .selectedPath(bestScoringResult != null ? bestScoringResult.getPath() : null)
                    .confidenceScore(bestScore)
                    .detectedIntent(intent)
                    .retrievedChunks(trace ? attempt.chunks() : topNRedacted(attempt.chunks(), 3))
                    .clauseClassifications(trace ? classifications : null)
                    .allCandidates(trace ? candidates : null)
                    .reflections(trace ? reflections : null)
                    .scores(trace ? scores : null)
                    .contradictionReports(trace ? contradictionReports : null)
                    .citations(citations)
                    .rejectedPaths(rejectedPaths)
                    .safetyWarnings(safety.warnings())
                    .financialCalculation(financial)
                    .structuredAnswer(structured)
                    .retrievalAttempts(attempt.attemptNumber())
                    .retrievalValid(attempt.validation().valid())
                    .retrievalRejectionReason(attempt.validation().valid() ? null : attempt.validation().reason())
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

    /** Treatment path — unchanged interface, used by AgentOrchestrator. */
    public SRLMResponse processTreatmentQuery(QueryRequest request, String patientContext, QueryIntent intent) {
        long start = System.currentTimeMillis();
        String sessionId = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString();
        if (patientContext == null || patientContext.isBlank()) {
            return noContextResponse(request.getQuery(), intent,
                    new RetrievalAttempt(List.of(), 1,
                            new ContextValidationService.Result(false, false, "No patient context", List.of(), List.of())),
                    System.currentTimeMillis() - start);
        }
        try {
            List<ReasoningCandidate> candidates =
                    multiReasoningService.generateTreatmentReasoningPaths(
                            request.getQuery(), patientContext, intent);
            if (candidates.isEmpty()) throw new AgentException("All treatment reasoning paths failed", null);

            List<ReflectionResult> reflections =
                    selfReflectionService.reflectOnCandidates(candidates, request.getQuery(), patientContext);
            List<ScoringResult> scores =
                    scoringService.scoreCandidates(candidates, reflections, request.getQuery(), patientContext);

            SynthesisService.SynthesisOutput synthesis =
                    synthesisService.synthesize(candidates, scores, request.getQuery(), intent);

            double bestScore = scores.stream().mapToDouble(ScoringResult::getOverallScore).max().orElse(0.0);
            GuardrailService.SafetyCheckResult safety =
                    guardrailService.checkAnswerLenient(synthesis.answer(), bestScore);

            ScoringResult best = scores.stream()
                    .max(Comparator.comparingDouble(ScoringResult::getOverallScore)).orElse(null);

            sessionMemory.saveQueryContext(sessionId, request.getQuery(), safety.safeAnswer());
            long elapsed = System.currentTimeMillis() - start;
            boolean trace = Boolean.TRUE.equals(request.getIncludeSteps());

            return SRLMResponse.builder()
                    .query(request.getQuery())
                    .finalAnswer(safety.safeAnswer())
                    .selectedPath(best != null ? best.getPath() : null)
                    .confidenceScore(bestScore)
                    .detectedIntent(intent)
                    .allCandidates(trace ? candidates : null)
                    .reflections(trace ? reflections : null)
                    .scores(trace ? scores : null)
                    .synthesisReasoning(synthesis.reasoning())
                    .processingTimeMs(elapsed)
                    .model(model)
                    .build();
        } catch (AgentException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("SRLM-treatment processing failed: {}", ex.getMessage(), ex);
            throw new AgentException("SRLM-treatment failed: " + ex.getMessage(), ex);
        }
    }

    // ── Retrieval loop ────────────────────────────────────────────────────────

    private RetrievalAttempt retrieveWithRetry(QueryRequest request) {
        return retrieveWithRetry(request, defaultTopK, 1);
    }

    private RetrievalAttempt retrieveWithRetry(QueryRequest request, int topK, int attemptNumber) {
        String filter = request.getDocumentId() != null
                ? "document_id == '" + request.getDocumentId() + "'"
                : null;

        List<RetrievedChunk> chunks = hybridRetrievalService.retrieve(request.getQuery(), topK, filter);
        ContextValidationService.Result validation =
                contextValidationService.validate(request.getQuery(), chunks);

        if (!validation.valid() && validation.shouldRetry() && attemptNumber < maxAttempts) {
            String refined = expandQuery(request.getQuery(), validation.missingEntities());
            log.warn("Retrieval attempt {} invalid ({}). Retrying with refined query '{}' and topK={}",
                    attemptNumber, validation.reason(), refined, retryTopK);
            List<RetrievedChunk> retryChunks = hybridRetrievalService.retrieve(refined, retryTopK, filter);
            ContextValidationService.Result retryValidation =
                    contextValidationService.validate(request.getQuery(), retryChunks);
            chunks = retryChunks;
            validation = retryValidation;
            attemptNumber++;
        }

        chunks = retrievalRerankerService.rerank(request.getQuery(), chunks);
        return new RetrievalAttempt(chunks, attemptNumber, validation);
    }

    private QueryRequest cloneWithRefinedQuery(QueryRequest original) {
        QueryRequest copy = new QueryRequest();
        copy.setQuery(original.getQuery() + " policy coverage clause exact wording");
        copy.setSessionId(original.getSessionId());
        copy.setDocumentId(original.getDocumentId());
        copy.setPatientUserId(original.getPatientUserId());
        copy.setIncludeSteps(original.getIncludeSteps());
        return copy;
    }

    private String expandQuery(String original, List<String> missingEntities) {
        if (missingEntities == null || missingEntities.isEmpty()) {
            return original + " policy coverage clause";
        }
        return original + " " + String.join(" ", missingEntities) + " clause";
    }

    /**
     * Build the [EVIDENCE CLAUSES] block for the reasoning prompt with stable
     * chunkIds the model can cite with [CITE: <id>] and (optionally) per-chunk
     * classifications so the model can preferentially use COVERAGE clauses for
     * coverage questions, FINANCIAL_LIMIT clauses for financial questions, etc.
     */
    private String buildEvidenceContext(List<RetrievedChunk> chunks,
                                        List<ClauseClassification> classifications) {
        java.util.Map<String, ClauseClassification> byId = new java.util.HashMap<>();
        if (classifications != null) {
            for (ClauseClassification cl : classifications) {
                if (cl.getChunkId() != null) byId.put(cl.getChunkId(), cl);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (RetrievedChunk c : chunks) {
            ClauseClassification cl = byId.get(c.getChunkId());
            sb.append("[chunkId=").append(c.getChunkId()).append("] ");
            sb.append("(score=").append(c.getFinalScore())
              .append(", category=").append(cl == null ? "?" : cl.getCategory())
              .append(", keywords=").append(c.getMatchedKeywords()).append(")\n");
            sb.append(c.getContent() == null ? "" : c.getContent().trim()).append("\n---\n");
        }
        return sb.toString();
    }

    private String buildEvidenceContext(List<RetrievedChunk> chunks) {
        return buildEvidenceContext(chunks, java.util.List.of());
    }

    private List<RetrievedChunk> topNRedacted(List<RetrievedChunk> chunks, int n) {
        return chunks.stream().limit(n).toList();
    }

    private SRLMResponse noContextResponse(String query, QueryIntent intent,
                                           RetrievalAttempt attempt, long elapsed) {
        return SRLMResponse.builder()
                .query(query)
                .finalAnswer("The uploaded policy does not contain enough information to answer this question. "
                        + "Please upload your insurance policy document and try again.")
                .detectedIntent(intent)
                .confidenceScore(0.0)
                .retrievedChunks(attempt.chunks())
                .retrievalAttempts(attempt.attemptNumber())
                .retrievalValid(attempt.validation().valid())
                .retrievalRejectionReason(attempt.validation().reason())
                .synthesisReasoning("Insufficient context: " + attempt.validation().reason())
                .processingTimeMs(elapsed)
                .model(model)
                .build();
    }

    private SRLMResponse blockedResponse(QueryRequest request, QueryIntent intent,
                                         RetrievalAttempt attempt,
                                         List<ReasoningCandidate> candidates,
                                         List<ReflectionResult> reflections,
                                         List<ScoringResult> scores,
                                         List<ContradictionReport> contradictions,
                                         List<String> rejectedPaths,
                                         String reason, long elapsed) {
        boolean trace = Boolean.TRUE.equals(request.getIncludeSteps());
        return SRLMResponse.builder()
                .query(request.getQuery())
                .finalAnswer("The uploaded policy does not contain enough information to answer this question.")
                .detectedIntent(intent)
                .confidenceScore(0.0)
                .retrievedChunks(trace ? attempt.chunks() : topNRedacted(attempt.chunks(), 3))
                .allCandidates(trace ? candidates : null)
                .reflections(trace ? reflections : null)
                .scores(trace ? scores : null)
                .contradictionReports(trace ? contradictions : null)
                .rejectedPaths(rejectedPaths)
                .safetyWarnings(List.of(reason))
                .retrievalAttempts(attempt.attemptNumber())
                .retrievalValid(attempt.validation().valid())
                .synthesisReasoning(reason)
                .processingTimeMs(elapsed)
                .model(model)
                .build();
    }

    private record RetrievalAttempt(
            List<RetrievedChunk> chunks,
            int attemptNumber,
            ContextValidationService.Result validation) {}
}
