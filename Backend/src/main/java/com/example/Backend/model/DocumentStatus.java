package com.example.Backend.model;

public enum DocumentStatus {
    UPLOADED,    // File saved to disk, queued for processing
    PROCESSING,  // Text extraction in progress
    CHUNKING,    // Text chunking in progress
    COMPLETED,   // Fully processed and stored in MongoDB
    FAILED       // Processing failed (see errorMessage)
}