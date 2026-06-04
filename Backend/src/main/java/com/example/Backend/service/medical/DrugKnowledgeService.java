package com.example.Backend.service.medical;

import com.example.Backend.dto.DrugInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads the verified drug reference at startup and exposes lookup helpers.
 *
 * - Each drug has a canonical name plus a list of aliases (brand names,
 *   alternate spellings). Lookup is case-insensitive and works against both.
 * - {@link #extractMentionedDrugs(String)} scans free text for any known
 *   drug name or alias and returns the matching DrugInfo objects in mention
 *   order, deduplicated by canonical name.
 *
 * Loading is fault-tolerant: a missing or malformed JSON resource does not
 * stop application startup — it just leaves the registry empty (callers can
 * still degrade gracefully).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DrugKnowledgeService {

    private static final String RESOURCE_PATH = "medical_drug_reference.json";

    private final ObjectMapper objectMapper;

    /** Canonical name → DrugInfo. Preserves insertion order for stable iteration. */
    private final Map<String, DrugInfo> registry = new LinkedHashMap<>();
    /** Lowercased name/alias → canonical name, for fast lookup. */
    private final Map<String, String> aliasIndex = new HashMap<>();
    /** Compiled regex of all drug name/alias tokens, used by extractMentionedDrugs. */
    private Pattern mentionPattern;

    @PostConstruct
    void load() {
        try (InputStream in = new ClassPathResource(RESOURCE_PATH).getInputStream()) {
            List<DrugInfo> drugs = objectMapper.readValue(in, new TypeReference<>() {});
            for (DrugInfo d : drugs) {
                register(d);
            }
            buildMentionPattern();
            log.info("DrugKnowledgeService: loaded {} verified drug entries with {} aliases",
                    registry.size(), aliasIndex.size());
        } catch (Exception e) {
            log.error("Failed to load {} — drug knowledge will be empty. Cause: {}",
                    RESOURCE_PATH, e.getMessage());
        }
    }

    private void register(DrugInfo drug) {
        if (drug == null || drug.getDrugName() == null || drug.getDrugName().isBlank()) return;
        String canonical = drug.getDrugName().trim();
        registry.put(canonical, drug);
        aliasIndex.put(canonical.toLowerCase(Locale.ROOT), canonical);
        if (drug.getAliases() != null) {
            for (String alias : drug.getAliases()) {
                if (alias != null && !alias.isBlank()) {
                    aliasIndex.put(alias.toLowerCase(Locale.ROOT), canonical);
                }
            }
        }
    }

    private void buildMentionPattern() {
        if (aliasIndex.isEmpty()) {
            mentionPattern = null;
            return;
        }
        // Sort longest-first so "Dolo 650" matches before "Dolo"
        List<String> keys = new ArrayList<>(aliasIndex.keySet());
        keys.sort((a, b) -> Integer.compare(b.length(), a.length()));
        StringBuilder sb = new StringBuilder("(?i)\\b(");
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) sb.append('|');
            sb.append(Pattern.quote(keys.get(i)));
        }
        sb.append(")\\b");
        mentionPattern = Pattern.compile(sb.toString());
    }

    public Optional<DrugInfo> lookup(String nameOrAlias) {
        if (nameOrAlias == null || nameOrAlias.isBlank()) return Optional.empty();
        String canonical = aliasIndex.get(nameOrAlias.trim().toLowerCase(Locale.ROOT));
        return canonical == null ? Optional.empty() : Optional.ofNullable(registry.get(canonical));
    }

    /**
     * Find all drugs mentioned in the given text (case-insensitive, whole-token).
     * Returned list is deduplicated and order-preserving by first mention.
     */
    public List<DrugInfo> extractMentionedDrugs(String text) {
        if (text == null || text.isBlank() || mentionPattern == null) return List.of();
        Matcher m = mentionPattern.matcher(text);
        LinkedHashMap<String, DrugInfo> hits = new LinkedHashMap<>();
        while (m.find()) {
            String match = m.group(1).toLowerCase(Locale.ROOT);
            String canonical = aliasIndex.get(match);
            if (canonical != null) {
                DrugInfo info = registry.get(canonical);
                if (info != null) hits.putIfAbsent(canonical, info);
            }
        }
        return new ArrayList<>(hits.values());
    }

    /** Snapshot of all verified entries (for diagnostics / admin endpoints). */
    public List<DrugInfo> all() {
        return Collections.unmodifiableList(new ArrayList<>(registry.values()));
    }

    public int size() {
        return registry.size();
    }
}
