package com.example.Backend.service.srlm;

import com.example.Backend.dto.Citation;
import com.example.Backend.dto.RetrievedChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Finds the chunks that most likely support a generated answer and produces
 * Citation objects pointing back to a short quoted span. Cheap, deterministic,
 * runs in microseconds — no extra LLM call.
 */
@Service
@Slf4j
public class CitationExtractor {

    private static final int MAX_CITATIONS = 3;
    private static final int QUOTE_RADIUS = 80;

    public List<Citation> extract(String answer, List<RetrievedChunk> chunks) {
        if (answer == null || answer.isBlank() || chunks == null || chunks.isEmpty()) {
            return List.of();
        }

        Set<String> answerTopics = InsuranceKeywords.matchedGroups(answer);
        List<Citation> citations = new ArrayList<>();
        Set<String> usedChunkIds = new HashSet<>();

        // For each topic the answer mentions, pick the highest-ranked chunk that contains it
        for (String topic : answerTopics) {
            String topicNorm = InsuranceKeywords.normalise(topic);
            for (RetrievedChunk chunk : chunks) {
                if (usedChunkIds.contains(chunk.getChunkId())) continue;
                String content = chunk.getContent();
                if (content == null) continue;
                int idx = InsuranceKeywords.normalise(content).indexOf(topicNorm);
                if (idx < 0) continue;
                citations.add(Citation.builder()
                        .chunkId(chunk.getChunkId())
                        .quote(snippet(content, topic))
                        .sourceLabel(buildSourceLabel(chunk, topic))
                        .evidenceScore(chunk.getFinalScore())
                        .build());
                usedChunkIds.add(chunk.getChunkId());
                break;
            }
            if (citations.size() >= MAX_CITATIONS) break;
        }

        // If no topical matches found, fall back to the top-ranked chunk
        if (citations.isEmpty() && !chunks.isEmpty()) {
            RetrievedChunk top = chunks.get(0);
            citations.add(Citation.builder()
                    .chunkId(top.getChunkId())
                    .quote(snippet(top.getContent(), null))
                    .sourceLabel("Retrieved policy excerpt")
                    .evidenceScore(top.getFinalScore())
                    .build());
        }
        return citations;
    }

    private String snippet(String content, String anchor) {
        if (content == null) return "";
        String trimmed = content.trim();
        if (trimmed.length() <= QUOTE_RADIUS * 2) return trimmed;

        if (anchor != null) {
            int i = trimmed.toLowerCase(Locale.ROOT).indexOf(anchor.toLowerCase(Locale.ROOT));
            if (i >= 0) {
                int start = Math.max(0, i - QUOTE_RADIUS);
                int end = Math.min(trimmed.length(), i + anchor.length() + QUOTE_RADIUS);
                String window = trimmed.substring(start, end).trim();
                return (start > 0 ? "… " : "") + window + (end < trimmed.length() ? " …" : "");
            }
        }
        return trimmed.substring(0, Math.min(trimmed.length(), QUOTE_RADIUS * 2)) + " …";
    }

    private String buildSourceLabel(RetrievedChunk chunk, String topic) {
        if (chunk.getMetadata() != null) {
            Object file = chunk.getMetadata().get("file_name");
            if (file == null) file = chunk.getMetadata().get("source");
            Object page = chunk.getMetadata().get("page");
            if (file != null) {
                return file + (page != null ? " (p. " + page + ")" : "") + " → " + topic;
            }
        }
        return "Policy → " + topic;
    }
}
