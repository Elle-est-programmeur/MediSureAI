package com.example.Backend.service.document;

import com.example.Backend.config.FileStorageConfig;
import com.example.Backend.exception.FileStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final FileStorageConfig fileStorageConfig;

    public String storeFile(MultipartFile file) {
        String originalFileName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown"
        );

        if (originalFileName.contains("..")) {
            throw new FileStorageException("Invalid file path sequence in filename: " + originalFileName);
        }

        String extension = getFileExtension(originalFileName);
        String uniqueFileName = UUID.randomUUID() + extension;

        try {
            Path targetLocation = Paths.get(fileStorageConfig.getUploadDirectory()).resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored file: {} → {}", originalFileName, uniqueFileName);
            return uniqueFileName;
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file: " + originalFileName, ex);
        }
    }

    public void deleteFile(String fileName) {
        try {
            Path filePath = Paths.get(fileStorageConfig.getUploadDirectory()).resolve(fileName);
            Files.deleteIfExists(filePath);
            log.info("Deleted file: {}", fileName);
        } catch (IOException ex) {
            log.error("Failed to delete file: {}", fileName, ex);
        }
    }

    public Path getFilePath(String fileName) {
        return Paths.get(fileStorageConfig.getUploadDirectory()).resolve(fileName);
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot == -1 ? "" : fileName.substring(lastDot);
    }
}