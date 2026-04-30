package com.example.Backend.model;

public enum QueryIntent {
    COVERAGE_QUESTION,       // "What does my insurance cover?"
    CLAIM_STATUS,            // "What's the status of my claim?"
    POLICY_DETAILS,          // "What's my deductible?"
    MEDICAL_INTERPRETATION,  // "What does this diagnosis mean?"
    TREATMENT_EXPLANATION,   // "Explain my treatment plan / what should I do?"
    BILLING_INQUIRY,         // "Why was I charged this amount?"
    GENERAL_INFO,            // General healthcare/insurance info
    UNKNOWN                  // Cannot determine intent
}