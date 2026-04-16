package com.example.Backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAI configuration placeholder.
 * The project now uses Spring AI + Ollama for both embeddings and LLM completions.
 * This class is kept to avoid property binding errors on existing application.properties keys.
 */
@Configuration
@Slf4j
public class OpenAIConfig {

    @Value("${openai.api.key:not-configured}")
    private String apiKey;

    // No beans here — Spring AI auto-configures Ollama ChatModel and EmbeddingModel
    // from the spring.ai.ollama.* properties in application.properties.
}