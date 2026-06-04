package com.example.Backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Verified pharmacological knowledge for a single drug.
 *
 * Sourced from medical_drug_reference.json. This DTO is the ONLY allowed
 * source of patient-facing drug claims — the LLM must not invent uses,
 * side effects, or warnings that are not present here.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DrugInfo {
    private String drugName;
    /** Brand names or alternate spellings that resolve to this entry. */
    private List<String> aliases;
    private String drugClass;
    private List<String> uses;
    private List<String> commonSideEffects;
    private List<String> seriousWarnings;
    private List<String> contraindications;
    private List<String> commonInteractions;
    private String patientFriendlyExplanation;
    private String evidenceSource;
}
