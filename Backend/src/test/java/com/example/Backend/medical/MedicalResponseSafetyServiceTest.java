package com.example.Backend.medical;

import com.example.Backend.dto.DrugInfo;
import com.example.Backend.service.medical.DrugKnowledgeService;
import com.example.Backend.service.medical.MedicalResponseSafetyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MedicalResponseSafetyServiceTest {

    private DrugKnowledgeService drugKnowledgeService;
    private MedicalResponseSafetyService safety;

    @BeforeEach
    void setUp() throws Exception {
        drugKnowledgeService = new DrugKnowledgeService(new ObjectMapper());
        var load = DrugKnowledgeService.class.getDeclaredMethod("load");
        load.setAccessible(true);
        load.invoke(drugKnowledgeService);
        ReflectionTestUtils.invokeMethod(drugKnowledgeService, "buildMentionPattern");
        safety = new MedicalResponseSafetyService(drugKnowledgeService);
    }

    /** Phase-13 critical test #1: Cetirizine must not claim heart rate / blood pressure effects. */
    @Test
    void blocksUnsupportedCetirizineHeartRateClaim() {
        DrugInfo cetirizine = drugKnowledgeService.lookup("Cetirizine").orElseThrow();
        String draft = "Cetirizine helps with allergies. High doses may increase heart rate and blood pressure.";
        MedicalResponseSafetyService.Result r = safety.validate(draft, List.of(cetirizine));
        assertThat(r.sanitized()).isTrue();
        assertThat(r.issues()).isNotEmpty();
        assertThat(r.safeAnswer()).contains("Safety note");
        assertThat(r.safeAnswer()).contains("consult your doctor or pharmacist");
    }

    /** Phase-13 critical test #2: Paracetamol must not be described as treating dehydration. */
    @Test
    void blocksParacetamolDehydrationClaim() {
        DrugInfo para = drugKnowledgeService.lookup("Paracetamol").orElseThrow();
        String draft = "Paracetamol reduces fever and is also used to treat dehydration.";
        MedicalResponseSafetyService.Result r = safety.validate(draft, List.of(para));
        assertThat(r.sanitized()).isTrue();
        assertThat(r.issues())
                .anyMatch(i -> i.getClaim().toLowerCase().contains("dehydration"));
    }

    @Test
    void verifiedCetirizineFactsAreNotFlagged() {
        DrugInfo cetirizine = drugKnowledgeService.lookup("Cetirizine").orElseThrow();
        String draft = "Cetirizine helps relieve allergy symptoms. The most common side effects are drowsiness and dry mouth.";
        MedicalResponseSafetyService.Result r = safety.validate(draft, List.of(cetirizine));
        assertThat(r.sanitized()).isFalse();
        assertThat(r.issues()).isEmpty();
    }

    /** Phase-13 critical test #3: "Can I stop my medication?" must redirect to the doctor. */
    @Test
    void prescribingDirectiveRedirectsToDoctor() {
        String answer = "Stopping your medication early can cause symptoms to return.";
        MedicalResponseSafetyService.DirectiveResult r =
                safety.handlePrescribingDirective("Can I stop my medication?", answer);
        assertThat(r.redirected()).isTrue();
        assertThat(r.safeAnswer()).startsWith("Please consult your doctor");
    }

    @Test
    void prescribingDirectiveWithExistingRedirectIsLeftAlone() {
        String answer = "I cannot advise that — please consult your doctor before changing your medication.";
        MedicalResponseSafetyService.DirectiveResult r =
                safety.handlePrescribingDirective("Can I stop my medication?", answer);
        assertThat(r.redirected()).isTrue();
        assertThat(r.safeAnswer()).isEqualTo(answer);
    }

    @Test
    void nonDirectiveQueryIsUntouched() {
        String answer = "Paracetamol reduces fever.";
        MedicalResponseSafetyService.DirectiveResult r =
                safety.handlePrescribingDirective("What is Paracetamol used for?", answer);
        assertThat(r.redirected()).isFalse();
        assertThat(r.safeAnswer()).isEqualTo(answer);
    }
}
