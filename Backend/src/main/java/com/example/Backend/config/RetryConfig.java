package com.example.Backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableRetry
public class RetryConfig {
    // Activates @Retryable / @Recover annotations across all Spring beans
}