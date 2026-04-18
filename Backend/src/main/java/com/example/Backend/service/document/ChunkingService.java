package com.example.Backend.service.document;

import com.example.Backend.document.DocumentChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ChunkingService {

    @Value("${document.chunking.max-chunk-size}")
    private int maxChunkSize;

    @Value("${document.chunking.overlap}")
    private int overlap;

    /**
     * Splits extracted text into overlapping chunks for vector storage.
     * Attempts to break at sentence boundaries (period or newline) to preserve context.
     */
    public List<DocumentChunk> chunkText(String text, String mongoDocumentId, Long postgresDocumentId) {
        List<DocumentChunk> chunks = new ArrayList<>();

        int textLength = text.length();
        int currentPos = 0;
        int chunkIndex = 0;

        while (currentPos < textLength) {
            int endPos = Math.min(currentPos + maxChunkSize, textLength);

            // Try to snap to a sentence boundary to avoid mid-sentence cuts
            if (endPos < textLength) {
                int lastPeriod = text.lastIndexOf('.', endPos);
                int lastNewline = text.lastIndexOf('\n', endPos);
                int boundary = Math.max(lastPeriod, lastNewline);

                if (boundary > currentPos + (maxChunkSize / 2)) {
                    // Only snap if we're at least halfway into the chunk
                    endPos = boundary + 1;
                }
            }

            String content = text.substring(currentPos, endPos).strip();

            if (!content.isEmpty()) {
                DocumentChunk chunk = DocumentChunk.builder()
                        .mongoDocumentId(mongoDocumentId)
                        .postgresDocumentId(postgresDocumentId)
                        .content(content)
                        .chunkIndex(chunkIndex)
                        .startPosition(currentPos)
                        .endPosition(endPos)
                        .metadata(buildChunkMetadata(chunkIndex, content))
                        .createdAt(LocalDateTime.now())
                        .build();

                chunks.add(chunk);
                chunkIndex++;
            }

            // Slide forward with overlap so adjacent chunks share context
            if (endPos >= textLength) {
                break; // Last chunk reached
            }
            
            int advance = endPos - currentPos - overlap;
            currentPos += Math.max(advance, 1);
        }

        log.info("Chunked text ({} chars) into {} chunks (maxSize={}, overlap={})",
                textLength, chunks.size(), maxChunkSize, overlap);
        return chunks;
    }

    private Map<String, Object> buildChunkMetadata(int index, String content) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("chunkIndex", index);
        meta.put("wordCount", content.split("\\s+").length);
        meta.put("characterCount", content.length());
        return meta;
    }
}