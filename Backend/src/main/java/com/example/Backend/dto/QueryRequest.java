package com.example.Backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QueryRequest {

    @NotBlank(message = "Query is required")
    private String query;

    /** Optional: restrict retrieval to a specific document. */
    private Long documentId;

    /** When true, response includes the full agent execution trace. */
    private Boolean includeSteps = false;

    /** Optional: session ID for conversation memory. Auto-generated if absent. */
    private String sessionId;

    /** Authenticated user's id — set server-side, not by clients. */
    private Long patientUserId;
}