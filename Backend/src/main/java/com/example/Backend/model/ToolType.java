package com.example.Backend.model;

public enum ToolType {
    VECTOR_SEARCH,   // Semantic search on document chunks
    METADATA_QUERY,  // Query document metadata
    POLICY_LOOKUP,   // Direct policy clause lookup
    CLAIM_STATUS,    // Claim status check (future)
    EXTERNAL_API     // External data source (future)
}