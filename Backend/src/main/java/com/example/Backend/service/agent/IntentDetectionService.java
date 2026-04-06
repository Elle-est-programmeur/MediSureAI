package com.example.Backend.service.agent;

import com.example.Backend.dto.IntentDetectionResult;
import com.example.Backend.model.QueryIntent;
import com.example.Backend.service.llm.LLMService;
import com.example.Backend.service.llm.PromptTemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntentDetectionService {

    private final LLMService llmService;
    private final PromptTemplateService promptTemplateService;
    private final ObjectMapper objectMapper;

    /**
     * Classifies a user query into a {@link QueryIntent} using GPT.
     * Falls back to UNKNOWN on any parsing or API failure so the pipeline
     * can still attempt a vector search and return a best-effort answer.
     */
    public IntentDetectionResult detectIntent(String query) {
        try {
            String prompt = promptTemplateService.buildIntentDetectionPrompt(query);
            String raw = llmService.generateCompletion(prompt);

            String json = stripCodeFence(raw);

            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(json, Map.class);

            QueryIntent intent = QueryIntent.valueOf((String) parsed.get("intent"));
            double confidence = ((Number) parsed.get("confidence")).doubleValue();
            String reasoning = (String) parsed.get("reasoning");

            log.info("Intent detected: {} (confidence={})", intent, confidence);
            return IntentDetectionResult.builder()
                    .intent(intent)
                    .confidence(confidence)
                    .reasoning(reasoning)
                    .build();

        } catch (Exception ex) {
            log.warn("Intent detection failed ({}), defaulting to UNKNOWN", ex.getMessage());
            return IntentDetectionResult.builder()
                    .intent(QueryIntent.UNKNOWN)
                    .confidence(0.0)
                    .reasoning("Detection failed: " + ex.getMessage())
                    .build();
        }
    }

    /** Strips optional ```json … ``` fences that GPT sometimes adds. */
    private String stripCodeFence(String raw) {
        String s = raw.strip();
        if (s.startsWith("```json")) s = s.substring(7);
        else if (s.startsWith("```"))  s = s.substring(3);
        if (s.endsWith("```")) s = s.substring(0, s.length() - 3);
        return s.strip();
    }
}