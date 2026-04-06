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
public class FileStorageConfig {

    @Value("${document.upload.directory}")
    private String uploadDirectory;

    @Value("${document.upload.allowed-types}")
    private String allowedTypes;

    @PostConstruct
    public void init() {
        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            try {
                Files.createDirectories(uploadPath);
                log.info("Created upload directory: {}", uploadDirectory);
            } catch (IOException e) {
                throw new RuntimeException("Could not create upload directory: " + uploadDirectory, e);
            }
        }
    }
}