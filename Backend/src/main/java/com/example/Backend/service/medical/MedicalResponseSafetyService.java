package com.example.Backend.service.medical;

import com.example.Backend.dto.DrugInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Last line of defence against pharmacological hallucinations.
 *
 * Two responsibilities:
 *
 * 1. {@link #validate(String, java.util.List)} — given a generated answer and
 *    the drugs it references (resolved via {@link DrugKnowledgeService}), scan
 *    for "adverse effect" claims that are NOT in any referenced drug's
 *    verified lexicon. Each unsupported claim becomes a {@link SafetyIssue}.
 *
 * 2. {@link #handlePrescribingDirective(String, String)} — detect when the
 *    user is asking the assistant to make a prescribing decision ("can I stop
 *    taking…", "should I double the dose…") and ensure the response contains
 *    an explicit referral to a doctor or pharmacist.
 *
 * The service does NOT rewrite the answer in place — it returns a structured
 * {@link Result} so callers can decide whether to replace, append a notice,
 * or trigger regeneration. AgentOrchestrator currently appends a safety
 * footer when issues are found.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MedicalResponseSafetyService {

    private final DrugKnowledgeService drugKnowledgeService;

    /** Adverse-effect keywords that REQUIRE explicit evidence in the verified DB. */
    private static final List<String> ADVERSE_KEYWORDS = List.of(
            "heart rate", "tachycardia", "bradycardia", "palpitations",
            "blood pressure", "hypertension", "hypotension",
            "liver failure", "liver damage", "hepatic", "hepatitis",
            "kidney failure", "renal failure", "nephrotoxic",
            "stroke", "seizure", "convulsions",
            "hallucinations", "psychosis",
            "anaphylaxis", "anaphylactic", "swelling of the face",
            "stomach bleeding", "gastrointestinal bleeding", "gi bleed",
            "addiction", "dependence",
            "respiratory depression", "breathing difficulty",
            "blood clot", "thrombosis", "embolism",
            "death", "fatal", "lethal",
            "dehydration",
            "infertility",
            "blindness"
    );

    /** Cues that indicate the user is seeking a prescribing decision. */
    private static final List<String> DIRECTIVE_CUES = List.of(
            "can i stop", "should i stop", "stop taking", "stop my medication",
            "should i increase", "can i increase", "double the dose", "double my dose",
            "should i skip", "can i skip", "miss a dose",
            "should i take", "can i take more",
            "should i change", "can i change",
            "is it safe to stop", "what if i stop"
    );

    /** Cues that already redirect the patient — the LLM met its obligation. */
    private static final List<String> REDIRECT_CUES = List.of(
            "consult your doctor", "consult a doctor",
            "speak to your doctor", "speak with your doctor", "talk to your doctor",
            "ask your doctor", "see your doctor",
            "consult your pharmacist", "speak to your pharmacist",
            "medical professional", "qualified healthcare", "healthcare professional"
    );

    public static final String DOCTOR_REDIRECT =
            "Please consult your doctor or pharmacist for personalized medical advice. " +
            "Do not stop, skip, or change a prescribed medication without their guidance.";

    private static final String UNSUPPORTED_FOOTER_TEMPLATE =
            "\n\nSafety note: %s This information has been removed from the answer above. " +
            "Please consult your doctor or pharmacist for any concerns about side effects or interactions.";

    /**
     * Cross-references the answer against the verified DB for the given drugs.
     * Returns a sanitized version of the response when unsupported claims are
     * found, and a list of structured issues for observability.
     */
    public Result validate(String answer, List<DrugInfo> referencedDrugs) {
        if (answer == null || answer.isBlank()) {
            return new Result(answer, List.of(), false);
        }
        // Always also resolve drugs mentioned in the text itself
        List<DrugInfo> drugsInText = drugKnowledgeService.extractMentionedDrugs(answer);
        LinkedHashSet<DrugInfo> allDrugs = new LinkedHashSet<>();
        if (referencedDrugs != null) allDrugs.addAll(referencedDrugs);
        allDrugs.addAll(drugsInText);

        if (allDrugs.isEmpty()) {
            return new Result(answer, List.of(), false);
        }

        String allowedLexicon = buildAllowedLexicon(allDrugs);
        String normAnswer = normalise(answer);

        List<SafetyIssue> issues = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String keyword : ADVERSE_KEYWORDS) {
            String normKeyword = normalise(keyword);
            if (!normAnswer.contains(normKeyword)) continue;
            if (allowedLexicon.contains(normKeyword)) continue;
            if (seen.contains(normKeyword)) continue;
            seen.add(normKeyword);
            issues.add(SafetyIssue.unsupportedAdverseEffect(keyword, allDrugs));
        }

        if (issues.isEmpty()) {
            return new Result(answer, List.of(), false);
        }

        log.warn("MedicalResponseSafetyService: {} unsupported claim(s) detected for drugs {} → sanitizing",
                issues.size(),
                allDrugs.stream().map(DrugInfo::getDrugName).toList());

        String summary = issues.size() == 1
                ? "The original draft mentioned \"" + issues.get(0).getClaim()
                + "\", which is not supported by the verified drug information."
                : "The original draft mentioned " + issues.size()
                + " effects that are not supported by the verified drug information.";

        String sanitized = answer + String.format(UNSUPPORTED_FOOTER_TEMPLATE, summary);
        return new Result(sanitized, issues, true);
    }

    /**
     * If the user is asking for a prescribing decision, returns an answer that
     * begins with — or appends — a doctor-redirect. If the original answer
     * already redirects, returns it unchanged.
     */
    public DirectiveResult handlePrescribingDirective(String query, String answer) {
        if (query == null || answer == null) return new DirectiveResult(answer, false);
        String q = query.toLowerCase(Locale.ROOT);
        boolean directive = DIRECTIVE_CUES.stream().anyMatch(q::contains);
        if (!directive) return new DirectiveResult(answer, false);

        String a = answer.toLowerCase(Locale.ROOT);
        boolean alreadyRedirects = REDIRECT_CUES.stream().anyMatch(a::contains);
        if (alreadyRedirects) return new DirectiveResult(answer, true);

        String redirected = DOCTOR_REDIRECT + "\n\n" + answer;
        log.info("MedicalResponseSafetyService: prescribing directive detected — prepending doctor redirect");
        return new DirectiveResult(redirected, true);
    }

    /** Build a single normalised string containing every verified claim across drugs. */
    private String buildAllowedLexicon(Set<DrugInfo> drugs) {
        StringBuilder sb = new StringBuilder();
        for (DrugInfo d : drugs) {
            appendList(sb, d.getUses());
            appendList(sb, d.getCommonSideEffects());
            appendList(sb, d.getSeriousWarnings());
            appendList(sb, d.getContraindications());
            appendList(sb, d.getCommonInteractions());
            if (d.getPatientFriendlyExplanation() != null) {
                sb.append(' ').append(d.getPatientFriendlyExplanation()).append(' ');
            }
        }
        return normalise(sb.toString());
    }

    private void appendList(StringBuilder sb, List<String> items) {
        if (items == null) return;
        for (String s : items) {
            if (s != null) sb.append(' ').append(s).append(' ');
        }
    }

    private static String normalise(String text) {
        if (text == null) return "";
        String n = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return n.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    public record Result(String safeAnswer, List<SafetyIssue> issues, boolean sanitized) {
        public List<SafetyIssue> getIssues() { return Collections.unmodifiableList(issues); }
    }

    public record DirectiveResult(String safeAnswer, boolean redirected) {}

    public static final class SafetyIssue {
        public enum Kind { UNSUPPORTED_ADVERSE_EFFECT, UNSUPPORTED_INTERACTION, UNSUPPORTED_USE }
        private final Kind kind;
        private final String claim;
        private final List<String> referencedDrugs;

        private SafetyIssue(Kind kind, String claim, List<String> referencedDrugs) {
            this.kind = kind;
            this.claim = claim;
            this.referencedDrugs = referencedDrugs;
        }

        static SafetyIssue unsupportedAdverseEffect(String keyword, Set<DrugInfo> drugs) {
            return new SafetyIssue(Kind.UNSUPPORTED_ADVERSE_EFFECT, keyword,
                    drugs.stream().map(DrugInfo::getDrugName).toList());
        }

        public Kind getKind() { return kind; }
        public String getClaim() { return claim; }
        public List<String> getReferencedDrugs() { return referencedDrugs; }
    }
}
