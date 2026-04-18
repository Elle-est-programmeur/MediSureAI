package com.example.Backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import com.example.Backend.repository.DocumentRepository;
import com.example.Backend.repository.RefreshTokenRepository;
import com.example.Backend.repository.UserCredRepo;

@Configuration
@EnableJpaRepositories(basePackageClasses = {
    DocumentRepository.class,
    RefreshTokenRepository.class,
    UserCredRepo.class
})
public class JpaConfig {
}
