package com.example.Backend.srlm;

import com.example.Backend.dto.FinancialCalculation;
import com.example.Backend.dto.RetrievedChunk;
import com.example.Backend.service.srlm.ConstraintReasoningService;
import com.example.Backend.service.srlm.FinancialCalculationValidator;
import com.example.Backend.service.srlm.MonetaryExtractor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConstraintReasoningServiceTest {

    private final ConstraintReasoningService service =
            new ConstraintReasoningService(new MonetaryExtractor());
    private final FinancialCalculationValidator validator = new FinancialCalculationValidator();

    private RetrievedChunk chunk(String id, String content) {
        return RetrievedChunk.builder().chunkId(id).content(content).finalScore(0.9).build();
    }

    /** Phase-18 critical test #2: ₹7,000 room rent with ₹5,000 cap → ₹2,000 payable. */
    @Test
    void roomRentCapComputesPayable() {
        FinancialCalculation calc = service.reason(
                "I had a ₹7,000 room — is it fully covered?",
                List.of(chunk("c1", "Room Rent Limit: Up to ₹5,000 per day.")));
        validator.validate(calc);

        assertThat(calc.getStatus()).isEqualTo(FinancialCalculation.CoverageStatus.PARTIALLY_COVERED);
        assertThat(calc.getActualAmount()).isEqualByComparingTo(new BigDecimal("7000"));
        assertThat(calc.getLimitAmount()).isEqualByComparingTo(new BigDecimal("5000"));
        assertThat(calc.getCoveredAmount()).isEqualByComparingTo(new BigDecimal("5000"));
        assertThat(calc.getPatientPayable()).isEqualByComparingTo(new BigDecimal("2000"));
        assertThat(calc.isValid()).isTrue();
    }

    @Test
    void actualWithinLimitIsFullyCovered() {
        FinancialCalculation calc = service.reason(
                "I had a ₹3,000 room — is it covered?",
                List.of(chunk("c1", "Room Rent Limit: Up to ₹5,000 per day.")));
        validator.validate(calc);

        assertThat(calc.getStatus()).isEqualTo(FinancialCalculation.CoverageStatus.COVERED);
        assertThat(calc.getPatientPayable()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(calc.isValid()).isTrue();
    }

    @Test
    void noActualReturnsCapDescription() {
        FinancialCalculation calc = service.reason(
                "What is the room rent limit?",
                List.of(chunk("c1", "Room Rent Limit: Up to ₹5,000 per day.")));
        validator.validate(calc);

        // Cap-only response is partially covered with limit set
        assertThat(calc.getLimitAmount()).isEqualByComparingTo(new BigDecimal("5000"));
        assertThat(calc.isValid()).isTrue();
    }

    @Test
    void noAmountsAtAllReturnsUnknownNotInvalid() {
        FinancialCalculation calc = service.reason(
                "Is ICU covered?",
                List.of(chunk("c1", "ICU charges are covered up to the sum insured.")));
        validator.validate(calc);

        assertThat(calc.getStatus()).isEqualTo(FinancialCalculation.CoverageStatus.UNKNOWN);
        assertThat(calc.isValid()).isTrue();   // UNKNOWN must NOT trigger validator rejection
    }
}
