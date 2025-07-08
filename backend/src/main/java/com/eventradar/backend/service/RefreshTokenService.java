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

    public String createRefreshToken(User user) {
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());
        RefreshToken tokenEntity = new RefreshToken();
        tokenEntity.setUser(user);
        tokenEntity.setToken(refreshToken);
        tokenEntity.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshTokenRepository.save(tokenEntity);
        return refreshToken;
    }

    public Optional<RefreshToken> findByToken(String token) {
        // Validierung des JWT-RefreshTokens
        try {
            String email = jwtUtil.extractEmailFromRefreshToken(token);
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                return refreshTokenRepository.findByToken(token);
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
