package com.example.Backend.vector;

import com.example.Backend.config.VectorDatabaseConfig;
import com.example.Backend.exception.VectorStoreException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory FAISS-style vector index backed by file persistence.
 *
 * Stores float[] embeddings with their metadata in thread-safe maps.
 * On shutdown (@PreDestroy) the maps are serialized to disk so the index
 * survives application restarts.  On startup (@PostConstruct) they are
 * deserialized back.
 *
 * Phase 4/5 can swap this out for the actual FAISS JNI bindings or
 * pgvector without changing the VectorStoreService API.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VectorIndex {

    private static final String INDEX_FILE = "vector_index.dat";

    private final VectorDatabaseConfig config;

    // chunkId → embedding
    private final Map<String, float[]> vectorStore = new ConcurrentHashMap<>();
    // chunkId → metadata (content, postgresDocumentId, chunkIndex, …)
    private final Map<String, Map<String, Object>> metadataStore = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @PostConstruct
    public void initialize() {
        loadFromDisk();
        log.info("Vector index ready ({} vectors loaded)", vectorStore.size());
    }

    @PreDestroy
    public void saveToDisk() {
        Path file = Paths.get(config.getIndexPath(), INDEX_FILE);
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file.toFile()))) {
            oos.writeObject(new HashMap<>(vectorStore));   // ConcurrentHashMap → HashMap for serialization
            oos.writeObject(new HashMap<>(metadataStore));
            log.info("Vector index persisted ({} vectors → {})", vectorStore.size(), file);
        } catch (IOException e) {
            log.error("Failed to persist vector index to disk", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Write operations
    // ─────────────────────────────────────────────────────────────────────────

    public void addVector(String chunkId, float[] embedding, Map<String, Object> metadata) {
        validateDimension(embedding, "addVector");
        vectorStore.put(chunkId, embedding);
        metadataStore.put(chunkId, metadata);
        log.debug("Indexed vector for chunk [{}]", chunkId);
    }

    public void addVectorsBatch(Map<String, float[]> embeddings, Map<String, Map<String, Object>> metadata) {
        embeddings.forEach((chunkId, embedding) -> addVector(chunkId, embedding, metadata.get(chunkId)));
        log.info("Batch-indexed {} vectors (total: {})", embeddings.size(), vectorStore.size());
    }

    public void deleteVector(String chunkId) {
        vectorStore.remove(chunkId);
        metadataStore.remove(chunkId);
    }

    public void deleteVectorsByDocument(Long documentId) {
        List<String> targets = new ArrayList<>();
        metadataStore.forEach((chunkId, meta) -> {
            if (documentId.equals(meta.get("postgresDocumentId"))) {
                targets.add(chunkId);
            }
        });
        targets.forEach(this::deleteVector);
        log.info("Removed {} vectors for document [id={}]", targets.size(), documentId);
    }

    public void clear() {
        vectorStore.clear();
        metadataStore.clear();
        log.info("Vector index cleared");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Search
    // ─────────────────────────────────────────────────────────────────────────

    public List<SearchResult> search(float[] queryEmbedding, int topK) {
        validateDimension(queryEmbedding, "search");

        if (vectorStore.isEmpty()) {
            log.warn("Vector index is empty — upload and embed documents first");
            return List.of();
        }

        List<SearchResult> results = new ArrayList<>();

        for (Map.Entry<String, float[]> entry : vectorStore.entrySet()) {
            double sim = cosineSimilarity(queryEmbedding, entry.getValue());
            if (sim >= config.getSimilarityThreshold()) {
                results.add(SearchResult.builder()
                        .chunkId(entry.getKey())
                        .similarity(sim)
                        .metadata(metadataStore.get(entry.getKey()))
                        .build());
            }
        }

        results.sort(Comparator.comparingDouble(SearchResult::getSimilarity).reversed());
        return results.subList(0, Math.min(topK, results.size()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public int size() {
        return vectorStore.size();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void loadFromDisk() {
        Path file = Paths.get(config.getIndexPath(), INDEX_FILE);
        if (!Files.exists(file)) {
            log.info("No persisted vector index found — starting fresh");
            return;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file.toFile()))) {
            Map<String, float[]> vectors = (Map<String, float[]>) ois.readObject();
            Map<String, Map<String, Object>> metadata = (Map<String, Map<String, Object>>) ois.readObject();
            vectorStore.putAll(vectors);
            metadataStore.putAll(metadata);
            log.info("Loaded {} vectors from disk", vectorStore.size());
        } catch (IOException | ClassNotFoundException e) {
            log.error("Failed to load persisted vector index — starting fresh", e);
        }
    }

    private void validateDimension(float[] embedding, String operation) {
        if (embedding.length != config.getDimension()) {
            throw new VectorStoreException(String.format(
                    "[%s] Dimension mismatch: expected %d, got %d",
                    operation, config.getDimension(), embedding.length));
        }
    }

    /**
     * Cosine similarity ∈ [-1, 1].  Returns 0 for zero-norm vectors.
     */
    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot  += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        return (normA == 0 || normB == 0) ? 0.0 : dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}