package com.example.Backend.config;

import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class OpenAIConfig {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.timeout:60000}")
    private long timeoutMs;

    @Bean
    public OpenAiService openAiService() {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("sk-proj-your")) {
            log.warn("OpenAI API key is not configured. Set the OPENAI_API_KEY environment variable.");
            log.warn("Embedding generation will fail until a valid API key is provided.");
        } else {
            log.info("OpenAI API configured (model=text-embedding-3-small, timeout={}ms)", timeoutMs);
        }
        return new OpenAiService(apiKey, Duration.ofMillis(timeoutMs));
    }
}