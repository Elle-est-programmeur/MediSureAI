package com.example.Backend.service.vector;

import com.example.Backend.dto.SearchRequest;
import com.example.Backend.dto.SearchResponse;
import com.example.Backend.service.embedding.EmbeddingService;
import com.example.Backend.vector.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SemanticSearchService {

    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;

    public SearchResponse search(SearchRequest request) {
        long start = System.currentTimeMillis();
        log.info("Semantic search: '{}'", request.getQuery());

        // Embed the query using the same model as the stored chunks
        float[] queryEmbedding = embeddingService.generateEmbedding(request.getQuery());

        // Cosine-similarity search in the vector index
        int topK = request.getTopK() != null ? request.getTopK() : 5;
        List<SearchResult> results = vectorStoreService.search(queryEmbedding, topK);

        // Optionally narrow to a single document
        if (request.getDocumentId() != null) {
            results = results.stream()
                    .filter(r -> request.getDocumentId().equals(r.getMetadata().get("postgresDocumentId")))
                    .collect(Collectors.toList());
        }

        // Hydrate content field from the metadata stored in the vector index
        // (avoids a secondary MongoDB round-trip per chunk)
        results.forEach(r -> r.setContent((String) r.getMetadata().get("content")));

        // Concatenate chunk text for direct use in an LLM prompt
        String context = results.stream()
                .map(SearchResult::getContent)
                .collect(Collectors.joining("\n\n---\n\n"));

        long elapsed = System.currentTimeMillis() - start;
        log.info("Search complete in {}ms — {} results", elapsed, results.size());

        return SearchResponse.builder()
                .query(request.getQuery())
                .resultsCount(results.size())
                .results(results)
                .context(context)
                .searchTimeMs(elapsed)
                .build();
    }
}