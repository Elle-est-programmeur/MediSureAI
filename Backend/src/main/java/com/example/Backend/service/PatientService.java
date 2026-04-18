package com.example.Backend.service;

import com.example.Backend.dto.*;
import com.example.Backend.model.Document;
import com.example.Backend.model.DocumentType;
import com.example.Backend.model.QueryIntent;
import com.example.Backend.model.Users;
import com.example.Backend.repository.DocumentRepository;
import com.example.Backend.service.llm.LLMService;
import com.example.Backend.service.srlm.MultiReasoningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientService {

    private final VectorStore vectorStore;
    private final DocumentRepository documentRepository;
    private final LLMService llmService;
    private final MultiReasoningService multiReasoningService;

    /**
     * Feature 2: Health Timeline
     */
    public List<DocumentMetadataDTO> getTimeline(Users user) {
        return documentRepository.findByUser(user).stream()
                .filter(doc -> doc.getEventDate() != null)
                .sorted(Comparator.comparing(Document::getEventDate).reversed())
                .map(this::toDTO)
                .toList();
    }

    /**
     * Feature 4: Smart Formulary Search
     */
    public SRLMResponse searchFormulary(String drugName, Users user) {
        log.info("Searching formulary for drug: {}", drugName);

        List<org.springframework.ai.document.Document> policyDocs = vectorStore.similaritySearch(
                SearchRequest.query(drugName)
                        .withTopK(5)
                        .withFilterExpression("documentType == 'INSURANCE_POLICY'")
        );

        String context = policyDocs.stream().map(org.springframework.ai.document.Document::getContent).collect(Collectors.joining("\n"));

        String prompt = String.format("""
                [CRITICAL INSTRUCTION: USE PROVIDED CONTEXT ONLY]
                You are a technical Medical Insurance Policy Auditor. Your task is to extract exact coverage details for the drug '%s' from the provided policy snippets.
                
                RULES:
                1. Do NOT provide generic medical advice or "Bronze/Silver/Gold" tier guesses.
                2. If the drug is not mentioned in the context, state "Drug not found in policy documents."
                3. Use the exact wording from the text for co-pays and tiers.
                4. Do NOT start with a medical advice disclaimer unless you are actually giving medical advice (which you should not do).
                
                DETERMINE:
                - Coverage Status: Is it mentioned as covered?
                - Cost Tier & Co-pay: (e.g., Tier 1, $10)
                - Generic Alternatives: List any generics mentioned in the same section.
                
                CONTEXT FROM POLICY:
                ---
                %s
                ---
                """, drugName, context);

        String answer = llmService.generateCompletion(prompt);

        return SRLMResponse.builder()
                .query("Drug search: " + drugName)
                .finalAnswer(answer)
                .build();
    }

    private DocumentMetadataDTO toDTO(Document document) {
        return DocumentMetadataDTO.builder()
                .id(document.getId())
                .fileName(document.getOriginalFileName())
                .documentType(document.getDocumentType())
                .status(document.getStatus())
                .eventDate(document.getEventDate())
                .uploadedAt(document.getUploadedAt())
                .build();
    }
}
