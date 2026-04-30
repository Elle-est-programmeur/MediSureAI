package com.example.Backend.service.agent;

import com.example.Backend.dto.IntentDetectionResult;
import com.example.Backend.model.QueryIntent;
import com.example.Backend.service.llm.CritiqueLLMService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Intelligent intent detection driven by LLM.
 *
 * Restored to full spec: Uses GPT-4o-mini (via OpenRouter) to precisely categorize
 * the user's medical or insurance inquiry into one of the specialized intents.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntentDetectionService {

    private final CritiqueLLMService llmService;
    private final ObjectMapper objectMapper;

    /**
     * Detects intent using LLM reasoning.
     */
    public IntentDetectionResult detectIntent(String query) {
        log.info("Detecting intent for query: '{}'", query);

        try {
            String prompt = buildIntentPrompt(query);
            String response = llmService.generateCritique(prompt);
            
            return parseIntentResponse(response);
        } catch (Exception e) {
            log.warn("LLM intent detection failed, falling back to GENERAL_INFO: {}", e.getMessage());
            return IntentDetectionResult.builder()
                    .intent(QueryIntent.GENERAL_INFO)
                    .confidence(0.5)
                    .reasoning("Fallback due to error: " + e.getMessage())
                    .build();
        }
    }

    private String buildIntentPrompt(String query) {
        String intents = Stream.of(QueryIntent.values())
                .filter(i -> i != QueryIntent.UNKNOWN)
                .map(Enum::name)
                .collect(Collectors.joining(", "));

        return String.format("""
                Categorize the user's healthcare query into exactly ONE of these intents: [%s].

                Intent guide:
                - TREATMENT_EXPLANATION: user asks to explain / summarize / clarify their OWN treatment plan,
                  prescribed drugs, dosing, what they should do next, or what their doctor told them.
                  Examples: "explain my treatment plan", "what are my prescribed medicines for",
                  "what should I do for my diagnosis", "summarize my treatment".
                - MEDICAL_INTERPRETATION: user asks what a generic medical term/diagnosis means
                  (not necessarily theirs). Example: "what is hypertension".
                - COVERAGE_QUESTION / POLICY_DETAILS / CLAIM_STATUS / BILLING_INQUIRY: insurance topics.
                - GENERAL_INFO: general healthcare questions not specific to the patient's records.

                USER QUERY: "%s"

                Return ONLY valid JSON:
                {
                  "intent": "INTENT_NAME",
                  "confidence": 0.0-1.0,
                  "reasoning": "brief explanation"
                }
                """, intents, query);
    }

    private IntentDetectionResult parseIntentResponse(String response) {
        try {
            String json = stripCodeFence(response);
            JsonNode node = objectMapper.readTree(json);
            
            String intentStr = node.path("intent").asText("GENERAL_INFO");
            QueryIntent intent;
            try {
                intent = QueryIntent.valueOf(intentStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                intent = QueryIntent.GENERAL_INFO;
            }

            return IntentDetectionResult.builder()
                    .intent(intent)
                    .confidence(node.path("confidence").asDouble(0.8))
                    .reasoning(node.path("reasoning").asText("LLM analyzed query"))
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse intent JSON: {}", response);
            throw new RuntimeException("Intent parsing failed", e);
        }
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