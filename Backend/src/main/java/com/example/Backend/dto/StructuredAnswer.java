package com.example.Backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Production-grade structured answer. Mirrors Phase-15 of the spec:
 *
 *   Coverage Status      : COVERED | PARTIALLY_COVERED | NOT_COVERED | UNKNOWN
 *   Explanation          : 1-3 sentence patient-friendly summary
 *   Patient Payable      : optional, only when financial calc applies
 *   Policy Clause        : the supporting clause quote
 *   Confidence           : 0-10 scalar with breakdown
 *   Reasoning Category   : ClauseCategory enum name from PolicyClauseClassifier
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StructuredAnswer {
    private String coverageStatus;
    private String explanation;
    private BigDecimal patientPayable;
    private BigDecimal coveredAmount;
    private String currency;
    private String unit;
    private String policyClause;
    private double confidence;
    private ConfidenceBreakdown confidenceBreakdown;
    private String reasoningCategory;
    private List<Citation> citations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfidenceBreakdown {
        private double retrieval;
        private double evidenceGrounding;
        private double contradictionFree;
        private double numericalValidity;
        private double reflection;
    }
}
