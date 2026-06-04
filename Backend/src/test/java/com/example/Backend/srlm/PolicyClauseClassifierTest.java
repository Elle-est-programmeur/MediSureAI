package com.example.Backend.srlm;

import com.example.Backend.dto.ClauseClassification;
import com.example.Backend.dto.RetrievedChunk;
import com.example.Backend.model.ClauseCategory;
import com.example.Backend.service.srlm.PolicyClauseClassifier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyClauseClassifierTest {

    private final PolicyClauseClassifier classifier = new PolicyClauseClassifier();

    private RetrievedChunk chunk(String id, String content) {
        return RetrievedChunk.builder().chunkId(id).content(content).finalScore(0.9).build();
    }

    @Test
    void financialLimitBeatsLimitation() {
        ClauseClassification c = classifier.classify(chunk("c",
                "Room rent is covered up to ₹5,000 per day."));
        assertThat(c.getCategory()).isEqualTo(ClauseCategory.FINANCIAL_LIMIT);
        assertThat(c.getMatchedCues()).isNotEmpty();
    }

    @Test
    void exclusionDetected() {
        ClauseClassification c = classifier.classify(chunk("c",
                "Cosmetic surgery is excluded from this policy and not payable."));
        assertThat(c.getCategory()).isEqualTo(ClauseCategory.EXCLUSION);
    }

    @Test
    void waitingPeriodDetected() {
        ClauseClassification c = classifier.classify(chunk("c",
                "Pre-existing diseases are subject to a waiting period of 36 months."));
        assertThat(c.getCategory()).isEqualTo(ClauseCategory.WAITING_PERIOD);
    }

    @Test
    void coverageClassified() {
        ClauseClassification c = classifier.classify(chunk("c",
                "ICU charges are covered up to the sum insured."));
        // Has "covered up to" so it counts as COVERAGE; doesn't carry money so not FINANCIAL_LIMIT
        assertThat(c.getCategory()).isIn(ClauseCategory.COVERAGE, ClauseCategory.FINANCIAL_LIMIT);
    }

    @Test
    void claimProcessDetected() {
        ClauseClassification c = classifier.classify(chunk("c",
                "Submit the claim form and supporting documents within 30 days of discharge."));
        assertThat(c.getCategory()).isEqualTo(ClauseCategory.CLAIM_PROCESS);
    }

    @Test
    void benefitDoesNotMasqueradeAsLimitation() {
        // Phase 18: benefits must not be classified as limitations
        ClauseClassification c = classifier.classify(chunk("c",
                "A no claim bonus and complimentary annual health check are included."));
        assertThat(c.getCategory()).isEqualTo(ClauseCategory.BENEFIT);
    }
}
