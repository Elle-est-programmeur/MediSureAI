package com.example.Backend.srlm;

import com.example.Backend.dto.RetrievedChunk;
import com.example.Backend.service.srlm.ContextValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContextValidationServiceTest {

    private ContextValidationService newService() {
        ContextValidationService s = new ContextValidationService();
        ReflectionTestUtils.setField(s, "minFinalScore", 0.45);
        ReflectionTestUtils.setField(s, "minContextChars", 120);
        return s;
    }

    private RetrievedChunk chunk(String content, double score) {
        return RetrievedChunk.builder().chunkId("c").content(content).finalScore(score).build();
    }

    @Test
    void emptyChunksTriggersRetry() {
        ContextValidationService.Result r = newService().validate("Is ICU covered?", List.of());
        assertThat(r.valid()).isFalse();
        assertThat(r.shouldRetry()).isTrue();
    }

    @Test
    void missingTopicTriggersRetry() {
        ContextValidationService.Result r = newService().validate(
                "Is ICU covered?",
                List.of(chunk("OPD consultations are at network hospitals. " +
                        "Pre-existing diseases are subject to a waiting period.", 0.9)));
        assertThat(r.valid()).isFalse();
        assertThat(r.shouldRetry()).isTrue();
        assertThat(r.missingEntities()).contains("ICU");
    }

    @Test
    void icuTopicWithStrongScoreIsValid() {
        ContextValidationService.Result r = newService().validate(
                "Is ICU treatment fully covered?",
                List.of(chunk(
                        "Section 4.2 ICU Charges: Covered up to the sum insured amount " +
                        "specified in the policy schedule. Coverage applies to all network " +
                        "and non-network hospitals subject to claim documentation and " +
                        "pre-authorisation requirements stated in Section 6.", 0.8)));
        assertThat(r.valid()).isTrue();
        assertThat(r.shouldRetry()).isFalse();
    }
}
