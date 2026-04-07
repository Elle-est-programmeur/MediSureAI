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

    private List<ReasoningCandidate> allCandidates;
    private List<ReflectionResult> reflections;
    private List<ScoringResult> scores;

    private String synthesisReasoning;
    private long processingTimeMs;
    private String model;
}