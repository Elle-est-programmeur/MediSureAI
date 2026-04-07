package com.example.Backend.dto;

import com.example.Backend.model.QueryIntent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryResponse {
    private String query;
    private String answer;
    private QueryIntent detectedIntent;
    private Double intentConfidence;

    // Confidence calibration
    private Double overallConfidence;      // 0-10 scale
    private String confidenceLevel;        // "LOW", "MEDIUM", "HIGH"
    private List<String> confidenceFactors;

    // Safety
    private List<String> safetyWarnings;

    private List<String> citations;
    /** Null unless {@code includeSteps: true} was requested. */
    private List<Map<String, Object>> executionSteps;
    private long processingTimeMs;
    private String model;
}