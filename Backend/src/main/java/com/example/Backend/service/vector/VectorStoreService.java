package com.example.Backend.service.vector;

import com.example.Backend.vector.SearchResult;
import com.example.Backend.vector.VectorIndex;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorStoreService {

    private final VectorIndex vectorIndex;

    public void addVector(String chunkId, float[] embedding, Map<String, Object> metadata) {
        vectorIndex.addVector(chunkId, embedding, metadata);
    }

    public void addVectorsBatch(Map<String, float[]> embeddings, Map<String, Map<String, Object>> metadata) {
        vectorIndex.addVectorsBatch(embeddings, metadata);
    }

    public List<SearchResult> search(float[] queryEmbedding, int topK) {
        return vectorIndex.search(queryEmbedding, topK);
    }

    public void deleteVector(String chunkId) {
        vectorIndex.deleteVector(chunkId);
    }

    public void deleteVectorsByDocument(Long documentId) {
        vectorIndex.deleteVectorsByDocument(documentId);
        log.info("Removed vectors for document [id={}] from index", documentId);
    }

    public int getIndexSize() {
        return vectorIndex.size();
    }

    public void clearIndex() {
        vectorIndex.clear();
        log.warn("Vector index cleared by admin request");
    }
}