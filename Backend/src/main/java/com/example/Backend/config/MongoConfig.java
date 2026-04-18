package com.example.Backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import com.example.Backend.repository.DocumentChunkRepository;
import com.example.Backend.repository.DocumentContentRepository;

@Configuration
@EnableMongoRepositories(basePackageClasses = {
    DocumentChunkRepository.class,
    DocumentContentRepository.class
})
public class MongoConfig {
}