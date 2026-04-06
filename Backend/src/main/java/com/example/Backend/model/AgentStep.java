package com.example.Backend.model;

public enum AgentStep {
    INTENT_DETECTION,
    TASK_PLANNING,
    VECTOR_SEARCH,
    SQL_QUERY,
    CONTEXT_BUILDING,
    LLM_REASONING,
    RESPONSE_FORMATTING
}