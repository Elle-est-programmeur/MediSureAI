package com.example.Backend.dto;

import com.example.Backend.model.ReasoningPath;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoringResult {

    private ReasoningPath path;
    private double relevanceScore;
    private double correctnessScore;
    private double completenessScore;
    private double evidenceConsistencyScore;
    private double contradictionPenalty;
    private double overallScore;
    private String scoringReasoning;
    private boolean rejected;
    private String rejectionReason;
}