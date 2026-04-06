package com.example.Backend.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Getter
@Slf4j
public class VectorDatabaseConfig {

    @Value("${vector.faiss.index-path}")
    private String indexPath;

    @Value("${vector.faiss.dimension}")
    private int dimension;

    @Value("${vector.search.top-k:5}")
    private int topK;

    @Value("${vector.search.similarity-threshold:0.7}")
    private double similarityThreshold;

    @PostConstruct
    public void init() {
        Path dir = Paths.get(indexPath);
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
                log.info("Created FAISS index directory: {}", indexPath);
            } catch (IOException e) {
                throw new RuntimeException("Could not create FAISS index directory: " + indexPath, e);
            }
        } else {
            log.info("FAISS index directory ready: {}", indexPath);
        }
    }
}