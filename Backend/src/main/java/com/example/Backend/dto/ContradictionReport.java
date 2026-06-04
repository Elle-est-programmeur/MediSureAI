package com.example.Backend.dto;

import com.example.Backend.model.ReasoningPath;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Per-candidate contradiction analysis. Produced by ContradictionDetectionService.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContradictionReport {
    private ReasoningPath path;
    /** True if the answer directly contradicts retrieved evidence. */
    private boolean contradicted;
    /** True if the answer asserts an exclusion not present in the evidence. */
    private boolean fabricatedExclusion;
    /** Concrete claim → counter-evidence pairs found. */
    private List<String> conflicts;
    /** Soft warnings (unsupported but not strictly contradicted). */
    private List<String> warnings;
    /** Severity in [0,1] — 0 clean, 1 hard contradiction. */
    private double severity;
}
