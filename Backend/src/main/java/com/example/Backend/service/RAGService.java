package com.example.Backend.service;

import com.example.Backend.dto.QuestionRequest;
import com.example.Backend.dto.QuestionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RAGService {

    private final VectorStore vectorStore;
    private final ChatModel chatModel;

    private static final String PROMPT_TEMPLATE = """
            You are a helpful AI assistant. Answer the user's question based on the context provided below.
            If the context doesn't contain enough information to answer the question, say so.
            
            Context:
            {context}
            
            Question: {question}
            
            Answer:
            """;

    public QuestionResponse askQuestion(QuestionRequest request) {
        log.info("Processing question: {}", request.getQuestion());

        try {
            // Search for relevant documents in vector store
            SearchRequest searchRequest = SearchRequest.defaults()
                .withQuery(request.getQuestion())
                .withTopK(request.getTopK());
            
            List<Document> relevantDocuments = vectorStore.similaritySearch(searchRequest);
            
            if (relevantDocuments.isEmpty()) {
                return QuestionResponse.builder()
                    .question(request.getQuestion())
                    .answer("I don't have any relevant information to answer this question. Please upload relevant documents first.")
                    .relevantChunks(List.of())
                    .documentsUsed(0)
                    .build();
            }

            // Extract context from documents
            String context = relevantDocuments.stream()
                .map(doc -> doc.getContent())
                .collect(Collectors.joining("\n\n"));

            // Get relevant chunks for response
            List<String> chunks = relevantDocuments.stream()
                .limit(3)
                .map(doc -> {
                    String content = doc.getContent();
                    return content.substring(0, Math.min(200, content.length())) + "...";
                })
                .collect(Collectors.toList());

            // Count unique documents
            Set<String> uniqueDocIds = new HashSet<>();
            for (Document doc : relevantDocuments) {
                Object docId = doc.getMetadata().get("document_id");
                if (docId != null) {
                    uniqueDocIds.add(docId.toString());
                }
            }

            // Create prompt with context
            PromptTemplate promptTemplate = new PromptTemplate(PROMPT_TEMPLATE);
            Prompt prompt = promptTemplate.create(Map.of(
                "context", context,
                "question", request.getQuestion()
            ));

            // Get answer from LLM
            String answer = chatModel.call(prompt).getResult().getOutput().getContent();

            log.info("Successfully generated answer for question");

            return QuestionResponse.builder()
                .question(request.getQuestion())
                .answer(answer)
                .relevantChunks(chunks)
                .documentsUsed(uniqueDocIds.size())
                .build();

        } catch (Exception e) {
            log.error("Error processing question: {}", e.getMessage(), e);
            return QuestionResponse.builder()
                .question(request.getQuestion())
                .answer("Sorry, I encountered an error while processing your question: " + e.getMessage())
                .relevantChunks(List.of())
                .documentsUsed(0)
                .build();
        }
    }
}
