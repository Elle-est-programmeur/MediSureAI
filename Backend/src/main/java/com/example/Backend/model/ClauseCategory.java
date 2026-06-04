package com.example.Backend.model;

/**
 * Semantic class of a policy clause. Used by the SRLM pipeline to bias
 * reasoning paths (e.g. "what are the limitations?" must NOT return BENEFIT
 * clauses) and to build the structured answer's reasoningCategory.
 */
public enum ClauseCategory {
    COVERAGE,           // affirmative coverage statements
    BENEFIT,            // value-adds, add-on benefits
    LIMITATION,         // numerical / functional caps that still allow coverage
    FINANCIAL_LIMIT,    // explicit monetary caps (sub-class of LIMITATION)
    EXCLUSION,          // explicit denials / non-payable items
    WAITING_PERIOD,     // time-bound restrictions before coverage applies
    CLAIM_PROCESS,      // how to claim, documentation, process steps
    GENERAL_INFO        // ambient text that classifies nothing else
}
