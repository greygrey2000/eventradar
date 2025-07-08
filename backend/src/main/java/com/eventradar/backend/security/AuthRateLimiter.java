package com.eventradar.backend.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthRateLimiter {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private static final int LOGIN_LIMIT = 5; // 5 Versuche
    private static final int REFRESH_LIMIT = 10; // 10 Versuche
    private static final int REGISTER_LIMIT = 3; // 3 Versuche
    private static final Duration DURATION = Duration.ofMinutes(1);

    public Bucket resolveBucket(String key, String type) {
        return buckets.computeIfAbsent(key + ":" + type, k -> {
            int limit = switch (type) {
                case "login" -> LOGIN_LIMIT;
                case "refresh" -> REFRESH_LIMIT;
                case "register" -> REGISTER_LIMIT;
                default -> 5;
            };
            Bandwidth bandwidth = Bandwidth.builder()
                    .capacity(limit)
                    .refillIntervally(limit, DURATION)
                    .build();
            return Bucket.builder()
                    .addLimit(bandwidth)
                    .build();
        });
    }
}
