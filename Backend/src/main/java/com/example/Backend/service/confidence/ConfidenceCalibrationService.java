package com.example.Backend.service.confidence;

import com.example.Backend.dto.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ConfidenceCalibrationService {

    public ConfidenceScore calculateConfidence(
            Double intentConfidence,
            List<ToolResult> toolResults,
            int contextLength) {

        List<String> factors = new ArrayList<>();
        double totalScore = 0.0;
        int factorCount = 0;

        // Factor 1: Intent confidence (0-1 → 0-10)
        if (intentConfidence != null) {
            double intentScore = intentConfidence * 10;
            totalScore += intentScore;
            factorCount++;
            factors.add(String.format("Intent detection: %.1f/10", intentScore));
        }

        // Factor 2: Retrieval quality (avg tool confidence)
        double avgToolConfidence = toolResults.stream()
                .filter(ToolResult::isSuccess)
                .mapToDouble(r -> r.getConfidence() != null ? r.getConfidence() : 0.5)
                .average()
                .orElse(0.0);
        double retrievalScore = avgToolConfidence * 10;
        totalScore += retrievalScore;
        factorCount++;
        factors.add(String.format("Retrieval quality: %.1f/10", retrievalScore));

        // Factor 3: Context availability (500+ chars = full 10)
        double contextScore = Math.min(contextLength / 500.0, 1.0) * 10;
        totalScore += contextScore;
        factorCount++;
        factors.add(String.format("Context availability: %.1f/10", contextScore));

        double overall = factorCount > 0 ? totalScore / factorCount : 0.0;
        overall = Math.round(overall * 10.0) / 10.0;

        String level = overall >= 7.0 ? "HIGH" : overall >= 4.0 ? "MEDIUM" : "LOW";

        log.info("Confidence: {}/10 ({})", overall, level);

        return new ConfidenceScore(overall, level, factors);
    }

    public record ConfidenceScore(
            Double score,
            String level,
            List<String> factors) {}
}