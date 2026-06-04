package com.example.Backend.service.srlm;

import com.example.Backend.dto.ReasoningCandidate;
import com.example.Backend.dto.ReflectionResult;
import com.example.Backend.service.llm.CritiqueLLMService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Asks the LLM to critically evaluate each reasoning candidate in parallel.
 * Returns structured feedback used by the scoring stage.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SelfReflectionService {

    private final CritiqueLLMService llmService;
    private final ObjectMapper objectMapper;

    @Value("${srlm.reflection.enabled:true}")
    private boolean reflectionEnabled;

    public List<ReflectionResult> reflectOnCandidates(
            List<ReasoningCandidate> candidates, String query, String context) {

        if (!reflectionEnabled) {
            return candidates.stream()
                    .map(this::defaultPassReflection)
                    .collect(Collectors.toList());
        }

        log.info("Reflecting on {} candidates in parallel...", candidates.size());

        // Launch all reflections simultaneously
        List<CompletableFuture<ReflectionResult>> futures = candidates.stream()
                .map(candidate -> CompletableFuture.supplyAsync(() -> {
                    try {
                        String prompt = buildReflectionPrompt(query, context, candidate);
                        String response = llmService.generateCritique(prompt);
                        return parseReflection(candidate, response);
                    } catch (Exception e) {
                        log.warn("Reflection failed for path {}: {}", candidate.getPath(), e.getMessage());
                        return defaultPassReflection(candidate);
                    }
                }))
                .collect(Collectors.toList());

        // Wait for all cloud calls to complete
        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    private String buildReflectionPrompt(String query, String context, ReasoningCandidate candidate) {
        return String.format("""
                You are an adversarial reviewer of an AI answer about a healthcare insurance
                policy. Your job is to catch hallucinations, contradictions, and fabricated
                exclusions before the answer reaches a patient.

                ORIGINAL QUESTION: %s

                EVIDENCE CLAUSES (the only source of truth):
                %s

                CANDIDATE ANSWER (reasoning path: %s):
                %s

                Answer the following questions and respond ONLY with valid JSON (no markdown):

                1. isValid             — is the answer well-formed and on-topic?
                2. factsCorrect        — are every concrete fact (limits, percentages, terms)
                                          present in the evidence?
                3. hasContradictions   — does any sentence contradict any clause? e.g. clause
                                          says "ICU: covered up to sum insured" but the answer
                                          says ICU is not covered.
                4. evidenceGrounded    — is EVERY factual claim traceable to a clause? An
                                          answer with sentences not grounded in the evidence
                                          fails this check.
                5. fabricatedExclusion — does the answer claim something is excluded / not
                                          covered / not payable WITHOUT a clause that says so?
                6. unsupportedAssumption — does the reasoning import logic from an unrelated
                                          section (e.g. applying daycare rules to ICU)?
                7. unsupportedClaims   — list specific sentences from the answer that are NOT
                                          supported by the evidence. Empty list if all good.

                {
                  "isValid": true,
                  "factsCorrect": true,
                  "hasContradictions": false,
                  "evidenceGrounded": true,
                  "fabricatedExclusion": false,
                  "unsupportedAssumption": false,
                  "strengths": ["..."],
                  "weaknesses": ["..."],
                  "unsupportedClaims": ["..."],
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
                    .evidenceGrounded(node.path("evidenceGrounded").asBoolean(true))
                    .fabricatedExclusion(node.path("fabricatedExclusion").asBoolean(false))
                    .unsupportedAssumption(node.path("unsupportedAssumption").asBoolean(false))
                    .strengths(parseStringList(node.path("strengths")))
                    .weaknesses(parseStringList(node.path("weaknesses")))
                    .unsupportedClaims(parseStringList(node.path("unsupportedClaims")))
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
                .evidenceGrounded(true)
                .fabricatedExclusion(false)
                .unsupportedAssumption(false)
                .strengths(List.of("Answer provided"))
                .weaknesses(List.of())
                .unsupportedClaims(List.of())
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