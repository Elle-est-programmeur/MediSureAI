package com.example.Backend.document;

import com.example.Backend.model.EmbeddingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "document_chunks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {

    @Id
    private String id;

    @Indexed
    private String mongoDocumentId;

    @Indexed
    private Long postgresDocumentId;

    private String content;

    private Integer chunkIndex;

    private Integer startPosition;

    private Integer endPosition;

    private Map<String, Object> metadata;

    private LocalDateTime createdAt;

    // ── Phase 3: Embedding fields ──────────────────────────────────────────

    @Indexed
    private EmbeddingStatus embeddingStatus;

    /** 1536-dimensional OpenAI text-embedding-3-small vector. */
    private float[] embedding;

    private LocalDateTime embeddingGeneratedAt;

    private String embeddingModel;
}