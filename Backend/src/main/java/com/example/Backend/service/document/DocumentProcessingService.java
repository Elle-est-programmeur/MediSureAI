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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

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

    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword"
    );

    // ─────────────────────────────────────────────────────────────────────────
    // UPLOAD: persists metadata to PostgreSQL, enqueues async processing
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public DocumentUploadResponse uploadDocument(MultipartFile file,
                                                  DocumentType documentType,
                                                  Users user) {
        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new UnsupportedFileTypeException(
                    "Unsupported file type: " + mimeType + ". Allowed: PDF, DOCX, DOC"
            );
        }

        // Persist file to disk
        String storedName = fileStorageService.storeFile(file);
        Path filePath = fileStorageService.getFilePath(storedName);

        // Create PostgreSQL metadata record
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

        // Enqueue for async Tika extraction + MongoDB storage
        messageProducer.sendForProcessing(saved.getId());

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

    // ─────────────────────────────────────────────────────────────────────────
    // PROCESS: called by RabbitMQ consumer — extract, chunk, store in MongoDB
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void processDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentProcessingException("Document not found: " + documentId));

        try {
            // Step 1: mark as PROCESSING
            document.setStatus(DocumentStatus.PROCESSING);
            documentRepository.save(document);

            // Step 2: extract text with Apache Tika
            Path filePath = Path.of(document.getFilePath());
            String extractedText = textExtractionService.extractText(filePath);

            // Step 3: persist full text in MongoDB
            DocumentContent content = DocumentContent.builder()
                    .postgresDocumentId(document.getId())
                    .fullText(extractedText)
                    .extractedBy("APACHE_TIKA")
                    .characterCount(extractedText.length())
                    .extractedAt(LocalDateTime.now())
                    .build();

            DocumentContent savedContent = documentContentRepository.save(content);

            // Step 4: update PostgreSQL with MongoDB reference, mark CHUNKING
            document.setMongoDocumentId(savedContent.getId());
            document.setStatus(DocumentStatus.CHUNKING);
            documentRepository.save(document);

            // Step 5: chunk text and store chunks in MongoDB
            List<DocumentChunk> chunks = chunkingService.chunkText(
                    extractedText,
                    savedContent.getId(),
                    document.getId()
            );
            documentChunkRepository.saveAll(chunks);

            // Step 6: mark COMPLETED
            document.setStatus(DocumentStatus.COMPLETED);
            document.setChunkCount(chunks.size());
            document.setProcessedAt(LocalDateTime.now());
            documentRepository.save(document);

            log.info("Document [id={}] processed successfully → {} chunks", documentId, chunks.size());

        } catch (Exception ex) {
            log.error("Document [id={}] processing failed: {}", documentId, ex.getMessage(), ex);
            document.setStatus(DocumentStatus.FAILED);
            document.setErrorMessage(truncate(ex.getMessage(), 900));
            documentRepository.save(document);
            throw new DocumentProcessingException("Processing failed for document: " + documentId, ex);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // QUERIES
    // ─────────────────────────────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void deleteDocument(Long documentId, Users user) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentProcessingException("Document not found: " + documentId));

        assertOwnership(document, user);

        // Clean up disk, MongoDB chunks, MongoDB content, then PostgreSQL row
        fileStorageService.deleteFile(document.getFileName());
        documentChunkRepository.deleteByPostgresDocumentId(documentId);
        documentContentRepository.deleteByPostgresDocumentId(documentId);
        documentRepository.delete(document);

        log.info("Document [id={}] deleted by user [{}]", documentId, user.getUsername());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

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
                .errorMessage(document.getErrorMessage())
                .build();
    }

    private String truncate(String message, int maxLen) {
        if (message == null) return null;
        return message.length() <= maxLen ? message : message.substring(0, maxLen);
    }
}