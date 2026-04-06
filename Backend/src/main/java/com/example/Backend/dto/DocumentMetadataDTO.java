package com.example.Backend.dto;

import com.example.Backend.model.DocumentStatus;
import com.example.Backend.model.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentMetadataDTO {
    private Long id;
    private String fileName;
    private DocumentType documentType;
    private DocumentStatus status;
    private Long fileSize;
    private Integer chunkCount;
    private LocalDateTime uploadedAt;
    private LocalDateTime processedAt;
    private String errorMessage;
}