package com.example.Backend.service.srlm;

import com.example.Backend.dto.ReasoningCandidate;
import com.example.Backend.dto.ReflectionResult;
import com.example.Backend.service.llm.LLMService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Asks the LLM to critically evaluate each reasoning candidate.
 * Returns structured feedback used by the scoring stage.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SelfReflectionService {

    private final LLMService llmService;
    private final ObjectMapper objectMapper;

    @Value("${srlm.reflection.enabled:true}")
    private boolean reflectionEnabled;

    public List<ReflectionResult> reflectOnCandidates(
            List<ReasoningCandidate> candidates, String query, String context) {

        List<ReflectionResult> results = new ArrayList<>();

        for (ReasoningCandidate candidate : candidates) {
            if (!reflectionEnabled) {
                results.add(defaultPassReflection(candidate));
                continue;
            }
            try {
                String prompt = buildReflectionPrompt(query, context, candidate);
                String response = llmService.generateCompletion(prompt);
                results.add(parseReflection(candidate, response));
            } catch (Exception e) {
                log.warn("Reflection failed for path {}: {}", candidate.getPath(), e.getMessage());
                results.add(defaultPassReflection(candidate));
            }
        }

        return results;
    }

    private String buildReflectionPrompt(String query, String context, ReasoningCandidate candidate) {
        return String.format("""
                You are a critical reviewer evaluating an AI-generated answer about healthcare insurance.

                ORIGINAL QUESTION: %s

                REFERENCE CONTEXT:
                %s

                CANDIDATE ANSWER (reasoning approach: %s):
                %s

                Critically evaluate this answer. Respond ONLY with valid JSON (no markdown):
                {
                  "isValid": true/false,
                  "factsCorrect": true/false,
                  "hasContradictions": true/false,
                  "strengths": ["strength1", "strength2"],
                  "weaknesses": ["weakness1", "weakness2"],
                  "overallAssessment": "one sentence summary"
                }
                """,
                query, context, candidate.getPath().name(), candidate.getAnswer());
    }

    private ReflectionResult parseReflection(ReasoningCandidate candidate, String response) {
        try {
            String json = stripCodeFence(response);
            JsonNode node = objectMapper.readTree(json);
            return ReflectionResult.builder()
                    .path(candidate.getPath())
                    .isValid(node.path("isValid").asBoolean(true))
                    .factsCorrect(node.path("factsCorrect").asBoolean(true))
                    .hasContradictions(node.path("hasContradictions").asBoolean(false))
                    .strengths(parseStringList(node.path("strengths")))
                    .weaknesses(parseStringList(node.path("weaknesses")))
                    .overallAssessment(node.path("overallAssessment").asText(""))
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse reflection JSON for path {}: {}", candidate.getPath(), e.getMessage());
            return defaultPassReflection(candidate);
        }
    }

    private List<String> parseStringList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(item -> list.add(item.asText()));
        }
        return list;
    }

    private ReflectionResult defaultPassReflection(ReasoningCandidate candidate) {
        return ReflectionResult.builder()
                .path(candidate.getPath())
                .isValid(true)
                .factsCorrect(true)
                .hasContradictions(false)
                .strengths(List.of("Answer provided"))
                .weaknesses(List.of())
                .overallAssessment("Reflection not performed")
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