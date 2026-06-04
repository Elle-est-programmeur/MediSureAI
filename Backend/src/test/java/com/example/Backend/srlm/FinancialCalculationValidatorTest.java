package com.example.Backend.srlm;

import com.example.Backend.dto.FinancialCalculation;
import com.example.Backend.service.srlm.FinancialCalculationValidator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialCalculationValidatorTest {

    private final FinancialCalculationValidator validator = new FinancialCalculationValidator();

    @Test
    void rejectsInconsistentArithmetic() {
        FinancialCalculation bad = FinancialCalculation.builder()
                .status(FinancialCalculation.CoverageStatus.PARTIALLY_COVERED)
                .actualAmount(new BigDecimal("7000"))
                .limitAmount(new BigDecimal("5000"))
                .coveredAmount(new BigDecimal("5000"))
                .patientPayable(new BigDecimal("3000")) // wrong: 5000+3000=8000≠7000
                .currency("INR")
                .build();
        validator.validate(bad);
        assertThat(bad.isValid()).isFalse();
        assertThat(bad.getInvalidReason()).contains("!= actual");
    }

    @Test
    void rejectsCoveredOverLimit() {
        FinancialCalculation bad = FinancialCalculation.builder()
                .status(FinancialCalculation.CoverageStatus.PARTIALLY_COVERED)
                .actualAmount(new BigDecimal("7000"))
                .limitAmount(new BigDecimal("5000"))
                .coveredAmount(new BigDecimal("6000")) // wrong: covered > limit
                .patientPayable(new BigDecimal("1000"))
                .build();
        validator.validate(bad);
        assertThat(bad.isValid()).isFalse();
    }

    @Test
    void rejectsNegativeAmount() {
        FinancialCalculation bad = FinancialCalculation.builder()
                .status(FinancialCalculation.CoverageStatus.PARTIALLY_COVERED)
                .actualAmount(new BigDecimal("7000"))
                .limitAmount(new BigDecimal("5000"))
                .coveredAmount(new BigDecimal("5000"))
                .patientPayable(new BigDecimal("-100"))
                .build();
        validator.validate(bad);
        assertThat(bad.isValid()).isFalse();
    }

    @Test
    void acceptsConsistentCalculation() {
        FinancialCalculation good = FinancialCalculation.builder()
                .status(FinancialCalculation.CoverageStatus.PARTIALLY_COVERED)
                .actualAmount(new BigDecimal("7000"))
                .limitAmount(new BigDecimal("5000"))
                .coveredAmount(new BigDecimal("5000"))
                .patientPayable(new BigDecimal("2000"))
                .build();
        validator.validate(good);
        assertThat(good.isValid()).isTrue();
    }
}
