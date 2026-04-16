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

    public CritiqueLLMService(@Qualifier("openAiChatModel") ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("You are a high-precision medical insurance auditor. " +
                        "Evaluate arguments based strictly on the provided context.")
                .build();
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public String generateCritique(String prompt) {
        try {
            log.debug("Sending critique request to OpenRouter (GPT-4o-mini)...");
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            return response != null ? response : "";
        } catch (Exception e) {
            log.error("Cloud Critique failed: {}", e.getMessage());
            throw new LLMException("Cloud critique failed: " + e.getMessage(), e);
        }
    }
}
