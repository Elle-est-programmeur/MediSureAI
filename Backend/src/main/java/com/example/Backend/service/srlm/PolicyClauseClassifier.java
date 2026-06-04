package com.example.Backend.service.srlm;

import com.example.Backend.dto.ClauseClassification;
import com.example.Backend.dto.RetrievedChunk;
import com.example.Backend.model.ClauseCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule-based policy-clause classifier.
 *
 * Deliberately deterministic — no LLM call. The rules are ordered by
 * specificity: an EXCLUSION cue beats a generic COVERAGE cue in the same
 * chunk because exclusions narrow the meaning. FINANCIAL_LIMIT beats
 * generic LIMITATION because a "₹5,000/day room rent cap" is functionally
 * different from "subject to medical necessity".
 *
 * Confidence is a function of how many distinct cues matched, bounded in [0,1].
 */
@Service
@Slf4j
public class PolicyClauseClassifier {

    private static final List<Rule> RULES = List.of(
            // FINANCIAL_LIMIT — must come before LIMITATION.
            // NOTE: avoid short cues whose normalised form is too generic.
            // "/day" normalises to "day" (slash stripped as punctuation) and
            // would match "30 days"; "rs." normalises to "rs" which is a
            // substring of many words. Use phrasal cues instead.
            rule(ClauseCategory.FINANCIAL_LIMIT, 0.95,
                    "₹", "inr", "rupees",
                    "per day", "per visit", "per claim",
                    "maximum of", "up to", "subject to a limit of",
                    "capped at", "limit of", "limited to",
                    "sum insured"),
            // EXCLUSION — strongest negative
            rule(ClauseCategory.EXCLUSION, 0.95,
                    "excluded", "exclusion", "not covered", "not payable",
                    "shall not be covered", "will not be covered",
                    "denied", "ineligible", "no cover for", "is not eligible"),
            // WAITING_PERIOD
            rule(ClauseCategory.WAITING_PERIOD, 0.90,
                    "waiting period", "after a period of", "moratorium",
                    "first 30 days", "first 90 days", "initial waiting",
                    "pre-existing disease", "ped waiting"),
            // CLAIM_PROCESS
            rule(ClauseCategory.CLAIM_PROCESS, 0.85,
                    "claim form", "submit", "documents required",
                    "intimation", "pre-authorization", "pre-authorisation",
                    "tpa", "claim settlement", "reimbursement process",
                    "within 30 days", "claim notification"),
            // LIMITATION (functional / non-monetary caps)
            rule(ClauseCategory.LIMITATION, 0.85,
                    "subject to", "provided that", "only if", "conditional",
                    "restricted to", "applies only", "co-pay", "copay",
                    "co-payment", "deductible"),
            // BENEFIT — value-adds, soft positives
            rule(ClauseCategory.BENEFIT, 0.80,
                    "no claim bonus", "free health check", "wellness",
                    "value added", "bonus", "additional benefit",
                    "complimentary", "loyalty bonus"),
            // COVERAGE — affirmative
            rule(ClauseCategory.COVERAGE, 0.80,
                    "is covered", "are covered", "covered up to",
                    "covered under", "payable", "reimbursed", "shall pay",
                    "will pay", "is included", "shall be reimbursed")
    );

    public ClauseClassification classify(RetrievedChunk chunk) {
        if (chunk == null || chunk.getContent() == null || chunk.getContent().isBlank()) {
            return ClauseClassification.builder()
                    .chunkId(chunk == null ? null : chunk.getChunkId())
                    .category(ClauseCategory.GENERAL_INFO)
                    .confidence(0.0)
                    .matchedCues(List.of())
                    .summary("Empty content")
                    .build();
        }

        String norm = InsuranceKeywords.normalise(chunk.getContent());
        ClauseCategory bestCategory = ClauseCategory.GENERAL_INFO;
        double bestConfidence = 0.0;
        List<String> matchedCues = new ArrayList<>();

        for (Rule rule : RULES) {
            List<String> hits = new ArrayList<>();
            for (String cue : rule.cues()) {
                String c = InsuranceKeywords.normalise(cue);
                if (norm.contains(c)) hits.add(cue);
            }
            if (hits.isEmpty()) continue;

            // confidence scales with cue density: 1 cue → base, 2 → +0.05, 3+ → +0.10
            double conf = Math.min(1.0, rule.baseConfidence() + 0.05 * (hits.size() - 1));
            if (conf > bestConfidence) {
                bestConfidence = conf;
                bestCategory = rule.category();
                matchedCues = hits;
            }
        }

        return ClauseClassification.builder()
                .chunkId(chunk.getChunkId())
                .category(bestCategory)
                .confidence(round(bestConfidence))
                .matchedCues(matchedCues)
                .summary(summarise(chunk.getContent()))
                .build();
    }

    public List<ClauseClassification> classifyAll(List<RetrievedChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) return List.of();
        List<ClauseClassification> out = new ArrayList<>(chunks.size());
        for (RetrievedChunk c : chunks) out.add(classify(c));
        return out;
    }

    private String summarise(String content) {
        String trimmed = content.trim().replaceAll("\\s+", " ");
        return trimmed.length() <= 140 ? trimmed : trimmed.substring(0, 140) + "…";
    }

    private double round(double v) { return Math.round(v * 100.0) / 100.0; }

    private static Rule rule(ClauseCategory category, double base, String... cues) {
        return new Rule(category, base, List.of(cues));
    }

    private record Rule(ClauseCategory category, double baseConfidence, List<String> cues) {}
}
