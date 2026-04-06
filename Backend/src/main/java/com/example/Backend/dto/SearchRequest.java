package com.example.Backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SearchRequest {

    @NotBlank(message = "Query is required")
    private String query;

    @Min(value = 1, message = "topK must be at least 1")
    @Max(value = 20, message = "topK cannot exceed 20")
    private Integer topK = 5;

    /** Optional: restrict search to chunks belonging to this document. */
    private Long documentId;
}