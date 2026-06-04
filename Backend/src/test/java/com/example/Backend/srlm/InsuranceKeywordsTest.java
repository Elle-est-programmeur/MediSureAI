package com.example.Backend.srlm;

import com.example.Backend.service.srlm.InsuranceKeywords;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InsuranceKeywordsTest {

    @Test
    void icuQueryMatchesIcuGroup() {
        Set<String> groups = InsuranceKeywords.matchedGroups("Is ICU treatment fully covered under my policy?");
        assertThat(groups).contains("ICU", "Coverage");
    }

    @Test
    void icuClauseMatchesIcuGroup() {
        Set<String> groups = InsuranceKeywords.matchedGroups("ICU Charges: Covered up to sum insured.");
        assertThat(groups).contains("ICU", "Sum Insured", "Coverage");
    }

    @Test
    void synonymsResolveToCanonical() {
        Set<String> g1 = InsuranceKeywords.matchedGroups("intensive care unit charges");
        assertThat(g1).contains("ICU");

        Set<String> g2 = InsuranceKeywords.matchedGroups("the patient was admitted to critical care");
        assertThat(g2).contains("ICU");
    }

    @Test
    void negationCuesDetected() {
        assertThat(InsuranceKeywords.NEGATION_CUES).contains("not covered", "excluded");
    }

    @Test
    void normalisationIsCaseAndWhitespaceInsensitive() {
        assertThat(InsuranceKeywords.normalise("  ICU  Charges  "))
                .isEqualTo("icu charges");
    }
}
