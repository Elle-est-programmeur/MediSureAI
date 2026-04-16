package com.example.Backend.service.srlm;

import com.example.Backend.dto.ReasoningCandidate;
import com.example.Backend.dto.ReflectionResult;
import com.example.Backend.dto.ScoringResult;
import com.example.Backend.service.llm.CritiqueLLMService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import java.util.concurrent.CompletableFuture;

/**
 * Advanced LLM-based scoring for reasoning candidates.
 *
 * Each path is evaluated on Relevance, Correctness, and Completeness.
 * Restored to full spec: Uses OpenRouter to provide deep qualitative analysis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScoringService {

    private final CritiqueLLMService llmService;
    private final ObjectMapper objectMapper;

    public List<ScoringResult> scoreCandidates(
            List<ReasoningCandidate> candidates,
            List<ReflectionResult> reflections,
            String query,
            String context) {

        log.info("Scoring {} reasoning candidates in parallel via LLM...", candidates.size());

        Map<Object, ReflectionResult> reflectionMap = reflections.stream()
                .collect(Collectors.toMap(ReflectionResult::getPath, Function.identity()));

        List<CompletableFuture<ScoringResult>> futures = candidates.stream()
                .map(candidate -> CompletableFuture.supplyAsync(() -> {
                    try {
                        ReflectionResult reflection = reflectionMap.get(candidate.getPath());
                        String prompt = buildScoringPrompt(query, context, candidate, reflection);
                        String response = llmService.generateCritique(prompt);
                        return parseScoring(candidate, response);
                    } catch (Exception e) {
                        log.warn("Scoring failed for path {}: {}", candidate.getPath(), e.getMessage());
                        return defaultScore(candidate);
                    }
                }))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    private String buildScoringPrompt(
            String query, String context, ReasoningCandidate candidate, ReflectionResult reflection) {
        
        String reflectionText = reflection != null ? reflection.getOverallAssessment() : "None";

        return String.format("""
                You are a quality auditor for a healthcare insurance AI.
                Score the following answer on a scale of 1.0 to 10.0 across three metrics.
                
                QUESTION: %s
                CONTEXT: %s
                ANSWER: %s
                REFLECTION SUMMARY: %s
                
                METRICS:
                1. Relevance: Did it answer the specific user question?
                2. Correctness: Are the facts supported by the context?
                3. Completeness: Does it cover all necessary details?
                
                Return ONLY valid JSON:
                {
                  "relevance": 0.0,
                  "correctness": 0.0,
                  "completeness": 0.0,
                  "overall": 0.0,
                  "reasoning": "one sentence explanation"
                }
                """, query, context, candidate.getAnswer(), reflectionText);
    }

    private ScoringResult parseScoring(ReasoningCandidate candidate, String response) {
        try {
            String json = stripCodeFence(response);
            JsonNode node = objectMapper.readTree(json);
            
            return ScoringResult.builder()
                    .path(candidate.getPath())
                    .relevanceScore(node.path("relevance").asDouble(5.0))
                    .correctnessScore(node.path("correctness").asDouble(5.0))
                    .completenessScore(node.path("completeness").asDouble(5.0))
                    .overallScore(node.path("overall").asDouble(5.0))
                    .scoringReasoning(node.path("reasoning").asText("LLM analyzed quality"))
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse scoring JSON: {}", response);
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
                .scoringReasoning("Fallback score due to analysis error")
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