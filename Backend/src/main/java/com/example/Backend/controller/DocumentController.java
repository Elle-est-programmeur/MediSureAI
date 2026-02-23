package com.example.Backend.controller;

import com.example.Backend.dto.DocumentUploadResponse;
import com.example.Backend.model.Document;
import com.example.Backend.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file) {
        
        log.info("Received upload request for file: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(DocumentUploadResponse.builder()
                    .status("FAILED")
                    .message("File is empty")
                    .build());
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || 
            (!contentType.equals("application/pdf") && 
             !contentType.startsWith("text/"))) {
            return ResponseEntity.badRequest()
                .body(DocumentUploadResponse.builder()
                    .status("FAILED")
                    .message("Only PDF and text files are supported")
                    .build());
        }

        DocumentUploadResponse response = documentService.processDocument(file);
        
        if ("FAILED".equals(response.getStatus())) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<Document>> getAllDocuments() {
        return ResponseEntity.ok(documentService.getAllDocuments());
    }

    @GetMapping("/completed")
    public ResponseEntity<List<Document>> getCompletedDocuments() {
        return ResponseEntity.ok(documentService.getCompletedDocuments());
    }
}
