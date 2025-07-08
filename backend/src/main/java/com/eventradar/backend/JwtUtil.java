package com.eventradar.backend;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.access.secret}")
    private String accessSecret;

    @Value("${jwt.refresh.secret}")
    private String refreshSecret;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.audience}")
    private String audience;

    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuer(issuer)
                .setAudience(audience)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 15 * 60 * 1000)) // 15 Minuten
                .signWith(getAccessKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractEmail(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getAccessKey())
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public String generateRefreshToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuer(issuer)
                .setAudience(audience)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000)) // 7 Tage
                .signWith(getRefreshKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractEmailFromRefreshToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getRefreshKey())
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    private Key getAccessKey() {
        byte[] key = accessSecret.getBytes(StandardCharsets.UTF_8);
        if (key.length < 32) // 256 bit für HS256
            throw new IllegalStateException("jwt.access.secret too short");
        return Keys.hmacShaKeyFor(key);
    }

    private Key getRefreshKey() {
        byte[] key = refreshSecret.getBytes(StandardCharsets.UTF_8);
        if (key.length < 32)
            throw new IllegalStateException("jwt.refresh.secret too short");
        return Keys.hmacShaKeyFor(key);
    }
}
