package com.example.Backend.service.srlm;

import com.example.Backend.dto.RetrievedChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Retrieves evidence chunks with explainable hybrid scoring:
 *
 *   finalScore = (W_E * embeddingSimilarity) + (W_K * keywordScore)
 *
 * - embeddingSimilarity is the cosine score from the vector store. If the
 *   underlying store hides distance (the Spring AI PgVector wrapper does),
 *   we fall back to a rank-decayed score so chunks still have a usable signal
 *   instead of every chunk reporting "0.99".
 * - keywordScore is the fraction of insurance-vocabulary groups in the query
 *   that also appear in the chunk text. This is the fix for queries like
 *   "Is ICU treatment fully covered?" where the embedding model sometimes
 *   surfaces daycare/exclusion clauses ahead of the literal ICU clause.
 *
 * Every chunk carries the keywords it matched so downstream stages and the
 * /debug endpoint can show *why* it was ranked where it was.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HybridRetrievalService {

    private final VectorStore vectorStore;

    @Value("${srlm.retrieval.embedding-weight:0.7}")
    private double embeddingWeight;

    @Value("${srlm.retrieval.keyword-weight:0.3}")
    private double keywordWeight;

    @Value("${srlm.retrieval.default-topk:8}")
    private int defaultTopK;

    public List<RetrievedChunk> retrieve(String query, int topK, String filterExpression) {
        int k = topK <= 0 ? defaultTopK : topK;

        SearchRequest req = SearchRequest.query(query).withTopK(k);
        if (filterExpression != null && !filterExpression.isBlank()) {
            req = req.withFilterExpression(filterExpression);
        }

        List<Document> docs;
        try {
            docs = vectorStore.similaritySearch(req);
        } catch (Exception ex) {
            log.error("Vector store similarity search failed: {}", ex.getMessage(), ex);
            return List.of();
        }
        if (docs == null || docs.isEmpty()) return List.of();

        Set<String> queryGroups = InsuranceKeywords.matchedGroups(query);
        // If query has no recognised insurance terms, fall back to query tokens
        Set<String> queryTokens = queryGroups.isEmpty() ? tokenize(query) : queryGroups;

        int n = docs.size();
        List<RetrievedChunk> chunks = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Document d = docs.get(i);
            double emb = extractEmbeddingSimilarity(d, i, n);
            String content = d.getContent() == null ? "" : d.getContent();

            Set<String> chunkGroups = InsuranceKeywords.matchedGroups(content);
            List<String> matched = new ArrayList<>();
            int hits = 0;
            for (String t : queryTokens) {
                boolean hit = queryGroups.isEmpty()
                        ? InsuranceKeywords.normalise(content).contains(t)
                        : chunkGroups.contains(t);
                if (hit) {
                    matched.add(t);
                    hits++;
                }
            }
            double kw = queryTokens.isEmpty() ? 0.0 : ((double) hits) / queryTokens.size();
            double finalScore = (embeddingWeight * emb) + (keywordWeight * kw);

            chunks.add(RetrievedChunk.builder()
                    .chunkId(d.getId())
                    .content(content)
                    .embeddingSimilarity(round(emb))
                    .keywordScore(round(kw))
                    .finalScore(round(finalScore))
                    .matchedKeywords(matched)
                    .metadata(d.getMetadata())
                    .build());
        }
        return chunks;
    }

    /**
     * Spring AI's PgVector wrapper does not surface the raw distance on the
     * Document, so try a handful of common metadata keys; otherwise approximate
     * with a rank-decayed score in [0.5, 1.0].
     */
    private double extractEmbeddingSimilarity(Document d, int rank, int total) {
        Map<String, Object> md = d.getMetadata();
        if (md != null) {
            for (String key : List.of("distance", "score", "similarity", "_distance")) {
                Object v = md.get(key);
                if (v instanceof Number num) {
                    double n = num.doubleValue();
                    if (key.contains("distance")) {
                        // distance ∈ [0, 2] for cosine — convert to similarity
                        return clamp01(1.0 - (n / 2.0));
                    }
                    return clamp01(n);
                }
            }
        }
        // Rank-decay fallback: rank 0 → 1.0, last → 0.5
        if (total <= 1) return 1.0;
        return 1.0 - 0.5 * ((double) rank / (total - 1));
    }

    private Set<String> tokenize(String text) {
        Set<String> out = new HashSet<>();
        for (String t : InsuranceKeywords.normalise(text).split(" ")) {
            if (t.length() >= 4) out.add(t);
        }
        return out;
    }

    private double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
