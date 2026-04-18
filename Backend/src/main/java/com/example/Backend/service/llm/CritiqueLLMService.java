package com.example.Backend.service.llm;

import com.example.Backend.exception.LLMException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Specialized Cloud LLM service via OpenRouter (OpenAI-compatible).
 * Used specifically for the "Brain" phases of SRLM: Reflection, Scoring, and Synthesis.
 */
@Service
@Slf4j
public class CritiqueLLMService {

    private final ChatClient chatClient;
    private final LLMService localLlmService;

    public CritiqueLLMService(@Qualifier("openAiChatModel") ChatModel chatModel, LLMService localLlmService) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("You are a high-precision medical insurance auditor. " +
                        "Evaluate arguments based strictly on the provided context.")
                .build();
        this.localLlmService = localLlmService;
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public String generateCritique(String prompt) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("Starting cloud critique (timeout=60s)...");
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            log.info("Cloud critique succeeded in {}ms", (System.currentTimeMillis() - startTime));
            return response != null ? response : "";
        } catch (Throwable e) {
            log.warn("Cloud Critique/Orchestration failed after {}ms: {}. Falling back to local Ollama.", 
                    (System.currentTimeMillis() - startTime), e.getMessage());
            try {
                // Fallback to local model to ensure the SRLM pipeline completes
                String localResponse = localLlmService.generateCompletion(prompt);
                log.info("Local fallback critique succeeded.");
                return localResponse;
            } catch (Exception localEx) {
                log.error("CRITICAL: Both cloud and local critique models failed: {}", localEx.getMessage());
                return "[Error: Analysis component failed. Detailed reasoning results unavailable.]";
            }
        }
    }
}
