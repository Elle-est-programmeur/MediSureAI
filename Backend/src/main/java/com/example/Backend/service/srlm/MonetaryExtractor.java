package com.example.Backend.service.srlm;

import com.example.Backend.dto.MonetaryEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts monetary amounts and their surrounding semantic context from text.
 * Supports ₹ / Rs. / Rs / INR / $ prefixes and "X rupees" suffix forms.
 *
 * Output {@link MonetaryEntity} carries the amount, currency, optional unit
 * ("per day", "per visit", …), a short surrounding context window for topic
 * association, and an isLimit flag set when neighbouring text uses cap-like
 * language ("up to", "limit", "maximum", "capped").
 */
@Service
@Slf4j
public class MonetaryExtractor {

    // ₹5,000.00 | Rs. 5,000 | Rs 5000 | INR 5,000 | $5000 | 5,000 rupees
    private static final Pattern AMOUNT = Pattern.compile(
            "(?i)(?:(₹|Rs\\.?|INR|\\$)\\s*([\\d,]+(?:\\.\\d+)?)" +
            "|([\\d,]+(?:\\.\\d+)?)\\s*(rupees|rs\\.?|inr))");

    private static final Pattern UNIT = Pattern.compile(
            "(?i)(per\\s+(day|visit|claim|year|policy\\s*year|hospitalisation|hospitalization|admission)|/\\s*day|/\\s*visit)");

    private static final List<String> LIMIT_CUES = List.of(
            "up to", "upto", "maximum", "capped", "limit", "limited to",
            "subject to a limit", "not exceeding", "ceiling"
    );

    public List<MonetaryEntity> extract(String text) {
        if (text == null || text.isEmpty()) return List.of();

        List<MonetaryEntity> out = new ArrayList<>();
        Matcher m = AMOUNT.matcher(text);
        while (m.find()) {
            String currency = m.group(1);
            String rawAmount = m.group(2);
            if (rawAmount == null) {
                rawAmount = m.group(3);
                currency = "INR"; // "rupees" suffix
            } else if (currency == null) {
                currency = inferCurrency(m.group(0));
            }
            BigDecimal amount;
            try {
                amount = new BigDecimal(rawAmount.replace(",", ""));
            } catch (NumberFormatException ex) {
                continue;
            }

            int start = Math.max(0, m.start() - 60);
            int end = Math.min(text.length(), m.end() + 60);
            String window = text.substring(start, end);

            String unit = findUnit(window);
            boolean isLimit = containsAny(window.toLowerCase(Locale.ROOT), LIMIT_CUES);

            out.add(MonetaryEntity.builder()
                    .amount(amount)
                    .currency(normaliseCurrency(currency))
                    .unit(unit)
                    .surroundingContext(window.replaceAll("\\s+", " ").trim())
                    .isLimit(isLimit)
                    .build());
        }
        return out;
    }

    private String findUnit(String window) {
        Matcher u = UNIT.matcher(window);
        if (u.find()) {
            String g = u.group(0).toLowerCase(Locale.ROOT)
                    .replaceAll("\\s+", " ")
                    .replace("/", "per ");
            return g.trim();
        }
        return null;
    }

    private String inferCurrency(String token) {
        if (token == null) return "INR";
        String t = token.toLowerCase(Locale.ROOT);
        if (t.contains("$")) return "USD";
        if (t.contains("₹") || t.contains("rs") || t.contains("inr") || t.contains("rupee")) return "INR";
        return "INR";
    }

    private String normaliseCurrency(String c) {
        if (c == null) return "INR";
        String t = c.trim().toLowerCase(Locale.ROOT);
        if (t.contains("$")) return "USD";
        return "INR";
    }

    private boolean containsAny(String haystack, List<String> needles) {
        for (String n : needles) {
            if (haystack.contains(n)) return true;
        }
        return false;
    }
}
