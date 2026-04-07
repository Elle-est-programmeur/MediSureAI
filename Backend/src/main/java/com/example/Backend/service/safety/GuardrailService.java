package com.example.Backend.service.safety;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class GuardrailService {

    private static final List<String> HALLUCINATION_INDICATORS = List.of(
            "I believe", "I think", "probably", "might be", "could be", "as far as I know"
    );

    private static final String LOW_CONFIDENCE_FALLBACK =
            "I don't have enough information in your documents to answer this confidently. " +
            "Please upload more relevant documents or rephrase your question.";

    @Value("${safety.min-confidence:4.0}")
    private double minConfidence;

    public SafetyCheckResult checkAnswer(String answer, Double confidence) {
        List<String> warnings = new ArrayList<>();
        boolean shouldBlock = false;

        // Check 1: Hallucination — uncertain language
        boolean hallucinationDetected = HALLUCINATION_INDICATORS.stream()
                .anyMatch(indicator -> answer.toLowerCase().contains(indicator.toLowerCase()));
        if (hallucinationDetected) {
            warnings.add("Potential hallucination: uncertain language detected");
        }

        // Check 2: Low confidence threshold
        if (confidence != null && confidence < minConfidence) {
            warnings.add(String.format("Low confidence score: %.1f/10", confidence));
            shouldBlock = true;
        }

        // Check 3: Empty or very short answer
        if (answer == null || answer.length() < 50) {
            warnings.add("Answer too short, may be incomplete");
            shouldBlock = true;
        }

        // Check 4: No explicit source citations
        if (answer != null
                && !answer.toLowerCase().contains("according to")
                && !answer.toLowerCase().contains("based on")) {
            warnings.add("No explicit source citations");
        }

        if (!warnings.isEmpty()) {
            log.warn("Safety warnings for answer: {}", warnings);
        }

        String safeAnswer = shouldBlock ? LOW_CONFIDENCE_FALLBACK : answer;

        return new SafetyCheckResult(safeAnswer, warnings, shouldBlock);
    }

    public record SafetyCheckResult(
            String safeAnswer,
            List<String> warnings,
            boolean blocked) {}
}