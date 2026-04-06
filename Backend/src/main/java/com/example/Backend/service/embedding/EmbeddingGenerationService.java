package com.example.Backend.service.embedding;

import com.example.Backend.document.DocumentChunk;
import com.example.Backend.exception.DocumentProcessingException;
import com.example.Backend.exception.EmbeddingGenerationException;
import com.example.Backend.model.Document;
import com.example.Backend.model.DocumentStatus;
import com.example.Backend.model.EmbeddingStatus;
import com.example.Backend.repository.DocumentChunkRepository;
import com.example.Backend.repository.DocumentRepository;
import com.example.Backend.service.vector.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingGenerationService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;

    @Value("${embedding.batch-size:100}")
    private int batchSize;

    @Value("${openai.model.embedding}")
    private String embeddingModel;

    /**
     * Generates OpenAI embeddings for every chunk of the given document and
     * stores them in the in-memory FAISS index.
     *
     * @param documentId PostgreSQL document ID
     * @param regenerate When true, re-embed chunks that already have embeddings
     */
    public void generateEmbeddingsForDocument(Long documentId, boolean regenerate) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentProcessingException("Document not found: " + documentId));

        if (document.getStatus() != DocumentStatus.COMPLETED) {
            throw new DocumentProcessingException(
                    "Document must reach COMPLETED status before embedding. Current: " + document.getStatus());
        }

        List<DocumentChunk> chunks = documentChunkRepository
                .findByPostgresDocumentIdOrderByChunkIndex(documentId);

        if (chunks.isEmpty()) {
            throw new DocumentProcessingException("No chunks found for document: " + documentId);
        }

        List<DocumentChunk> toProcess = chunks.stream()
                .filter(c -> regenerate
                        || c.getEmbeddingStatus() == null
                        || c.getEmbeddingStatus() == EmbeddingStatus.PENDING
                        || c.getEmbeddingStatus() == EmbeddingStatus.FAILED)
                .toList();

        if (toProcess.isEmpty()) {
            log.info("All {} chunks already embedded for document [id={}]", chunks.size(), documentId);
            return;
        }

        log.info("Generating embeddings for {}/{} chunks [document id={}]",
                toProcess.size(), chunks.size(), documentId);

        // Process in batches to respect OpenAI token limits
        List<List<DocumentChunk>> batches = partition(toProcess, batchSize);
        for (int i = 0; i < batches.size(); i++) {
            log.info("Processing batch {}/{}", i + 1, batches.size());
            processBatch(batches.get(i));
        }

        log.info("Embedding generation complete for document [id={}]", documentId);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void processBatch(List<DocumentChunk> chunks) {
        // Mark in-progress so we don't double-process on restart
        chunks.forEach(c -> c.setEmbeddingStatus(EmbeddingStatus.PROCESSING));
        documentChunkRepository.saveAll(chunks);

        try {
            List<String> texts = chunks.stream().map(DocumentChunk::getContent).toList();
            List<float[]> embeddings = embeddingService.generateEmbeddingsBatch(texts);

            Map<String, float[]> vectorBatch = new HashMap<>();
            Map<String, Map<String, Object>> metaBatch = new HashMap<>();

            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk chunk = chunks.get(i);
                float[] vec = embeddings.get(i);

                chunk.setEmbedding(vec);
                chunk.setEmbeddingStatus(EmbeddingStatus.COMPLETED);
                chunk.setEmbeddingGeneratedAt(LocalDateTime.now());
                chunk.setEmbeddingModel(embeddingModel);

                vectorBatch.put(chunk.getId(), vec);
                metaBatch.put(chunk.getId(), buildChunkMeta(chunk));
            }

            documentChunkRepository.saveAll(chunks);
            vectorStoreService.addVectorsBatch(vectorBatch, metaBatch);

            log.info("Batch complete: {} chunks embedded and indexed", chunks.size());

        } catch (Exception ex) {
            chunks.forEach(c -> c.setEmbeddingStatus(EmbeddingStatus.FAILED));
            documentChunkRepository.saveAll(chunks);
            throw new EmbeddingGenerationException("Batch embedding failed", ex);
        }
    }

    /**
     * Builds the metadata map stored alongside each vector in the index.
     * The {@code content} key is critical: it lets SemanticSearchService
     * return chunk text without a secondary MongoDB lookup.
     */
    private Map<String, Object> buildChunkMeta(DocumentChunk chunk) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("chunkId", chunk.getId());
        meta.put("mongoDocumentId", chunk.getMongoDocumentId());
        meta.put("postgresDocumentId", chunk.getPostgresDocumentId());
        meta.put("chunkIndex", chunk.getChunkIndex());
        meta.put("content", chunk.getContent());
        return meta;
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> parts = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            parts.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return parts;
    }
}