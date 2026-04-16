package com.example.Backend.service.messaging;

import com.example.Backend.service.document.DocumentProcessingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentMessageConsumer {

    private final DocumentProcessingService documentProcessingService;

    /**
     * Full pipeline triggered by a single RabbitMQ message:
     *   1. Tika text extraction  → MongoDB DocumentContent
     *   2. Text chunking         → MongoDB DocumentChunk
     *   3. OpenAI embedding      → VectorIndex (FAISS)
     *
     * Embedding failures are caught separately so a bad OpenAI key doesn't
     * roll back an otherwise successful extraction + chunking.
     */
    @RabbitListener(queues = "${rabbitmq.queue.document-processing}")
    public void consume(Map<String, Object> message) {
        Long documentId = null;
        try {
            documentId = ((Number) message.get("documentId")).longValue();
            log.info("Received processing request for document [id={}]", documentId);

            // Phase 2: extract text + chunk into MongoDB + Vectorize into PGVector
            documentProcessingService.processDocument(documentId);
            log.info("Document processing and vectorization complete for document [id={}]", documentId);

        } catch (Throwable ex) {
            log.error("Failed to process document [id={}]: {}", documentId, ex.getMessage(), ex);
        }
    }
}
