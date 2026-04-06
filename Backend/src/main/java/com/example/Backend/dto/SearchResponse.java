package com.example.Backend.dto;

import com.example.Backend.vector.SearchResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {
    private String query;
    private int resultsCount;
    private List<SearchResult> results;
    private String context;        // Concatenated chunk text — ready for LLM prompt
    private long searchTimeMs;
}