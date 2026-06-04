package com.example.Backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialCalculation {
    public enum CoverageStatus { COVERED, PARTIALLY_COVERED, NOT_COVERED, UNKNOWN }

    private CoverageStatus status;
    /** Actual amount the patient is presented with (e.g. ₹7,000 room rent). */
    private BigDecimal actualAmount;
    /** Policy's stated limit / cap (e.g. ₹5,000). */
    private BigDecimal limitAmount;
    /** Amount the insurer covers. */
    private BigDecimal coveredAmount;
    /** Amount the patient must pay out-of-pocket. */
    private BigDecimal patientPayable;
    /** Currency for all amounts. */
    private String currency;
    /** Unit (per day / per visit / total). */
    private String unit;
    /** Topic this calculation refers to (e.g. "Room Rent"). */
    private String topic;
    /** The exact policy clause that supplied the cap. */
    private String supportingClause;
    /** Diagnostic notes — explanation of how the calculation was derived. */
    private List<String> notes;
    private boolean valid;
    private String invalidReason;
}
