package com.example.Backend.service.document;

import com.example.Backend.document.DocumentChunk;
import com.example.Backend.document.DocumentContent;
import com.example.Backend.dto.DocumentMetadataDTO;
import com.example.Backend.dto.DocumentUploadResponse;
import com.example.Backend.exception.DocumentProcessingException;
import com.example.Backend.exception.UnsupportedFileTypeException;
import com.example.Backend.model.Document;
import com.example.Backend.model.DocumentStatus;
import com.example.Backend.model.DocumentType;
import com.example.Backend.model.Users;
import com.example.Backend.repository.DocumentChunkRepository;
import com.example.Backend.repository.DocumentContentRepository;
import com.example.Backend.repository.DocumentRepository;
import com.example.Backend.service.messaging.DocumentMessageProducer;
import com.example.Backend.service.llm.LLMService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentProcessingService {

    private final DocumentRepository documentRepository;
    private final DocumentContentRepository documentContentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final FileStorageService fileStorageService;
    private final TextExtractionService textExtractionService;
    private final ChunkingService chunkingService;
    private final DocumentMessageProducer messageProducer;
    private final LLMService llmService;
    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword",
            "text/plain",
            "text/markdown"
    );

    @Transactional
    public DocumentUploadResponse uploadDocument(MultipartFile file,
                                                  DocumentType documentType,
                                                  Users user) {
        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new UnsupportedFileTypeException(
                    "Unsupported file type: " + mimeType + ". Allowed: " + String.join(", ", ALLOWED_MIME_TYPES)
            );
        }

        String storedName = fileStorageService.storeFile(file);
        Path filePath = fileStorageService.getFilePath(storedName);

        Document document = Document.builder()
                .user(user)
                .fileName(storedName)
                .originalFileName(file.getOriginalFilename())
                .filePath(filePath.toString())
                .contentType(mimeType)
                .fileSize(file.getSize())
                .documentType(documentType)
                .status(DocumentStatus.UPLOADED)
                .mongoDocumentId("pending")
                .uploadedAt(LocalDateTime.now())
                .build();

        Document saved = documentRepository.save(document);
        log.info("Document metadata saved [id={}] → enqueuing for processing", saved.getId());

        // Publish the processing message only AFTER this transaction commits.
        // Otherwise the (very fast) RabbitMQ consumer can call findById() before the
        // INSERT is visible and fail with "Document not found", stranding the upload.
        final Long documentId = saved.getId();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    messageProducer.sendForProcessing(documentId);
                }
            });
        } else {
            messageProducer.sendForProcessing(documentId);
        }

        return DocumentUploadResponse.builder()
                .documentId(saved.getId())
                .fileName(file.getOriginalFilename())
                .documentType(documentType)
                .status(DocumentStatus.UPLOADED)
                .fileSize(file.getSize())
                .uploadedAt(saved.getUploadedAt())
                .message("Document uploaded successfully. Processing started asynchronously.")
                .build();
    }

    @Transactional
    public void processDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentProcessingException("Document not found: " + documentId));

        try {
            document.setStatus(DocumentStatus.PROCESSING);
            documentRepository.save(document);

            Path filePath = Path.of(document.getFilePath());
            String extractedText = textExtractionService.extractText(filePath);

            DocumentContent content = DocumentContent.builder()
                    .postgresDocumentId(document.getId())
                    .fullText(extractedText)
                    .extractedBy("APACHE_TIKA")
                    .characterCount(extractedText.length())
                    .extractedAt(LocalDateTime.now())
                    .build();

            DocumentContent savedContent = documentContentRepository.save(content);

            document.setMongoDocumentId(savedContent.getId());
            document.setStatus(DocumentStatus.CHUNKING);
            documentRepository.save(document);

            List<DocumentChunk> chunks = chunkingService.chunkText(
                    extractedText,
                    savedContent.getId(),
                    document.getId()
            );
            documentChunkRepository.saveAll(chunks);

            log.info("Starting date extraction for document [{}] using LLM", documentId);
            // Step 5.1: Extract Event Date from first chunk if not already set
            if (document.getEventDate() == null && !chunks.isEmpty()) {
                try {
                    String snippet = chunks.get(0).getContent();
                    log.debug("Using text snippet for date extraction: {}", 
                            snippet.substring(0, Math.min(50, snippet.length())) + "...");
                            
                    String datePrompt = String.format("""
                            Extract the primary medical/service date from this document snippet. 
                            Return ONLY the date in YYYY-MM-DD format. 
                            If no date is found, return 'NOT_FOUND'.
                            
                            Snippet:
                            %s
                            """, snippet.substring(0, Math.min(1000, snippet.length())));
                    
                    String dateStr = llmService.generateCompletion(datePrompt);
                    if (dateStr == null) {
                        log.warn("LLM returned null for date extraction on document {}", documentId);
                    } else {
                        dateStr = dateStr.trim();
                        log.info("LLM date extraction raw result: [{}]", dateStr);
                        
                        // Robust extraction: find YYYY-MM-DD anywhere in the response
                        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
                        java.util.regex.Matcher matcher = pattern.matcher(dateStr);
                        
                        if (matcher.find()) {
                            String cleanDate = matcher.group();
                            log.info("Successfully parsed event date: {}", cleanDate);
                            document.setEventDate(java.time.LocalDate.parse(cleanDate).atStartOfDay());
                        } else {
                            log.warn("No valid YYYY-MM-DD date found in LLM response for document {}", documentId);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to extract date from document {}: {}", documentId, e.getMessage());
                }
            } else {
                log.info("Skipping date extraction: eventDate already set or no chunks available.");
            }

            List<org.springframework.ai.document.Document> aiDocs = chunks.stream().map(chunk -> {
                Map<String, Object> meta = new HashMap<>(chunk.getMetadata() != null ? chunk.getMetadata() : new HashMap<>());
                meta.put("postgresDocumentId", chunk.getPostgresDocumentId());
                meta.put("document_id", chunk.getPostgresDocumentId().toString());
                meta.put("fileName", document.getOriginalFileName() != null ? document.getOriginalFileName() : document.getFileName());
                meta.put("documentType", document.getDocumentType() != null ? document.getDocumentType().name() : "OTHER");
                return new org.springframework.ai.document.Document(chunk.getContent(), meta);
            }).toList();
            
            if (!aiDocs.isEmpty()) {
                vectorStore.add(aiDocs);
                log.info("Document [id={}] embedded and stored {} chunks in VectorStore", documentId, aiDocs.size());
            }

            document.setStatus(DocumentStatus.COMPLETED);
            document.setChunkCount(chunks.size());
            document.setProcessedAt(LocalDateTime.now());
            documentRepository.save(document);

            log.info("Document [id={}] processed successfully → {} chunks", documentId, chunks.size());

        } catch (Throwable ex) {
            log.error("Document [id={}] processing failed: {}", documentId, ex.getMessage(), ex);
            document.setStatus(DocumentStatus.FAILED);
            document.setErrorMessage(truncate(ex.getMessage(), 900));
            documentRepository.save(document);
            throw new DocumentProcessingException("Processing failed for document: " + documentId, ex);
        }
    }

    public List<DocumentMetadataDTO> getUserDocuments(Users user) {
        return documentRepository.findByUser(user)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public DocumentMetadataDTO getDocumentMetadata(Long documentId, Users user) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentProcessingException("Document not found: " + documentId));

        assertOwnership(document, user);
        return toDTO(document);
    }

    @Transactional
    public void deleteDocument(Long documentId, Users user) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentProcessingException("Document not found: " + documentId));

        assertOwnership(document, user);

        fileStorageService.deleteFile(document.getFileName());
        documentChunkRepository.deleteByPostgresDocumentId(documentId);
        documentContentRepository.deleteByPostgresDocumentId(documentId);
        documentRepository.delete(document);

        log.info("Document [id={}] deleted by user [{}]", documentId, user.getUsername());
    }

    private void assertOwnership(Document document, Users user) {
        if (document.getUser() == null || !document.getUser().getId().equals(user.getId())) {
            throw new DocumentProcessingException(
                    "Access denied: document " + document.getId() + " does not belong to this user"
            );
        }
    }

    private DocumentMetadataDTO toDTO(Document document) {
        return DocumentMetadataDTO.builder()
                .id(document.getId())
                .fileName(document.getOriginalFileName() != null
                        ? document.getOriginalFileName()
                        : document.getFileName())
                .documentType(document.getDocumentType())
                .status(document.getStatus())
                .fileSize(document.getFileSize())
                .chunkCount(document.getChunkCount())
                .uploadedAt(document.getUploadedAt())
                .processedAt(document.getProcessedAt())
                .eventDate(document.getEventDate())
                .errorMessage(document.getErrorMessage())
                .build();
    }

    @Transactional
    public void clearUserDocuments(Users user) {
        log.info("Clearing all documents and vectors for user: {}", user.getUsername());
        
        List<Document> userDocs = documentRepository.findByUser(user);
        if (userDocs.isEmpty()) {
            return;
        }

        List<String> docIds = userDocs.stream()
                .map(d -> d.getId().toString())
                .toList();

        for (String id : docIds) {
            jdbcTemplate.update("DELETE FROM vector_store WHERE metadata->>'document_id' = ?", id);
        }
        log.info("Cleared user vectors from vector_store");

        for (Document doc : userDocs) {
            documentChunkRepository.deleteByPostgresDocumentId(doc.getId());
            if (doc.getMongoDocumentId() != null && !doc.getMongoDocumentId().equals("pending")) {
                documentContentRepository.deleteById(doc.getMongoDocumentId());
            }
        }
        log.info("Cleared user data from MongoDB");

        documentRepository.deleteAll(userDocs);
        log.info("Cleared user metadata from PostgreSQL");
    }

    private String truncate(String message, int maxLen) {
        if (message == null) return null;
        return message.length() <= maxLen ? message : message.substring(0, maxLen);
    }
}
