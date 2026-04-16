package com.example.Backend.service;

import com.example.Backend.dto.DocumentUploadResponse;
import com.example.Backend.model.Document;
import com.example.Backend.model.DocumentStatus;
import com.example.Backend.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Legacy RAG document service — uses Spring AI's PDF reader + pgvector embeddings.
 * Retained as the Phase 3 bridge: once Ollama is running this pipeline re-activates.
 * Phase 2 document processing (Tika + MongoDB) is handled by DocumentProcessingService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final VectorStore vectorStore;
    private final DocumentRepository documentRepository;
    private final Tika tika = new Tika();

    public DocumentUploadResponse processDocument(MultipartFile file) {
        log.info("Processing document via RAG pipeline: {}", file.getOriginalFilename());

        Document document = Document.builder()
                .fileName(file.getOriginalFilename())
                .originalFileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .uploadedAt(LocalDateTime.now())
                .status(DocumentStatus.PROCESSING)
                .chunkCount(0)
                .build();

        document = documentRepository.save(document);

        try {
            Path tempFile = Files.createTempFile("upload-", file.getOriginalFilename());
            file.transferTo(tempFile.toFile());

            List<org.springframework.ai.document.Document> documents;
            if ("application/pdf".equals(file.getContentType())) {
                documents = processPdf(tempFile);
            } else {
                documents = processText(tempFile);
            }

            TokenTextSplitter splitter = new TokenTextSplitter();
            List<org.springframework.ai.document.Document> chunks = splitter.apply(documents);

            for (org.springframework.ai.document.Document chunk : chunks) {
                chunk.getMetadata().put("document_id", document.getId().toString());
                chunk.getMetadata().put("file_name", document.getFileName());
                chunk.getMetadata().put("upload_time", document.getUploadedAt().toString());
            }

            vectorStore.add(chunks);

            document.setStatus(DocumentStatus.COMPLETED);
            document.setChunkCount(chunks.size());
            documentRepository.save(document);

            Files.deleteIfExists(tempFile);
            log.info("RAG pipeline processed {} → {} chunks", file.getOriginalFilename(), chunks.size());

            return DocumentUploadResponse.builder()
                    .documentId(document.getId())
                    .fileName(document.getFileName())
                    .status(DocumentStatus.COMPLETED)
                    .chunkCount(chunks.size())
                    .message("Document processed and indexed in vector store")
                    .build();

        } catch (Exception e) {
            log.error("RAG pipeline failed for {}: {}", file.getOriginalFilename(), e.getMessage(), e);
            document.setStatus(DocumentStatus.FAILED);
            document.setErrorMessage(e.getMessage());
            documentRepository.save(document);

            return DocumentUploadResponse.builder()
                    .documentId(document.getId())
                    .fileName(document.getFileName())
                    .status(DocumentStatus.FAILED)
                    .chunkCount(0)
                    .message("Processing error: " + e.getMessage())
                    .build();
        }
    }

    public List<Document> getAllDocuments() {
        return documentRepository.findAllByOrderByUploadedAtDesc();
    }

    public List<Document> getCompletedDocuments() {
        return documentRepository.findByStatusOrderByUploadedAtDesc(DocumentStatus.COMPLETED);
    }

    private List<org.springframework.ai.document.Document> processPdf(Path filePath) {
        try {
            String content = tika.parseToString(filePath);
            return List.of(new org.springframework.ai.document.Document(
                    content, Map.of("source", filePath.getFileName().toString())
            ));
        } catch (IOException | TikaException ex) {
            throw new RuntimeException("Failed to extract PDF content: " + filePath.getFileName(), ex);
        }
    }

    private List<org.springframework.ai.document.Document> processText(Path filePath) throws IOException {
        String content = Files.readString(filePath);
        return List.of(new org.springframework.ai.document.Document(
                content, Map.of("source", filePath.getFileName().toString())
        ));
    }
}
