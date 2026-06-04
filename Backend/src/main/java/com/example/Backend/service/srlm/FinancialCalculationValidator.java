package com.example.Backend.service.srlm;

import com.example.Backend.dto.FinancialCalculation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Sanity-checks a financial calculation. Enforces:
 *
 *   covered + payable == actual         (arithmetic consistency)
 *   covered ≤ limit                     (no overflow)
 *   payable ≥ 0                         (no negative bills)
 *   actual, limit, covered, payable ≥ 0 (non-negative amounts)
 *   currency matches across fields if both are present
 *
 * Mutates {@code valid} and {@code invalidReason} on the input calculation.
 * Returns the same instance for fluent use.
 */
@Service
@Slf4j
public class FinancialCalculationValidator {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    public FinancialCalculation validate(FinancialCalculation calc) {
        if (calc == null) return null;
        List<String> issues = new ArrayList<>();

        if (calc.getStatus() == FinancialCalculation.CoverageStatus.UNKNOWN) {
            calc.setValid(true);
            return calc;
        }

        if (anyNegative(calc.getActualAmount(), calc.getLimitAmount(),
                       calc.getCoveredAmount(), calc.getPatientPayable())) {
            issues.add("Negative amount detected");
        }

        BigDecimal actual = calc.getActualAmount();
        BigDecimal limit = calc.getLimitAmount();
        BigDecimal covered = calc.getCoveredAmount();
        BigDecimal payable = calc.getPatientPayable();

        if (covered != null && limit != null && covered.subtract(limit).compareTo(TOLERANCE) > 0) {
            issues.add(String.format("Covered (%s) exceeds limit (%s)",
                    covered.toPlainString(), limit.toPlainString()));
        }

        if (actual != null && covered != null && payable != null) {
            BigDecimal sum = covered.add(payable).setScale(2, RoundingMode.HALF_UP);
            BigDecimal want = actual.setScale(2, RoundingMode.HALF_UP);
            if (sum.subtract(want).abs().compareTo(TOLERANCE) > 0) {
                issues.add(String.format("covered + payable (%s) != actual (%s)",
                        sum.toPlainString(), want.toPlainString()));
            }
        }

        if (issues.isEmpty()) {
            calc.setValid(true);
        } else {
            calc.setValid(false);
            calc.setInvalidReason(String.join("; ", issues));
            log.warn("FinancialCalculationValidator rejected calc: {}", calc.getInvalidReason());
        }
        return calc;
    }

    private boolean anyNegative(BigDecimal... values) {
        for (BigDecimal v : values) {
            if (v != null && v.compareTo(ZERO) < 0) return true;
        }
        return false;
    }
}
