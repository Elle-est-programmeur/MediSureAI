package com.example.Backend.service.srlm;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Curated vocabulary used for hybrid retrieval boosting, context validation,
 * and contradiction detection. Centralised so all SRLM stages agree on which
 * terms matter and on their canonical aliases.
 */
public final class InsuranceKeywords {

    private InsuranceKeywords() {}

    /** Canonical terms → list of synonyms / close variants. */
    public static final List<KeywordGroup> GROUPS = List.of(
            group("ICU", "icu", "intensive care", "intensive care unit", "critical care", "icu charges"),
            group("Sum Insured", "sum insured", "sum-insured", "coverage limit", "policy limit", "maximum coverage"),
            group("Coverage", "coverage", "covered", "covers", "covering", "is covered"),
            group("Exclusion", "exclusion", "excluded", "not covered", "not payable", "excluded from", "shall not"),
            group("Waiting Period", "waiting period", "waiting-period", "moratorium"),
            group("Hospitalization", "hospitalization", "hospitalisation", "hospital stay", "in-patient", "inpatient"),
            group("Cashless", "cashless", "cashless facility", "cashless hospitalization", "cashless network"),
            group("Claim", "claim", "claims", "claim process", "claim settlement", "claim form"),
            group("Pre-existing", "pre-existing", "pre existing", "preexisting", "ped"),
            group("Daycare", "daycare", "day care", "day-care", "day care procedure"),
            group("Co-pay", "copay", "co-pay", "co pay", "co-payment", "deductible"),
            group("Premium", "premium", "premium amount"),
            group("Network Hospital", "network hospital", "empanelled hospital", "preferred provider"),
            group("Pre-hospitalization", "pre-hospitalization", "pre hospitalization", "pre-hospitalisation"),
            group("Post-hospitalization", "post-hospitalization", "post hospitalization", "post-hospitalisation"),
            group("Maternity", "maternity", "pregnancy", "delivery"),
            group("OPD", "opd", "out-patient", "outpatient", "out patient"),
            group("Diagnostic", "diagnostic", "investigation", "lab test", "test"),
            group("Surgery", "surgery", "surgical", "operation", "procedure"),
            group("Pharmacy", "pharmacy", "drug", "medication", "medicine", "prescription"),
            group("Ambulance", "ambulance", "ambulance charges"),
            group("Renewal", "renewal", "renew"),
            group("Domiciliary", "domiciliary", "home treatment", "home care")
    );

    private static final Pattern WS = Pattern.compile("\\s+");
    private static final Pattern PUNCT = Pattern.compile("[\\p{Punct}&&[^\\-]]");

    /** Lowercase + trim + collapse whitespace + strip accents + drop punctuation (keeps hyphens). */
    public static String normalise(String text) {
        if (text == null) return "";
        String n = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        n = n.toLowerCase(Locale.ROOT);
        n = PUNCT.matcher(n).replaceAll(" ");
        return WS.matcher(n).replaceAll(" ").trim();
    }

    /**
     * Returns the canonical labels for groups whose ANY alias appears in the text.
     * Order preserved via LinkedHashSet.
     */
    public static Set<String> matchedGroups(String text) {
        String norm = " " + normalise(text) + " ";
        Set<String> hits = new LinkedHashSet<>();
        for (KeywordGroup g : GROUPS) {
            for (String alias : g.aliases()) {
                String token = " " + normalise(alias) + " ";
                if (norm.contains(token)) {
                    hits.add(g.canonical());
                    break;
                }
            }
        }
        return hits;
    }

    /** Returns the *aliases* (not canonicals) that literally occur in the text. */
    public static Set<String> matchedAliases(String text) {
        String norm = " " + normalise(text) + " ";
        Set<String> hits = new LinkedHashSet<>();
        for (KeywordGroup g : GROUPS) {
            for (String alias : g.aliases()) {
                String token = " " + normalise(alias) + " ";
                if (norm.contains(token)) {
                    hits.add(alias);
                }
            }
        }
        return hits;
    }

    /** Negation cues used by the contradiction detector. */
    public static final Set<String> NEGATION_CUES = Set.of(
            "not covered", "no coverage", "is not covered", "are not covered",
            "excluded", "exclusion", "not payable", "shall not", "will not",
            "denied", "rejected", "no cover", "not eligible", "ineligible"
    );

    /**
     * Affirmative coverage cues. Deliberately excludes bare "covered" because it is
     * a substring of "not covered" and would otherwise classify negative clauses as
     * affirmative. The phrasal cues below carry the affirmative meaning unambiguously.
     */
    public static final Set<String> AFFIRMATIVE_CUES = Set.of(
            "is covered", "are covered", "covered up to", "covered under",
            "shall be covered", "will be covered",
            "payable", "shall pay", "will pay",
            "is eligible", "are eligible",
            "included", "reimbursed", "reimbursable"
    );

    private static KeywordGroup group(String canonical, String... aliases) {
        return new KeywordGroup(canonical, Arrays.asList(aliases));
    }

    public record KeywordGroup(String canonical, List<String> aliases) {}
}
