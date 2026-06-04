package com.example.Backend.dto;

import com.example.Backend.model.QueryIntent;
import com.example.Backend.model.ReasoningPath;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SRLMResponse {

    private String query;
    private String finalAnswer;
    private ReasoningPath selectedPath;
    private double confidenceScore;
    private QueryIntent detectedIntent;

    // Evidence + reasoning trace
    private List<RetrievedChunk> retrievedChunks;
    private List<ClauseClassification> clauseClassifications;
    private List<ReasoningCandidate> allCandidates;
    private List<ReflectionResult> reflections;
    private List<ScoringResult> scores;
    private List<ContradictionReport> contradictionReports;
    private List<Citation> citations;
    private List<String> rejectedPaths;
    private List<String> safetyWarnings;
    private FinancialCalculation financialCalculation;
    private StructuredAnswer structuredAnswer;
    private Integer retrievalAttempts;
    private Boolean retrievalValid;
    private String retrievalRejectionReason;

    private String synthesisReasoning;
    private long processingTimeMs;
    private String model;
}
