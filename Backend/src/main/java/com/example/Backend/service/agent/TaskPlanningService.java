package com.example.Backend.service.agent;

import com.example.Backend.dto.TaskPlan;
import com.example.Backend.model.AgentStep;
import com.example.Backend.model.QueryIntent;
import com.example.Backend.service.llm.LLMService;
import com.example.Backend.service.llm.PromptTemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskPlanningService {

    private final LLMService llmService;
    private final PromptTemplateService promptTemplateService;
    private final ObjectMapper objectMapper;

    private static final TaskPlan DEFAULT_PLAN = TaskPlan.builder()
            .steps(List.of(AgentStep.VECTOR_SEARCH, AgentStep.CONTEXT_BUILDING, AgentStep.LLM_REASONING))
            .parameters(Map.of("topK", 5))
            .reasoning("Default fallback plan")
            .build();

    /**
     * Asks GPT to produce a minimal task plan for the given query and intent.
     * Falls back to the default 3-step plan on any parsing failure.
     */
    public TaskPlan createPlan(String query, QueryIntent intent) {
        try {
            String prompt = promptTemplateService.buildTaskPlanningPrompt(query, intent);
            String raw = llmService.generateCompletion(prompt);
            String json = stripCodeFence(raw);

            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(json, Map.class);

            @SuppressWarnings("unchecked")
            List<String> stepStrings = (List<String>) parsed.get("steps");
            List<AgentStep> steps = stepStrings.stream().map(AgentStep::valueOf).toList();

            @SuppressWarnings("unchecked")
            Map<String, Object> parameters = parsed.containsKey("parameters")
                    ? (Map<String, Object>) parsed.get("parameters")
                    : new HashMap<>();

            String reasoning = (String) parsed.getOrDefault("reasoning", "");

            log.info("Task plan created: {} steps — {}", steps.size(), steps);
            return TaskPlan.builder()
                    .steps(steps)
                    .parameters(parameters)
                    .reasoning(reasoning)
                    .build();

        } catch (Exception ex) {
            log.warn("Task planning failed ({}), using default plan", ex.getMessage());
            return DEFAULT_PLAN;
        }
    }

    private String stripCodeFence(String raw) {
        String s = raw.strip();
        if (s.startsWith("```json")) s = s.substring(7);
        else if (s.startsWith("```"))  s = s.substring(3);
        if (s.endsWith("```")) s = s.substring(0, s.length() - 3);
        return s.strip();
    }
}