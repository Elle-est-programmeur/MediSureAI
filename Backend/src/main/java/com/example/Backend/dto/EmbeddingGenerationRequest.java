package com.example.Backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EmbeddingGenerationRequest {

    @NotNull(message = "documentId is required")
    private Long documentId;

    /** When true, regenerate embeddings even for chunks that already have them. */
    private Boolean regenerate = false;
}