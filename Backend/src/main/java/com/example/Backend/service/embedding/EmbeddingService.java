package com.example.Backend.service.embedding;

import com.example.Backend.exception.EmbeddingGenerationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Embedding service backed by Local Ollama (nomic-embed-text).
 * Provides efficient, local vector representations (768 dims).
 */
@Service
@Slf4j
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(@Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * Generates a single embedding vector for the given text using Ollama.
     * Retried up to 3 times with 2s exponential back-off on any exception.
     */
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public float[] generateEmbedding(String text) {
        try {
            EmbeddingRequest request = new EmbeddingRequest(List.of(text), null);
            EmbeddingResponse response = embeddingModel.call(request);

            if (response.getResults().isEmpty()) {
                throw new EmbeddingGenerationException("Ollama returned no embedding for the provided text");
            }

            float[] embedding = response.getResults().get(0).getOutput();
            log.debug("Generated embedding of dimension {}", embedding.length);
            return embedding;

        } catch (EmbeddingGenerationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Embedding generation failed: {}", e.getMessage());
            throw new EmbeddingGenerationException("Embedding generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Batch-generates embeddings for multiple texts in a single API call.
     */
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public List<float[]> generateEmbeddingsBatch(List<String> texts) {
        try {
            EmbeddingRequest request = new EmbeddingRequest(texts, null);
            EmbeddingResponse response = embeddingModel.call(request);

            log.debug("Batch embedding: {} texts → {} results", texts.size(), response.getResults().size());

            return response.getResults().stream()
                    .map(result -> result.getOutput())
                    .toList();

        } catch (EmbeddingGenerationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Batch embedding generation failed: {}", e.getMessage());
            throw new EmbeddingGenerationException("Batch embedding failed: " + e.getMessage(), e);
        }
    }
}