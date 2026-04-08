package com.example.Backend.service;

import com.example.Backend.exception.TokenRefreshException;
import com.example.Backend.model.RefreshToken;
import com.example.Backend.model.Users;
import com.example.Backend.repository.RefreshTokenRepository;
import com.example.Backend.repository.UserCredRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Value("${jwt.refresh.expiration}")
    private long refreshTokenExpiration;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserCredRepo userCredRepo;

    @Transactional
    public RefreshToken createRefreshToken(String username) {
        Users user = userCredRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        refreshTokenRepository.deleteByUser(user);
        refreshTokenRepository.flush();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenExpiration))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), "Refresh token has expired. Please login again.");
        }
        return token;
    }

    @Transactional
    public void deleteByUsername(String username) {
        userCredRepo.findByUsername(username).ifPresent(refreshTokenRepository::deleteByUser);
    }
}