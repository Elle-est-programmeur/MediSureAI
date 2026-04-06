package com.example.Backend.service.rag;

import com.example.Backend.dto.SearchRequest;
import com.example.Backend.dto.SearchResponse;
import com.example.Backend.service.vector.SemanticSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContextBuilderService {

    private final SemanticSearchService semanticSearchService;

    /**
     * Retrieves the top-k semantically similar chunks and concatenates their
     * text into a single context string ready for an LLM prompt.
     */
    public String buildContext(String query, Long documentId, int maxChunks) {
        SearchRequest req = new SearchRequest();
        req.setQuery(query);
        req.setTopK(maxChunks);
        req.setDocumentId(documentId);   // null = search across all documents

        SearchResponse response = semanticSearchService.search(req);
        log.debug("Built context from {} chunks for query '{}'", response.getResultsCount(), query);
        return response.getContext();
    }

    /**
     * Wraps a RAG context string + user question into a complete LLM prompt
     * that enforces grounded, citation-backed answers.
     */
    public String buildPromptWithContext(String userQuery, String context) {
        return """
                You are a helpful AI assistant specialized in healthcare insurance.

                Context from relevant documents:
                ---
                %s
                ---

                User Question: %s

                Instructions:
                - Answer ONLY from the context above.
                - If the answer is not in the context, say: "I don't have enough information to answer that."
                - Cite the specific part of the context that supports your answer.
                - Be concise and accurate.

                Answer:""".formatted(context, userQuery);
    }

    /**
     * Phase 4 hook — AgentOrchestrator will call this with a resolved intent
     * and structured task plan instead of a raw user query.
     */
    public String buildAgenticPrompt(String intent, String context, String userQuery) {
        // Phase 4: replace with intent-driven multi-step prompting
        return buildPromptWithContext(userQuery, context);
    }
}