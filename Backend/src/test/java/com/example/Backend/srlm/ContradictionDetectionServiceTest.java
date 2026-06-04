package com.example.Backend.srlm;

import com.example.Backend.dto.ContradictionReport;
import com.example.Backend.dto.ReasoningCandidate;
import com.example.Backend.dto.RetrievedChunk;
import com.example.Backend.model.ReasoningPath;
import com.example.Backend.service.srlm.ContradictionDetectionService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for the canonical SRLM failure mode:
 *   Evidence says ICU is covered → answer must not say it is not.
 */
class ContradictionDetectionServiceTest {

    private final ContradictionDetectionService service = new ContradictionDetectionService();

    private RetrievedChunk chunk(String id, String content) {
        return RetrievedChunk.builder().chunkId(id).content(content).finalScore(0.9).build();
    }

    private ReasoningCandidate candidate(String answer) {
        return ReasoningCandidate.builder()
                .path(ReasoningPath.POLICY_FOCUSED)
                .approach("test")
                .reasoning("test")
                .answer(answer)
                .build();
    }

    @Test
    void detectsDirectContradictionOnIcuCoverage() {
        var evidence = List.of(chunk("c1", "ICU Charges: Covered up to sum insured."));
        var cand = candidate("ICU treatment is not covered under your policy.");
        ContradictionReport r = service.detect(cand, "Is ICU treatment covered?", evidence);

        assertThat(r.isContradicted()).isTrue();
        assertThat(r.getSeverity()).isGreaterThanOrEqualTo(0.8);
        assertThat(r.getConflicts()).anyMatch(s -> s.toLowerCase().contains("icu"));
    }

    @Test
    void detectsFabricatedExclusionWhenEvidenceIsSilent() {
        var evidence = List.of(chunk("c1", "The policyholder must submit claim form within 30 days."));
        var cand = candidate("Maternity is excluded from your policy.");
        ContradictionReport r = service.detect(cand, "Is maternity covered?", evidence);

        assertThat(r.isFabricatedExclusion()).isTrue();
        assertThat(r.getSeverity()).isGreaterThanOrEqualTo(0.7);
    }

    @Test
    void doesNotFlagWhenAnswerAgreesWithEvidence() {
        var evidence = List.of(chunk("c1", "ICU Charges: Covered up to sum insured."));
        var cand = candidate("Yes, ICU charges are covered up to the sum insured amount.");
        ContradictionReport r = service.detect(cand, "Is ICU treatment covered?", evidence);

        assertThat(r.isContradicted()).isFalse();
        assertThat(r.isFabricatedExclusion()).isFalse();
        assertThat(r.getSeverity()).isLessThan(0.5);
    }

    @Test
    void flagsAffirmativeClaimWithSilentEvidenceAsWarning() {
        var evidence = List.of(chunk("c1", "ICU Charges: Covered up to sum insured."));
        var cand = candidate("Maternity is covered under your policy.");
        ContradictionReport r = service.detect(cand, "What about maternity?", evidence);

        // Maternity is silent in evidence, so this is an unsupported claim warning
        assertThat(r.getWarnings()).isNotEmpty();
    }
}
