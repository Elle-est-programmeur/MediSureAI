package com.example.Backend.model;

public enum ReasoningPath {
    POLICY_FOCUSED,
    COVERAGE_FOCUSED,
    BALANCED,
    CLAIM_ORIENTED,
    PATIENT_BENEFIT,

    // Treatment / medical-interpretation focused (use PATIENT_DATA context, not policy)
    TREATMENT_PLAIN,    // restate plan in plain English
    TREATMENT_DRUGS,    // emphasize drug regimen, dosing, purpose
    TREATMENT_NEXT_STEPS // emphasize what the patient should do
}