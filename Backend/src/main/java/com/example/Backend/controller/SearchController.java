package com.example.Backend.controller;

import com.example.Backend.dto.EmbeddingGenerationRequest;
import com.example.Backend.dto.SearchRequest;
import com.example.Backend.dto.SearchResponse;
import com.example.Backend.service.embedding.EmbeddingGenerationService;
import com.example.Backend.service.vector.SemanticSearchService;
import com.example.Backend.service.vector.VectorStoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final SemanticSearchService semanticSearchService;
    private final EmbeddingGenerationService embeddingGenerationService;
    private final VectorStoreService vectorStoreService;

    /**
     * Semantic search over the vector index.
     * Optionally scoped to a single document via {@code documentId} in the body.
     */
    @PostMapping
    public ResponseEntity<SearchResponse> search(
            @Valid @RequestBody SearchRequest request,
            Authentication authentication) {

        log.info("Search request from [{}]: '{}'", authentication.getName(), request.getQuery());
        return ResponseEntity.ok(semanticSearchService.search(request));
    }

    /**
     * Trigger (or re-trigger) embedding generation for a specific document.
     * Useful when a document was processed before the OpenAI key was configured,
     * or when you want to force a refresh with {@code regenerate: true}.
     */
    @PostMapping("/embeddings/generate")
    public ResponseEntity<Map<String, Object>> generateEmbeddings(
            @Valid @RequestBody EmbeddingGenerationRequest request,
            Authentication authentication) {

        log.info("Embedding generation triggered for document [id={}] by [{}]",
                request.getDocumentId(), authentication.getName());

        embeddingGenerationService.generateEmbeddingsForDocument(
                request.getDocumentId(),
                Boolean.TRUE.equals(request.getRegenerate())
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "documentId", request.getDocumentId(),
                "message", "Embedding generation completed successfully"
        ));
    }

    /**
     * ADMIN ONLY — returns vector index statistics.
     */
    @GetMapping("/index/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> indexStats() {
        return ResponseEntity.ok(Map.of(
                "totalVectors", vectorStoreService.getIndexSize(),
                "status", "active"
        ));
    }

    /**
     * ADMIN ONLY — wipes the entire in-memory vector index.
     * Re-upload and re-embed documents to rebuild it.
     */
    @DeleteMapping("/index/clear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> clearIndex() {
        vectorStoreService.clearIndex();
        return ResponseEntity.ok(Map.of("message", "Vector index cleared successfully"));
    }
}