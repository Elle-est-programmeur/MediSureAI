package com.example.Backend.model;

public enum ReasoningPath {
    POLICY_FOCUSED,
    COVERAGE_FOCUSED,
    BALANCED,
    CLAIM_ORIENTED,
    PATIENT_BENEFIT,
    FINANCIAL_FOCUSED,   // emphasizes monetary limits, caps, payable amounts
    RISK_AWARE,          // emphasizes exclusions, conditions, waiting periods

    // Treatment / medical-interpretation focused (use PATIENT_DATA context, not policy)
    TREATMENT_PLAIN,
    TREATMENT_DRUGS,
    TREATMENT_NEXT_STEPS
}