package com.example.Backend.service.srlm;

import com.example.Backend.dto.Citation;
import com.example.Backend.dto.ClauseClassification;
import com.example.Backend.dto.ContradictionReport;
import com.example.Backend.dto.FinancialCalculation;
import com.example.Backend.dto.ReflectionResult;
import com.example.Backend.dto.RetrievedChunk;
import com.example.Backend.dto.ScoringResult;
import com.example.Backend.dto.StructuredAnswer;
import com.example.Backend.model.ClauseCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Builds the structured production-format answer + a calibrated multi-dim
 * confidence breakdown. Pure assembly logic — no LLM call.
 */
@Service
@Slf4j
public class StructuredAnswerAssembler {

    public StructuredAnswer assemble(
            String narrative,
            List<RetrievedChunk> chunks,
            List<ClauseClassification> classifications,
            List<ReflectionResult> reflections,
            List<ScoringResult> scores,
            List<ContradictionReport> contradictions,
            FinancialCalculation calc,
            List<Citation> citations,
            int retrievalAttempts) {

        String coverageStatus = pickCoverageStatus(calc, classifications, contradictions);
        String reasoningCategory = pickReasoningCategory(classifications);
        String clause = pickSupportingClause(calc, chunks, classifications);

        StructuredAnswer.ConfidenceBreakdown breakdown = buildConfidenceBreakdown(
                chunks, reflections, scores, contradictions, calc, retrievalAttempts);
        double confidence = combineConfidence(breakdown);

        return StructuredAnswer.builder()
                .coverageStatus(coverageStatus)
                .explanation(narrative)
                .patientPayable(calc == null ? null : calc.getPatientPayable())
                .coveredAmount(calc == null ? null : calc.getCoveredAmount())
                .currency(calc == null ? null : calc.getCurrency())
                .unit(calc == null ? null : calc.getUnit())
                .policyClause(clause)
                .confidence(round(confidence))
                .confidenceBreakdown(breakdown)
                .reasoningCategory(reasoningCategory)
                .citations(citations)
                .build();
    }

    private String pickCoverageStatus(FinancialCalculation calc,
                                      List<ClauseClassification> classifications,
                                      List<ContradictionReport> contradictions) {
        if (calc != null && calc.getStatus() != null
                && calc.getStatus() != FinancialCalculation.CoverageStatus.UNKNOWN) {
            return calc.getStatus().name();
        }
        // Otherwise infer from the dominant clause category among top-scored chunks
        ClauseCategory cat = dominantCategory(classifications);
        return switch (cat) {
            case COVERAGE, BENEFIT -> "COVERED";
            case EXCLUSION -> "NOT_COVERED";
            case LIMITATION, FINANCIAL_LIMIT -> "PARTIALLY_COVERED";
            case WAITING_PERIOD -> "CONDITIONALLY_COVERED";
            case CLAIM_PROCESS, GENERAL_INFO -> "UNKNOWN";
        };
    }

    private String pickReasoningCategory(List<ClauseClassification> classifications) {
        ClauseCategory cat = dominantCategory(classifications);
        return cat.name();
    }

    private ClauseCategory dominantCategory(List<ClauseClassification> classifications) {
        if (classifications == null || classifications.isEmpty()) return ClauseCategory.GENERAL_INFO;
        // Weight categories by classification confidence
        java.util.EnumMap<ClauseCategory, Double> weights = new java.util.EnumMap<>(ClauseCategory.class);
        for (ClauseClassification c : classifications) {
            if (c.getCategory() == null) continue;
            weights.merge(c.getCategory(), c.getConfidence(), Double::sum);
        }
        return weights.entrySet().stream()
                .max(Comparator.comparingDouble(java.util.Map.Entry::getValue))
                .map(java.util.Map.Entry::getKey)
                .orElse(ClauseCategory.GENERAL_INFO);
    }

    private String pickSupportingClause(FinancialCalculation calc,
                                        List<RetrievedChunk> chunks,
                                        List<ClauseClassification> classifications) {
        if (calc != null && calc.getSupportingClause() != null && !calc.getSupportingClause().isBlank()) {
            return truncate(calc.getSupportingClause(), 320);
        }
        if (chunks == null || chunks.isEmpty()) return null;
        // Prefer the highest-scored chunk whose classification is informative
        if (classifications != null && !classifications.isEmpty()) {
            for (RetrievedChunk c : chunks) {
                ClauseClassification cl = classifications.stream()
                        .filter(x -> x.getChunkId() != null && x.getChunkId().equals(c.getChunkId()))
                        .findFirst().orElse(null);
                if (cl != null && cl.getCategory() != ClauseCategory.GENERAL_INFO) {
                    return truncate(c.getContent(), 320);
                }
            }
        }
        return truncate(chunks.get(0).getContent(), 320);
    }

    private StructuredAnswer.ConfidenceBreakdown buildConfidenceBreakdown(
            List<RetrievedChunk> chunks,
            List<ReflectionResult> reflections,
            List<ScoringResult> scores,
            List<ContradictionReport> contradictions,
            FinancialCalculation calc,
            int retrievalAttempts) {

        // Retrieval: top chunk finalScore (×10) penalised by retry count
        double retrieval = 0.0;
        if (chunks != null && !chunks.isEmpty()) {
            retrieval = chunks.get(0).getFinalScore() * 10.0;
            retrieval -= Math.max(0, retrievalAttempts - 1) * 1.5;
            retrieval = clamp(retrieval, 0, 10);
        }

        // Evidence grounding: avg evidenceConsistencyScore across non-rejected scores
        double evidence = 0.0;
        long n = 0;
        if (scores != null) {
            for (ScoringResult s : scores) {
                if (s.isRejected()) continue;
                evidence += s.getEvidenceConsistencyScore();
                n++;
            }
            if (n > 0) evidence /= n;
        }

        // Contradiction-free: 10 minus the worst severity across non-rejected paths × 10
        double contradictionFree = 10.0;
        if (contradictions != null && scores != null) {
            java.util.Set<Object> rejected = new java.util.HashSet<>();
            for (ScoringResult s : scores) if (s.isRejected()) rejected.add(s.getPath());
            double worst = 0.0;
            for (ContradictionReport r : contradictions) {
                if (rejected.contains(r.getPath())) continue;
                worst = Math.max(worst, r.getSeverity());
            }
            contradictionFree = clamp(10.0 - 10.0 * worst, 0, 10);
        }

        // Numerical validity: 10 if calc valid, 0 if not, 7 if no calc was needed
        double numerical;
        if (calc == null || calc.getStatus() == FinancialCalculation.CoverageStatus.UNKNOWN) {
            numerical = 7.0;
        } else {
            numerical = calc.isValid() ? 10.0 : 0.0;
        }

        // Reflection: fraction of reflections that pass evidenceGrounded × 10
        double reflection = 10.0;
        if (reflections != null && !reflections.isEmpty()) {
            long good = reflections.stream().filter(r ->
                    r.isEvidenceGrounded() && !r.isFabricatedExclusion() && !r.isUnsupportedAssumption()
            ).count();
            reflection = (10.0 * good) / reflections.size();
        }

        return StructuredAnswer.ConfidenceBreakdown.builder()
                .retrieval(round(retrieval))
                .evidenceGrounding(round(evidence))
                .contradictionFree(round(contradictionFree))
                .numericalValidity(round(numerical))
                .reflection(round(reflection))
                .build();
    }

    /**
     * Combined confidence is capped by evidence grounding — confidence can never
     * be higher than the evidence supports.
     */
    private double combineConfidence(StructuredAnswer.ConfidenceBreakdown b) {
        double blended = (
                0.20 * b.getRetrieval() +
                0.30 * b.getEvidenceGrounding() +
                0.20 * b.getContradictionFree() +
                0.15 * b.getNumericalValidity() +
                0.15 * b.getReflection()
        );
        return Math.min(blended, b.getEvidenceGrounding());
    }

    private double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
    private double round(double v) { return Math.round(v * 10.0) / 10.0; }

    private String truncate(String s, int n) {
        if (s == null) return null;
        String t = s.trim().replaceAll("\\s+", " ");
        return t.length() <= n ? t : t.substring(0, n) + "…";
    }
}
