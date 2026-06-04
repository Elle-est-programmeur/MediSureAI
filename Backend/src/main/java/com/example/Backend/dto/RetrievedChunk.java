package com.example.Backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * A single retrieved evidence chunk with full scoring transparency.
 * Used by the rerank + reasoning + citation stages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievedChunk {
    /** Stable chunk identifier from the vector store. */
    private String chunkId;
    /** Raw text content used for prompts and citations. */
    private String content;
    /** [0,1] cosine-similarity-equivalent score from the embedding store. */
    private double embeddingSimilarity;
    /** [0,1] keyword overlap score against the query. */
    private double keywordScore;
    /** finalScore = w_e * embeddingSimilarity + w_k * keywordScore. */
    private double finalScore;
    /** Keywords from the query that occur literally in this chunk. */
    private List<String> matchedKeywords;
    /** Original vector-store metadata (document_id, page, etc). */
    private Map<String, Object> metadata;
    /** Reason a chunk was rejected, if any (debug only). */
    private String rejectionReason;
}
