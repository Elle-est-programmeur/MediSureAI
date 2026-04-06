package com.example.Backend.service.embedding;

import com.example.Backend.exception.EmbeddingGenerationException;
import com.theokanning.openai.embedding.Embedding;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final OpenAiService openAiService;

    @Value("${openai.model.embedding}")
    private String embeddingModel;

    /**
     * Generates a single embedding vector for the given text.
     * Retried up to 3 times with 2 s exponential back-off on any exception.
     */
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public float[] generateEmbedding(String text) {
        try {
            EmbeddingRequest request = EmbeddingRequest.builder()
                    .model(embeddingModel)
                    .input(List.of(text))
                    .build();

            List<Embedding> embeddings = openAiService.createEmbeddings(request).getData();

            if (embeddings.isEmpty()) {
                throw new EmbeddingGenerationException("OpenAI returned no embedding for the provided text");
            }

            return toFloatArray(embeddings.get(0).getEmbedding());

        } catch (EmbeddingGenerationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Embedding generation failed: {}", e.getMessage());
            throw new EmbeddingGenerationException("Embedding generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Batch-generates embeddings for multiple texts in a single API call.
     * More efficient than calling generateEmbedding() in a loop.
     */
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public List<float[]> generateEmbeddingsBatch(List<String> texts) {
        try {
            EmbeddingRequest request = EmbeddingRequest.builder()
                    .model(embeddingModel)
                    .input(texts)
                    .build();

            List<Embedding> embeddings = openAiService.createEmbeddings(request).getData();
            log.debug("Batch embedding: {} texts → {} results", texts.size(), embeddings.size());

            return embeddings.stream()
                    .map(e -> toFloatArray(e.getEmbedding()))
                    .toList();

        } catch (EmbeddingGenerationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Batch embedding generation failed: {}", e.getMessage());
            throw new EmbeddingGenerationException("Batch embedding failed: " + e.getMessage(), e);
        }
    }

    private float[] toFloatArray(List<Double> values) {
        float[] arr = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            arr[i] = values.get(i).floatValue();
        }
        return arr;
    }
}