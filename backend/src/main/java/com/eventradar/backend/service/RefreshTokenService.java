package com.eventradar.backend.service;

import com.eventradar.backend.JwtUtil;
import com.eventradar.backend.model.RefreshToken;
import com.eventradar.backend.model.User;
import com.eventradar.backend.repository.RefreshTokenRepository;
import com.eventradar.backend.repository.UserRepository;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    @Autowired
    private JwtUtil jwtUtil;

    @Value("${jwt.refresh.expiration.ms:604800000}") // 7 Tage
    private long refreshTokenDurationMs;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public String createRefreshToken(User user) {
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());
        String hashedToken = hashToken(refreshToken);
        RefreshToken tokenEntity = new RefreshToken();
        tokenEntity.setUser(user);
        tokenEntity.setToken(hashedToken);
        tokenEntity.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshTokenRepository.save(tokenEntity);
        return refreshToken; // Nur das Original-Token an den Client senden
    }

    public Optional<RefreshToken> findByToken(String token) {
        // Validierung des JWT-RefreshTokens
        try {
            String email = jwtUtil.extractEmailFromRefreshToken(token);
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                String hashedToken = hashToken(token);
                return refreshTokenRepository.findByToken(hashedToken)
                        .filter(dbTok -> MessageDigest.isEqual(dbTok.getToken().getBytes(StandardCharsets.UTF_8),
                                                               hashedToken.getBytes(StandardCharsets.UTF_8)));
            }
        } catch (Exception e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    public boolean isExpired(RefreshToken token) {
        return token.getExpiryDate().isBefore(Instant.now());
    }
    @Transactional
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
