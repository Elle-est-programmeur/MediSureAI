package com.example.Backend.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration to resolve bean ambiguity and inject OpenRouter headers.
 * Uses a RestClientCustomizer to ensure all cloud LLM calls include mandatory headers.
 */
@Configuration
public class ModelConfig {

    @Bean
    public RestClientCustomizer openRouterCustomizer() {
        return restClientBuilder -> restClientBuilder
                .defaultHeader("HTTP-Referer", "https://github.com/vinayak-pol003/MediSureAI")
                .defaultHeader("X-Title", "MediSureAI");
    }

    @Bean
    @Primary
    public ChatModel primaryChatModel(@Qualifier("ollamaChatModel") ChatModel ollamaChatModel) {
        return ollamaChatModel;
    }

    @Bean
    @Primary
    public EmbeddingModel primaryEmbeddingModel(@Qualifier("ollamaEmbeddingModel") EmbeddingModel ollamaEmbeddingModel) {
        return ollamaEmbeddingModel;
    }
}
