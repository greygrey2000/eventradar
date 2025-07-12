package com.eventradar.backend.service;

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
        // Erzeuge einen kryptographisch sicheren zufälligen Token (z.B. UUID + SecureRandom)
        String rawToken;
        try {
            rawToken = UUID.randomUUID().toString() + "-" + java.security.SecureRandom.getInstanceStrong().nextLong();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SecureRandom instance not available", e);
        }
        String hashedToken = hashToken(rawToken);
        // Optional: Alte Tokens für den User löschen (nur ein aktives Token pro User)
        refreshTokenRepository.deleteByUser(user);
        RefreshToken tokenEntity = new RefreshToken();
        tokenEntity.setUser(user);
        tokenEntity.setToken(hashedToken);
        tokenEntity.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshTokenRepository.save(tokenEntity);
        return rawToken; // Nur das Original-Token an den Client senden
    }

    public Optional<RefreshToken> findByToken(String rawToken) {
        String hashedToken = hashToken(rawToken);
        return refreshTokenRepository.findByToken(hashedToken)
                .filter(dbTok -> MessageDigest.isEqual(dbTok.getToken().getBytes(StandardCharsets.UTF_8),
                                                       hashedToken.getBytes(StandardCharsets.UTF_8)));
    }

    public boolean isExpired(RefreshToken token) {
        return token.getExpiryDate().isBefore(Instant.now());
    }
    @Transactional
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
