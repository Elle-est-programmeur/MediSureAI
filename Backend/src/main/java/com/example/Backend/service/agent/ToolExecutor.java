package com.example.Backend.service.agent;

import com.example.Backend.dto.SearchRequest;
import com.example.Backend.dto.SearchResponse;
import com.example.Backend.service.vector.SemanticSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ToolExecutor {

    private final SemanticSearchService semanticSearchService;

    /**
     * Executes a semantic (vector) search and returns the concatenated chunk
     * context string ready for inclusion in an LLM prompt.
     *
     * @param query      Natural-language query to embed and search
     * @param parameters Map from the task plan (e.g. {@code {"topK": 5}})
     * @param documentId Optional — restrict search to a single document
     * @return Concatenated chunk text, or empty string if no results found
     */
    public String executeVectorSearch(String query, Map<String, Object> parameters, Long documentId) {
        int topK = parameters.containsKey("topK")
                ? ((Number) parameters.get("topK")).intValue()
                : 5;

        SearchRequest request = new SearchRequest();
        request.setQuery(query);
        request.setTopK(topK);
        request.setDocumentId(documentId);
        
        if (parameters.containsKey("documentType")) {
            request.setFilterExpression("documentType == '" + parameters.get("documentType") + "'");
        }

        log.info("Executing vector search [topK={}, documentId={}]", topK, documentId);

        try {
            SearchResponse response = semanticSearchService.search(request);
            log.info("Vector search found {} results", response.getResultsCount());
            return response.getContext();
        } catch (Exception ex) {
            log.error("Vector search failed: {}", ex.getMessage(), ex);
            return "";
        }
    }

    // Phase 5: SQL tool will be added here for structured metadata queries
}