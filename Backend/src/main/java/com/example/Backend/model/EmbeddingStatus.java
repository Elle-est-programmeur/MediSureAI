package com.example.Backend.model;

public enum EmbeddingStatus {
    PENDING,     // Chunk saved; embedding not yet generated
    PROCESSING,  // Embedding generation in progress
    COMPLETED,   // Embedding generated and stored in vector index
    FAILED       // Embedding generation failed (will retry on next trigger)
}