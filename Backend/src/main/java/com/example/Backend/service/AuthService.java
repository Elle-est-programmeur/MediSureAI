package com.example.Backend.service;

import com.example.Backend.dto.AuthResponse;
import com.example.Backend.dto.LoginRequest;
import com.example.Backend.dto.RegisterRequest;
import com.example.Backend.dto.TokenRefreshRequest;
import com.example.Backend.exception.TokenRefreshException;
import com.example.Backend.exception.UserAlreadyExistsException;
import com.example.Backend.model.RefreshToken;
import com.example.Backend.model.Users;
import com.example.Backend.repository.RefreshTokenRepository;
import com.example.Backend.repository.UserCredRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    @Autowired
    private UserCredRepo userCredRepo;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private BCryptPasswordEncoder encoder;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userCredRepo.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already taken: " + request.getUsername());
        }
        if (userCredRepo.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered: " + request.getEmail());
        }

        Users user = Users.builder()
                .username(request.getUsername())
                .password(encoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role(request.getRole())
                .build();

        userCredRepo.save(user);

        String accessToken = jwtService.generateToken(user.getUsername());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        Users user = userCredRepo.findByUsername(request.getUsername())
                .orElseThrow();

        String accessToken = jwtService.generateToken(user.getUsername());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(TokenRefreshRequest request) {
        String requestToken = request.getRefreshToken();

        RefreshToken refreshToken = refreshTokenRepository.findByToken(requestToken)
                .orElseThrow(() -> new TokenRefreshException(requestToken, "Refresh token not found"));

        refreshTokenService.verifyExpiration(refreshToken);

        Users user = refreshToken.getUser();
        String newAccessToken = jwtService.generateToken(user.getUsername());

        // Rotate refresh token
        refreshTokenRepository.delete(refreshToken);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getUsername());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    @Transactional
    public void logout(String username) {
        refreshTokenService.deleteByUsername(username);
    }
}