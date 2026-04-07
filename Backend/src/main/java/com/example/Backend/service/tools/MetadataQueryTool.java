package com.example.Backend.service.tools;

import com.example.Backend.dto.ToolContext;
import com.example.Backend.dto.ToolResult;
import com.example.Backend.model.Document;
import com.example.Backend.model.QueryIntent;
import com.example.Backend.model.ToolType;
import com.example.Backend.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class MetadataQueryTool implements Tool {

    private final DocumentRepository documentRepository;

    @Override
    public ToolType getType() {
        return ToolType.METADATA_QUERY;
    }

    @Override
    public ToolResult execute(ToolContext context) {
        long startTime = System.currentTimeMillis();
        try {
            List<Document> documents;
            if (context.getDocumentId() != null) {
                documents = documentRepository.findById(context.getDocumentId())
                        .map(List::of)
                        .orElse(List.of());
            } else {
                documents = documentRepository.findAll();
            }

            String metadata = documents.stream()
                    .map(doc -> String.format(
                            "Document: %s\nType: %s\nUploaded: %s\nStatus: %s",
                            doc.getOriginalFileName(),
                            doc.getDocumentType(),
                            doc.getUploadedAt(),
                            doc.getStatus()))
                    .collect(Collectors.joining("\n\n"));

            return ToolResult.builder()
                    .toolType(ToolType.METADATA_QUERY)
                    .success(!documents.isEmpty())
                    .content(metadata)
                    .confidence(1.0)
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();

        } catch (Exception ex) {
            log.error("Metadata query tool failed: {}", ex.getMessage());
            return ToolResult.builder()
                    .toolType(ToolType.METADATA_QUERY)
                    .success(false)
                    .errorMessage(ex.getMessage())
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    @Override
    public boolean isApplicable(ToolContext context) {
        return context.getIntent() == QueryIntent.POLICY_DETAILS
                || context.getDocumentId() != null;
    }
}