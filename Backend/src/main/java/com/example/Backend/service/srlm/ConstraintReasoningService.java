package com.example.Backend.service.srlm;

import com.example.Backend.dto.FinancialCalculation;
import com.example.Backend.dto.MonetaryEntity;
import com.example.Backend.dto.RetrievedChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Computes a deterministic financial coverage interpretation.
 *
 * Given:
 *   - the user's query (which may contain an actual amount, e.g. "₹7,000 room rent")
 *   - retrieved evidence chunks (which may contain a cap, e.g. "Room Rent: ₹5,000/day")
 *
 * Produces a {@link FinancialCalculation} with:
 *   covered    = min(actual, limit)
 *   payable    = max(0, actual - limit)
 *   status     = COVERED / PARTIALLY_COVERED / NOT_COVERED / UNKNOWN
 *
 * The reasoning is intentionally rule-based — no LLM call — so the same input
 * always produces the same output. The downstream FinancialCalculationValidator
 * sanity-checks that covered + payable == actual and refuses to ship inconsistent
 * results.
 *
 * If no amount / limit / topic can be matched, returns UNKNOWN with valid=true
 * so the SRLM pipeline can degrade gracefully without forcing a fallback.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConstraintReasoningService {

    private final MonetaryExtractor monetaryExtractor;

    public FinancialCalculation reason(String query, List<RetrievedChunk> evidence) {
        List<MonetaryEntity> queryAmounts = monetaryExtractor.extract(safe(query));
        // Only the "isLimit=false" amounts in the query are actuals; the rest are caps the user quoted
        MonetaryEntity actual = queryAmounts.stream()
                .filter(e -> !e.isLimit())
                .findFirst()
                .orElseGet(() -> queryAmounts.isEmpty() ? null : queryAmounts.get(0));

        // Limits come from the policy evidence
        MonetaryEntity limit = null;
        String topic = inferTopic(query, evidence);
        RetrievedChunk supporting = null;

        if (evidence != null) {
            outer:
            for (RetrievedChunk c : evidence) {
                List<MonetaryEntity> amounts = monetaryExtractor.extract(c.getContent());
                for (MonetaryEntity a : amounts) {
                    if (a.isLimit()) {
                        // Prefer a limit whose surrounding context mentions the topic
                        if (topic == null || normContains(a.getSurroundingContext(), topic)) {
                            limit = a;
                            supporting = c;
                            break outer;
                        }
                    }
                }
            }
            // Fallback: first chunk with any limit (even without topic match)
            if (limit == null) {
                for (RetrievedChunk c : evidence) {
                    List<MonetaryEntity> amounts = monetaryExtractor.extract(c.getContent());
                    for (MonetaryEntity a : amounts) {
                        if (a.isLimit()) {
                            limit = a;
                            supporting = c;
                            break;
                        }
                    }
                    if (limit != null) break;
                }
            }
        }

        // If no actual amount in the query but a limit was found, this is still
        // useful — caller can present the cap to the user as a policy fact.
        if (actual == null && limit == null) {
            return FinancialCalculation.builder()
                    .status(FinancialCalculation.CoverageStatus.UNKNOWN)
                    .topic(topic)
                    .valid(true)
                    .notes(List.of("No monetary amount found in query or evidence"))
                    .build();
        }

        BigDecimal actualAmt = actual == null ? null : actual.getAmount();
        BigDecimal limitAmt  = limit  == null ? null : limit.getAmount();
        String currency = actual != null ? actual.getCurrency()
                : limit != null ? limit.getCurrency() : "INR";
        String unit = limit != null ? limit.getUnit() : actual != null ? actual.getUnit() : null;

        BigDecimal covered = null;
        BigDecimal payable = null;
        FinancialCalculation.CoverageStatus status;
        List<String> notes = new ArrayList<>();

        if (actualAmt != null && limitAmt != null) {
            covered = actualAmt.min(limitAmt);
            payable = actualAmt.subtract(covered).max(BigDecimal.ZERO);
            if (payable.signum() == 0) {
                status = FinancialCalculation.CoverageStatus.COVERED;
                notes.add("Actual " + format(actualAmt) + " ≤ limit " + format(limitAmt) + " — fully covered");
            } else {
                status = FinancialCalculation.CoverageStatus.PARTIALLY_COVERED;
                notes.add("Actual " + format(actualAmt) + " > limit " + format(limitAmt)
                        + " — insurer pays " + format(covered) + ", patient pays " + format(payable));
            }
        } else if (limitAmt != null) {
            // Query had no actual; we can still describe the cap
            status = FinancialCalculation.CoverageStatus.PARTIALLY_COVERED;
            notes.add("Policy cap is " + format(limitAmt)
                    + (unit != null ? " " + unit : "") + "; actual amount not provided");
        } else {
            status = FinancialCalculation.CoverageStatus.UNKNOWN;
            notes.add("Limit not specified in evidence — coverage interpretation deferred");
        }

        FinancialCalculation calc = FinancialCalculation.builder()
                .status(status)
                .actualAmount(scale(actualAmt))
                .limitAmount(scale(limitAmt))
                .coveredAmount(scale(covered))
                .patientPayable(scale(payable))
                .currency(currency)
                .unit(unit)
                .topic(topic)
                .supportingClause(supporting == null ? null : supporting.getContent())
                .notes(notes)
                .valid(true)
                .build();
        log.info("ConstraintReasoning: topic={} status={} actual={} limit={} covered={} payable={}",
                topic, status, actualAmt, limitAmt, covered, payable);
        return calc;
    }

    /**
     * Light topic inference — look for an insurance group both the query AND
     * an evidence chunk mention. Falls back to the highest-frequency query group.
     */
    private String inferTopic(String query, List<RetrievedChunk> evidence) {
        Set<String> qg = InsuranceKeywords.matchedGroups(safe(query));
        if (qg.isEmpty()) return null;
        if (evidence != null) {
            for (String g : qg) {
                for (RetrievedChunk c : evidence) {
                    if (InsuranceKeywords.matchedGroups(c.getContent()).contains(g)) return g;
                }
            }
        }
        return qg.iterator().next();
    }

    private boolean normContains(String text, String topic) {
        if (text == null || topic == null) return false;
        return InsuranceKeywords.normalise(text).contains(InsuranceKeywords.normalise(topic));
    }

    private String format(BigDecimal v) {
        return v == null ? "?" : "₹" + v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private BigDecimal scale(BigDecimal v) {
        return v == null ? null : v.setScale(2, RoundingMode.HALF_UP);
    }

    private String safe(String s) { return s == null ? "" : s; }
}
