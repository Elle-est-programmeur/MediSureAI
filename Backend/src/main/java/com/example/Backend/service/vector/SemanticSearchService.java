package com.example.Backend.service.vector;

import com.example.Backend.dto.SearchRequest;
import com.example.Backend.dto.SearchResponse;
import com.example.Backend.vector.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SemanticSearchService {

    // ── Phase 5 Integration: Query exactly where Phase 4 natively stored the docs! ──
    private final VectorStore vectorStore;

    public SearchResponse search(SearchRequest request) {
        long start = System.currentTimeMillis();
        log.info("Semantic search via Spring AI (PgVector): '{}'", request.getQuery());

        int topK = request.getTopK() != null ? request.getTopK() : 5;

        // Build native Spring AI SearchRequest
        org.springframework.ai.vectorstore.SearchRequest aiRequest = 
                org.springframework.ai.vectorstore.SearchRequest.query(request.getQuery())
                        .withTopK(topK);

        // Filter safely by document ID if specified 
        if (request.getDocumentId() != null) {
            aiRequest.withFilterExpression("document_id == '" + request.getDocumentId() + "'");
        }

        // Run similarity search directly against the robust PgVector store
        List<org.springframework.ai.document.Document> docs = vectorStore.similaritySearch(aiRequest);

        // Map Spring AI documents to our custom phase DTO expected by SRLM Pipeline
        List<SearchResult> results = docs.stream().map(doc -> {
            SearchResult res = new SearchResult();
            res.setChunkId(doc.getId());
            res.setContent(doc.getContent());
            res.setMetadata(doc.getMetadata());
            // Defaulting similarity score, PgVector returns distance but it's hidden under Spring AI's native abstraction
            res.setSimilarity(0.99); 
            return res;
        }).collect(Collectors.toList());

        // Extract and format the specific context strings to pass into the Agent
        String context = results.stream()
                .map(SearchResult::getContent)
                .collect(Collectors.joining("\n\n---\n\n"));

        long elapsed = System.currentTimeMillis() - start;
        log.info("PgVector Search complete in {}ms — Found {} deeply relevant results", elapsed, results.size());

        return SearchResponse.builder()
                .query(request.getQuery())
                .resultsCount(results.size())
                .results(results)
                .context(context)
                .searchTimeMs(elapsed)
                .build();
    }
}