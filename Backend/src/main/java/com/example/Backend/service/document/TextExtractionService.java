package com.example.Backend.service.document;

import com.example.Backend.exception.DocumentProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@Slf4j
public class TextExtractionService {

    private final Tika tika = new Tika();

    /**
     * Extracts plain text from a file using Apache Tika.
     * Supports PDF, DOCX, DOC, TXT, and most office formats.
     */
    public String extractText(Path filePath) {
        log.info("Extracting text from: {}", filePath.getFileName());

        try (InputStream stream = Files.newInputStream(filePath)) {
            String text = tika.parseToString(stream);
            log.info("Extracted {} characters from {}", text.length(), filePath.getFileName());
            return text;
        } catch (IOException | TikaException ex) {
            throw new DocumentProcessingException(
                    "Text extraction failed for: " + filePath.getFileName(), ex
            );
        }
    }

    /**
     * Detects the MIME type of a file using Tika's magic-byte detection.
     * More reliable than trusting the Content-Type header from the client.
     */
    public String detectMimeType(Path filePath) {
        try {
            return tika.detect(filePath.toFile());
        } catch (IOException ex) {
            throw new DocumentProcessingException("MIME type detection failed for: " + filePath.getFileName(), ex);
        }
    }
}