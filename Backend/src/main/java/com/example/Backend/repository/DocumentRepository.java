package com.example.Backend.repository;

import com.example.Backend.model.Document;
import com.example.Backend.model.DocumentStatus;
import com.example.Backend.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByUser(Users user);

    List<Document> findByUserAndStatus(Users user, DocumentStatus status);

    List<Document> findByStatus(DocumentStatus status);

    Optional<Document> findByMongoDocumentId(String mongoDocumentId);

    List<Document> findByStatusOrderByUploadedAtDesc(DocumentStatus status);

    List<Document> findAllByOrderByUploadedAtDesc();
}