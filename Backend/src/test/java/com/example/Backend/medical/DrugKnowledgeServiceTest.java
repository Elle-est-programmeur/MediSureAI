package com.example.Backend.medical;

import com.example.Backend.dto.DrugInfo;
import com.example.Backend.service.medical.DrugKnowledgeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DrugKnowledgeServiceTest {

    private DrugKnowledgeService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new DrugKnowledgeService(new ObjectMapper());
        // PostConstruct doesn't run for plain `new` — invoke explicitly
        var load = DrugKnowledgeService.class.getDeclaredMethod("load");
        load.setAccessible(true);
        load.invoke(service);
        ReflectionTestUtils.invokeMethod(service, "buildMentionPattern");
    }

    @Test
    void loadsAllSeedDrugs() {
        // 7 entries in medical_drug_reference.json
        assertThat(service.size()).isGreaterThanOrEqualTo(7);
    }

    @Test
    void canonicalLookupSucceeds() {
        Optional<DrugInfo> info = service.lookup("Cetirizine");
        assertThat(info).isPresent();
        assertThat(info.get().getCommonSideEffects()).contains("Drowsiness");
    }

    @Test
    void brandNameResolvesToCanonical() {
        Optional<DrugInfo> a = service.lookup("Dolo");
        Optional<DrugInfo> b = service.lookup("Crocin");
        assertThat(a).isPresent();
        assertThat(b).isPresent();
        assertThat(a.get().getDrugName()).isEqualTo("Paracetamol");
        assertThat(b.get().getDrugName()).isEqualTo("Paracetamol");
    }

    @Test
    void caseInsensitiveLookup() {
        assertThat(service.lookup("cetirizine")).isPresent();
        assertThat(service.lookup("CETIRIZINE")).isPresent();
        assertThat(service.lookup("  Cetirizine  ")).isPresent();
    }

    @Test
    void unknownDrugReturnsEmpty() {
        assertThat(service.lookup("Unobtainium")).isEmpty();
    }

    @Test
    void extractMentionedDrugsFindsAllAndDedups() {
        String text = "Patient is on Cetirizine 10mg and Pantoprazole 40mg. Took Dolo for fever.";
        List<DrugInfo> hits = service.extractMentionedDrugs(text);
        // Cetirizine, Pantoprazole, Dolo (=Paracetamol) — but Dolo dedups with itself
        assertThat(hits).extracting(DrugInfo::getDrugName)
                .containsExactly("Cetirizine", "Pantoprazole", "Paracetamol");
    }
}
