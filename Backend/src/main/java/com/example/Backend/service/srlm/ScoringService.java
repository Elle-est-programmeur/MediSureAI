package com.example.Backend.service.srlm;

import com.example.Backend.dto.ReasoningCandidate;
import com.example.Backend.dto.ReflectionResult;
import com.example.Backend.dto.ScoringResult;
import com.example.Backend.service.llm.LLMService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Scores each reasoning candidate on relevance, correctness, and completeness.
 * Incorporates reflection feedback as a penalty modifier.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScoringService {

    private final LLMService llmService;
    private final ObjectMapper objectMapper;

    @Value("${srlm.scoring.min-confidence:6.0}")
    private double minConfidence;

    public List<ScoringResult> scoreCandidates(
            List<ReasoningCandidate> candidates,
            List<ReflectionResult> reflections,
            String query,
            String context) {

        Map<Object, ReflectionResult> reflectionMap = reflections.stream()
                .collect(Collectors.toMap(ReflectionResult::getPath, Function.identity()));

        List<ScoringResult> results = new ArrayList<>();

        for (ReasoningCandidate candidate : candidates) {
            try {
                ReflectionResult reflection = reflectionMap.get(candidate.getPath());
                String prompt = buildScoringPrompt(query, context, candidate, reflection);
                String response = llmService.generateCompletion(prompt);
                ScoringResult score = parseScore(candidate, response, reflection);
                results.add(score);
                log.debug("Scored path {} → overall={}", candidate.getPath(), score.getOverallScore());
            } catch (Exception e) {
                log.warn("Scoring failed for path {}: {}", candidate.getPath(), e.getMessage());
                results.add(defaultScore(candidate));
            }
        }

        return results;
    }

    private String buildScoringPrompt(
            String query, String context,
            ReasoningCandidate candidate, ReflectionResult reflection) {

        String reflectionSummary = reflection != null
                ? String.format("Valid: %b, FactsCorrect: %b, Contradictions: %b. Assessment: %s",
                        reflection.isValid(), reflection.isFactsCorrect(),
                        reflection.isHasContradictions(), reflection.getOverallAssessment())
                : "No reflection available";

        return String.format("""
                Score the following healthcare insurance answer on a scale of 1-10 for each dimension.

                QUESTION: %s

                REFERENCE CONTEXT:
                %s

                ANSWER (path: %s):
                %s

                REFLECTION FEEDBACK: %s

                Respond ONLY with valid JSON (no markdown):
                {
                  "relevanceScore": 8.5,
                  "correctnessScore": 7.0,
                  "completenessScore": 6.5,
                  "scoringReasoning": "brief explanation"
                }
                """,
                query, context, candidate.getPath().name(), candidate.getAnswer(), reflectionSummary);
    }

    private ScoringResult parseScore(
            ReasoningCandidate candidate, String response, ReflectionResult reflection) {
        try {
            String json = stripCodeFence(response);
            JsonNode node = objectMapper.readTree(json);

            double relevance = node.path("relevanceScore").asDouble(5.0);
            double correctness = node.path("correctnessScore").asDouble(5.0);
            double completeness = node.path("completenessScore").asDouble(5.0);
            String reasoning = node.path("scoringReasoning").asText("");

            double overall = (relevance + correctness + completeness) / 3.0;

            // Penalise answers flagged as invalid or having contradictions
            if (reflection != null) {
                if (!reflection.isValid()) overall *= 0.6;
                else if (reflection.isHasContradictions()) overall *= 0.8;
                else if (!reflection.isFactsCorrect()) overall *= 0.7;
            }

            return ScoringResult.builder()
                    .path(candidate.getPath())
                    .relevanceScore(relevance)
                    .correctnessScore(correctness)
                    .completenessScore(completeness)
                    .overallScore(Math.round(overall * 10.0) / 10.0)
                    .scoringReasoning(reasoning)
                    .build();

        } catch (Exception e) {
            log.warn("Failed to parse scoring JSON for path {}: {}", candidate.getPath(), e.getMessage());
            return defaultScore(candidate);
        }
    }

    private ScoringResult defaultScore(ReasoningCandidate candidate) {
        return ScoringResult.builder()
                .path(candidate.getPath())
                .relevanceScore(5.0)
                .correctnessScore(5.0)
                .completenessScore(5.0)
                .overallScore(5.0)
                .scoringReasoning("Default score — LLM scoring unavailable")
                .build();
    }

    private String stripCodeFence(String text) {
        String stripped = text.trim();
        if (stripped.startsWith("```")) {
            stripped = stripped.replaceFirst("```[a-zA-Z]*\\n?", "");
            int end = stripped.lastIndexOf("```");
            if (end != -1) stripped = stripped.substring(0, end);
        }
        return stripped.trim();
    }
}