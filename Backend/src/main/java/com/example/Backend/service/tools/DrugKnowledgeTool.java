package com.example.Backend.service.tools;

import com.example.Backend.dto.DrugInfo;
import com.example.Backend.dto.ToolContext;
import com.example.Backend.dto.ToolResult;
import com.example.Backend.model.ToolType;
import com.example.Backend.service.medical.DrugKnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Pulls verified drug knowledge from {@link DrugKnowledgeService} for any drug
 * mentioned in the user query OR in the patient's prescribed-drugs list (passed
 * via {@link ToolContext#getParameters()} under key "prescribedDrugs").
 *
 * The output is a plain-text block formatted for inclusion in the SRLM prompt:
 * uses, common side effects, serious warnings, contraindications, common
 * interactions, and a patient-friendly summary. The LLM is instructed
 * elsewhere (MultiReasoningService prompts) that this block is the ONLY
 * permitted source of pharmacological claims.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DrugKnowledgeTool implements Tool {

    private final DrugKnowledgeService drugKnowledgeService;

    @Override
    public ToolType getType() {
        return ToolType.DRUG_KNOWLEDGE;
    }

    @Override
    public ToolResult execute(ToolContext context) {
        long start = System.currentTimeMillis();

        Set<DrugInfo> hits = new LinkedHashSet<>();
        // 1. Drugs explicitly named in the user query
        hits.addAll(drugKnowledgeService.extractMentionedDrugs(context.getQuery()));

        // 2. Drugs from the patient's prescription, passed by the orchestrator
        Object prescribed = context.getParameters() == null
                ? null : context.getParameters().get("prescribedDrugs");
        if (prescribed instanceof List<?> list) {
            for (Object o : list) {
                if (o == null) continue;
                drugKnowledgeService.lookup(o.toString()).ifPresent(hits::add);
            }
        }

        if (hits.isEmpty()) {
            return ToolResult.builder()
                    .toolType(ToolType.DRUG_KNOWLEDGE)
                    .success(true)
                    .content("")
                    .confidence(0.0)
                    .executionTimeMs(System.currentTimeMillis() - start)
                    .build();
        }

        String block = formatBlock(new ArrayList<>(hits));
        log.info("DrugKnowledgeTool: matched {} drug entr{} ({}ms)",
                hits.size(), hits.size() == 1 ? "y" : "ies", System.currentTimeMillis() - start);
        return ToolResult.builder()
                .toolType(ToolType.DRUG_KNOWLEDGE)
                .success(true)
                .content(block)
                .confidence(1.0)
                .executionTimeMs(System.currentTimeMillis() - start)
                .build();
    }

    @Override
    public boolean isApplicable(ToolContext context) {
        if (context == null) return false;
        // Applicable if query mentions a drug OR caller explicitly passed prescribedDrugs
        if (context.getQuery() != null
                && !drugKnowledgeService.extractMentionedDrugs(context.getQuery()).isEmpty()) {
            return true;
        }
        if (context.getParameters() != null) {
            Object p = context.getParameters().get("prescribedDrugs");
            if (p instanceof List<?> list && !list.isEmpty()) return true;
        }
        return false;
    }

    /** Formats the verified knowledge block. The headings are stable so prompts can refer to them. */
    public String formatBlock(List<DrugInfo> drugs) {
        StringBuilder sb = new StringBuilder();
        sb.append("[VERIFIED DRUG KNOWLEDGE — authoritative source for all pharmacological claims]\n");
        sb.append("Source: medical_drug_reference.json (curated). Do NOT invent facts beyond this block.\n\n");
        for (int i = 0; i < drugs.size(); i++) {
            DrugInfo d = drugs.get(i);
            sb.append("Drug ").append(i + 1).append(": ").append(d.getDrugName());
            if (d.getDrugClass() != null && !d.getDrugClass().isBlank()) {
                sb.append("  (").append(d.getDrugClass()).append(")");
            }
            sb.append("\n");
            appendSection(sb, "Uses",                    d.getUses());
            appendSection(sb, "Common side effects",     d.getCommonSideEffects());
            appendSection(sb, "Serious warnings",        d.getSeriousWarnings());
            appendSection(sb, "Contraindications",       d.getContraindications());
            appendSection(sb, "Common interactions",     d.getCommonInteractions());
            if (d.getPatientFriendlyExplanation() != null && !d.getPatientFriendlyExplanation().isBlank()) {
                sb.append("  Patient summary: ").append(d.getPatientFriendlyExplanation()).append("\n");
            }
            if (d.getEvidenceSource() != null && !d.getEvidenceSource().isBlank()) {
                sb.append("  Source: ").append(d.getEvidenceSource()).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private void appendSection(StringBuilder sb, String label, List<String> items) {
        if (items == null || items.isEmpty()) return;
        sb.append("  ").append(label).append(":\n");
        for (String item : items) sb.append("    - ").append(item).append("\n");
    }

    /** Helper: build the prescribed-drugs list expected by execute. Used by AgentOrchestrator. */
    public static List<String> prescribedDrugNames(List<String> drugs) {
        if (drugs == null) return List.of();
        List<String> out = new ArrayList<>();
        for (String d : drugs) {
            if (d == null) continue;
            String name = d.trim();
            int space = name.indexOf(' ');
            if (space > 0) name = name.substring(0, space);
            if (!name.isEmpty()) out.add(name.toLowerCase(Locale.ROOT));
        }
        return out;
    }
}
