package com.example.Backend.model;

public enum QueryIntent {
    COVERAGE_QUESTION,       // "What does my insurance cover?"
    CLAIM_STATUS,            // "What's the status of my claim?"
    POLICY_DETAILS,          // "What's my deductible?"
    MEDICAL_INTERPRETATION,  // "What does this diagnosis mean?"
    BILLING_INQUIRY,         // "Why was I charged this amount?"
    GENERAL_INFO,            // General healthcare/insurance info
    UNKNOWN                  // Cannot determine intent
}