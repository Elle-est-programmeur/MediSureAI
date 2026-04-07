package com.example.Backend.service.tools;

import com.example.Backend.dto.SearchRequest;
import com.example.Backend.dto.SearchResponse;
import com.example.Backend.dto.ToolContext;
import com.example.Backend.dto.ToolResult;
import com.example.Backend.model.ToolType;
import com.example.Backend.service.vector.SemanticSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class VectorSearchTool implements Tool {

    private final SemanticSearchService semanticSearchService;

    @Override
    public ToolType getType() {
        return ToolType.VECTOR_SEARCH;
    }

    @Override
    public ToolResult execute(ToolContext context) {
        long startTime = System.currentTimeMillis();
        try {
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.setQuery(context.getQuery());
            searchRequest.setTopK((Integer) context.getParameters().getOrDefault("topK", 5));
            searchRequest.setDocumentId(context.getDocumentId());

            SearchResponse response = semanticSearchService.search(searchRequest);

            double avgSimilarity = response.getResults().stream()
                    .mapToDouble(r -> r.getSimilarity())
                    .average()
                    .orElse(0.0);

            return ToolResult.builder()
                    .toolType(ToolType.VECTOR_SEARCH)
                    .success(response.getResultsCount() > 0)
                    .content(response.getContext())
                    .confidence(avgSimilarity)
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();

        } catch (Exception ex) {
            log.error("Vector search tool failed: {}", ex.getMessage());
            return ToolResult.builder()
                    .toolType(ToolType.VECTOR_SEARCH)
                    .success(false)
                    .errorMessage(ex.getMessage())
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    @Override
    public boolean isApplicable(ToolContext context) {
        return true;
    }
}