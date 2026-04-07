package com.example.Backend.service.llm;

import com.example.Backend.exception.LLMException;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LLMService {

    private static final String SYSTEM_ROLE = "system";
    private static final String USER_ROLE = "user";
    private static final String SYSTEM_PROMPT =
            "You are a helpful AI assistant specialized in healthcare insurance. " +
            "Provide accurate, grounded answers based only on the information given to you.";

    private final OpenAiService openAiService;

    @Value("${openai.model.completion}")
    private String completionModel;

    @Value("${openai.model.temperature:0.3}")
    private double temperature;

    @Value("${openai.model.max-tokens:1000}")
    private int maxTokens;

    /**
     * Sends a single user prompt to GPT with a fixed system message.
     * Retried up to 3 times with exponential back-off on any failure.
     */
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public String generateCompletion(String prompt) {
        return generateCompletionWithMessages(List.of(
                new ChatMessage(SYSTEM_ROLE, SYSTEM_PROMPT),
                new ChatMessage(USER_ROLE, prompt)
        ));
    }

    /**
     * Generates a completion with a custom temperature.
     * Used by SRLM's MultiReasoningService (temperature=0.7) to produce
     * diverse reasoning paths, while keeping structured prompts at 0.3.
     */
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public String generateCompletionWithTemperature(String prompt, double customTemperature) {
        try {
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(completionModel)
                    .messages(List.of(
                            new ChatMessage(SYSTEM_ROLE, SYSTEM_PROMPT),
                            new ChatMessage(USER_ROLE, prompt)
                    ))
                    .temperature(customTemperature)
                    .maxTokens(maxTokens)
                    .build();

            String response = openAiService.createChatCompletion(request)
                    .getChoices().get(0).getMessage().getContent();

            log.debug("LLM response (temp={}): {} chars", customTemperature, response.length());
            return response;

        } catch (LLMException e) {
            throw e;
        } catch (Exception e) {
            log.error("LLM call failed: {}", e.getMessage());
            throw new LLMException("LLM completion failed: " + e.getMessage(), e);
        }
    }

    /**
     * Sends an arbitrary message list (system + user + assistant turns).
     * Useful for multi-turn reasoning in Phase 5.
     */
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public String generateCompletionWithMessages(List<ChatMessage> messages) {
        try {
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(completionModel)
                    .messages(messages)
                    .temperature(temperature)
                    .maxTokens(maxTokens)
                    .build();

            String response = openAiService.createChatCompletion(request)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();

            log.debug("LLM response: {} chars", response.length());
            return response;

        } catch (LLMException e) {
            throw e;
        } catch (Exception e) {
            log.error("LLM call failed: {}", e.getMessage());
            throw new LLMException("LLM completion failed: " + e.getMessage(), e);
        }
    }
}