package com.example.Backend.vector;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {
    private String chunkId;
    private double similarity;
    private String content;           // Populated from vector metadata by the service layer
    private Map<String, Object> metadata;
}