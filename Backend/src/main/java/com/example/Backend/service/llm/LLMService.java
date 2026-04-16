package com.example.Backend.service.llm;

import com.example.Backend.exception.LLMException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Foundation LLMService backed by local Ollama.
 * Used for the initial reasoning paths and bulk text generation to save costs.
 */
@Service
@Slf4j
public class LLMService {

    @Value("${srlm.reasoning.max-tokens:400}")
    private int maxTokens;

    private final ChatClient chatClient;

    public LLMService(@Qualifier("ollamaChatModel") ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("You are a medical insurance assistant. " +
                        "Generate detailed reasoning paths based on the provided context.")
                .build();
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public String generateCompletion(String prompt) {
        return generateCompletionWithTemperature(prompt, 0.3);
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public String generateCompletionWithTemperature(String prompt, double temperature) {
        try {
            log.debug("Generating local reasoning with Ollama (temp={})...", temperature);
            return chatClient.prompt()
                    .user(prompt)
                    .options(OllamaOptions.builder()
                            .withTemperature(temperature)
                            .withNumPredict(maxTokens) // Soft limit to prevent rambling
                            .build())
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Local Ollama reasoning failed: {}", e.getMessage());
            throw new LLMException("Local reasoning failed: " + e.getMessage(), e);
        }
    }
}