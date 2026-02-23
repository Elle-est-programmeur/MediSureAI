package com.example.Backend.service;

import com.example.Backend.dto.DocumentUploadResponse;
import com.example.Backend.model.Document;
import com.example.Backend.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final VectorStore vectorStore;
    private final DocumentRepository documentRepository;

    public DocumentUploadResponse processDocument(MultipartFile file) {
        log.info("Processing document: {}", file.getOriginalFilename());
        
        Document document = Document.builder()
            .fileName(file.getOriginalFilename())
            .contentType(file.getContentType())
            .fileSize(file.getSize())
            .uploadedAt(LocalDateTime.now())
            .status("PROCESSING")
            .chunkCount(0)
            .build();
        
        document = documentRepository.save(document);

        try {
            // Save file temporarily
            Path tempFile = Files.createTempFile("upload-", file.getOriginalFilename());
            file.transferTo(tempFile.toFile());

            // Process based on file type
            List<org.springframework.ai.document.Document> documents;
            
            if (file.getContentType() != null && file.getContentType().equals("application/pdf")) {
                documents = processPdfDocument(tempFile);
            } else {
                documents = processTextDocument(tempFile);
            }

            // Split documents into chunks
            TokenTextSplitter splitter = new TokenTextSplitter();
            List<org.springframework.ai.document.Document> chunks = splitter.apply(documents);

            // Add metadata to each chunk
            for (org.springframework.ai.document.Document chunk : chunks) {
                chunk.getMetadata().put("document_id", document.getId().toString());
                chunk.getMetadata().put("file_name", document.getFileName());
                chunk.getMetadata().put("upload_time", document.getUploadedAt().toString());
            }

            // Store embeddings in vector store
            vectorStore.add(chunks);

            // Update document status
            document.setStatus("COMPLETED");
            document.setChunkCount(chunks.size());
            documentRepository.save(document);

            // Clean up temp file
            Files.deleteIfExists(tempFile);

            log.info("Successfully processed document {} with {} chunks", 
                file.getOriginalFilename(), chunks.size());

            return DocumentUploadResponse.builder()
                .documentId(document.getId())
                .fileName(document.getFileName())
                .status("COMPLETED")
                .chunkCount(chunks.size())
                .message("Document processed successfully")
                .build();

        } catch (Exception e) {
            log.error("Error processing document: {}", e.getMessage(), e);
            document.setStatus("FAILED");
            document.setErrorMessage(e.getMessage());
            documentRepository.save(document);

            return DocumentUploadResponse.builder()
                .documentId(document.getId())
                .fileName(document.getFileName())
                .status("FAILED")
                .chunkCount(0)
                .message("Error processing document: " + e.getMessage())
                .build();
        }
    }

    private List<org.springframework.ai.document.Document> processPdfDocument(Path filePath) {
        log.info("Processing PDF document");
        Resource resource = new FileSystemResource(filePath.toFile());
        DocumentReader reader = new PagePdfDocumentReader(resource);
        return reader.get();
    }

    private List<org.springframework.ai.document.Document> processTextDocument(Path filePath) throws IOException {
        log.info("Processing text document");
        String content = Files.readString(filePath);
        
        org.springframework.ai.document.Document doc = new org.springframework.ai.document.Document(
            content,
            Map.of("source", filePath.getFileName().toString())
        );
        
        return List.of(doc);
    }

    public List<Document> getAllDocuments() {
        return documentRepository.findAllByOrderByUploadedAtDesc();
    }

    public List<Document> getCompletedDocuments() {
        return documentRepository.findByStatusOrderByUploadedAtDesc("COMPLETED");
    }
}
