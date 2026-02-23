package com.example.Backend.repository;

import com.example.Backend.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByStatusOrderByUploadedAtDesc(String status);
    List<Document> findAllByOrderByUploadedAtDesc();
}
