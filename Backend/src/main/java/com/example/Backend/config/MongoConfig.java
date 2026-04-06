package com.example.Backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * MongoDB configuration.
 * Repository scanning is left to Spring Boot auto-configuration, which correctly
 * distinguishes JpaRepository from MongoRepository by interface type — no need
 * for an explicit @EnableMongoRepositories that would conflict with the JPA repos
 * living in the same package.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
    // Connection URI is read from application.properties:
    // spring.data.mongodb.uri=mongodb://localhost:27017/medisure_documents
}