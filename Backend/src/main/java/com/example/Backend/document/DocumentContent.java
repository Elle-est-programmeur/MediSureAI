package com.example.Backend.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "document_contents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentContent {

    @Id
    private String id;

    @Indexed
    private Long postgresDocumentId;

    private String fullText;

    private String extractedBy;

    private Integer characterCount;

    private LocalDateTime extractedAt;
}