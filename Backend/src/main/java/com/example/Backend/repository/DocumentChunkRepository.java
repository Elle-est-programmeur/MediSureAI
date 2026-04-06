package com.example.Backend.repository;

import com.example.Backend.document.DocumentChunk;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends MongoRepository<DocumentChunk, String> {

    List<DocumentChunk> findByMongoDocumentIdOrderByChunkIndex(String mongoDocumentId);

    List<DocumentChunk> findByPostgresDocumentIdOrderByChunkIndex(Long postgresDocumentId);

    long countByPostgresDocumentId(Long postgresDocumentId);

    void deleteByMongoDocumentId(String mongoDocumentId);

    void deleteByPostgresDocumentId(Long postgresDocumentId);
}