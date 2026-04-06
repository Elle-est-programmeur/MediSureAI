package com.example.Backend.repository;

import com.example.Backend.document.DocumentContent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentContentRepository extends MongoRepository<DocumentContent, String> {

    Optional<DocumentContent> findByPostgresDocumentId(Long postgresDocumentId);

    void deleteByPostgresDocumentId(Long postgresDocumentId);
}