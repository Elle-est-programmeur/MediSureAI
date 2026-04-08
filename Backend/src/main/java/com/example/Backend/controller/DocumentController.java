package com.example.Backend.controller;

import com.example.Backend.dto.DocumentMetadataDTO;
import com.example.Backend.dto.DocumentUploadResponse;
import com.example.Backend.model.DocumentType;
import com.example.Backend.model.Users;
import com.example.Backend.repository.UserCredRepo;
import com.example.Backend.service.document.DocumentProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final DocumentProcessingService documentProcessingService;
    private final UserCredRepo userCredRepo;

    /**
     * Upload a PDF or DOCX file. Processing (Tika extraction + MongoDB chunking)
     * happens asynchronously via RabbitMQ. Returns immediately with UPLOADED status.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<DocumentUploadResponse>> uploadFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("documentType") DocumentType documentType,
            Authentication authentication) {

        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body(List.of(
                    DocumentUploadResponse.builder()
                            .status(com.example.Backend.model.DocumentStatus.FAILED)
                            .message("No files were uploaded")
                            .build()
            ));
        }

        Users user = resolveUser(authentication);
        List<DocumentUploadResponse> responses = new ArrayList<>();

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                log.info("Upload request: {} [{}] by {}", file.getOriginalFilename(), documentType, user.getUsername());
                try {
                    DocumentUploadResponse res = documentProcessingService.uploadDocument(file, documentType, user);
                    responses.add(res);
                } catch (Exception e) {
                    log.error("Failed to enqueue document {}", file.getOriginalFilename(), e);
                    responses.add(DocumentUploadResponse.builder()
                            .fileName(file.getOriginalFilename())
                            .status(com.example.Backend.model.DocumentStatus.FAILED)
                            .message(e.getMessage())
                            .build());
                }
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * List all documents belonging to the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<DocumentMetadataDTO>> getAllDocuments(Authentication authentication) {
        Users user = resolveUser(authentication);
        return ResponseEntity.ok(documentProcessingService.getUserDocuments(user));
    }

    /**
     * Get metadata for a single document (must belong to the caller).
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentMetadataDTO> getDocument(
            @PathVariable Long id,
            Authentication authentication) {

        Users user = resolveUser(authentication);
        return ResponseEntity.ok(documentProcessingService.getDocumentMetadata(id, user));
    }

    /**
     * Delete a document and all its associated data (disk + MongoDB + PostgreSQL).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteDocument(
            @PathVariable Long id,
            Authentication authentication) {

        Users user = resolveUser(authentication);
        documentProcessingService.deleteDocument(id, user);
        return ResponseEntity.ok(Map.of("message", "Document deleted successfully"));
    }

    /**
     * Clears all documents and vectors for the current user.
     * Used for session-based analysis where data should be forgotten on reload.
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, String>> clearSession(Authentication authentication) {
        Users user = resolveUser(authentication);
        log.info("Clear session request by user: {}", user.getUsername());
        
        documentProcessingService.clearUserDocuments(user);
        
        return ResponseEntity.ok(Map.of("message", "Session cleared successfully"));
    }

    private Users resolveUser(Authentication authentication) {
        return userCredRepo.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database"));
    }
}