package com.example.Backend.srlm;

import com.example.Backend.dto.RetrievedChunk;
import com.example.Backend.service.srlm.RetrievalRerankerService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RetrievalRerankerServiceTest {

    private RetrievalRerankerService newService() {
        RetrievalRerankerService s = new RetrievalRerankerService();
        ReflectionTestUtils.setField(s, "directMatchBoost", 0.15);
        ReflectionTestUtils.setField(s, "cueBoost", 0.08);
        ReflectionTestUtils.setField(s, "minContentChars", 60);
        return s;
    }

    private RetrievedChunk chunk(String id, String content, double base) {
        return RetrievedChunk.builder()
                .chunkId(id)
                .content(content)
                .embeddingSimilarity(base)
                .keywordScore(0.5)
                .finalScore(base)
                .matchedKeywords(List.of())
                .build();
    }

    @Test
    void directIcuClauseFloatsToTop() {
        RetrievalRerankerService s = newService();
        List<RetrievedChunk> chunks = new ArrayList<>(List.of(
                chunk("daycare", "Daycare procedures listed in Annexure A are not covered.", 0.70),
                chunk("icu",      "ICU Charges: Covered up to sum insured amount.",            0.60),
                chunk("opd",      "OPD consultations are available at network hospitals.",    0.55)
        ));
        List<RetrievedChunk> reranked = s.rerank("Is ICU treatment fully covered?", chunks);

        assertThat(reranked.get(0).getChunkId()).isEqualTo("icu");
    }
}
