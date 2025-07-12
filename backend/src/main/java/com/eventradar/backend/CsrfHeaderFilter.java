package com.eventradar.backend;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class CsrfHeaderFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(CsrfHeaderFilter.class);
    @Value("${app.csrf.secret:change_this_secret}")
    private String csrfSecret;

    private String generateCsrfToken(String userId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(csrfSecret.getBytes(), "HmacSHA256");
            mac.init(secretKey);
            byte[] hmac = mac.doFinal(userId.getBytes());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmac);
        } catch (Exception e) {
            throw new RuntimeException("CSRF HMAC error", e);
        }
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String method = request.getMethod();
        String path = request.getServletPath();
        log.debug("[CSRF-Filter] {} {} aufgerufen", method, path);
        if (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT") || method.equalsIgnoreCase("DELETE")) {
            String csrfHeader = request.getHeader("X-CSRF-Token");
            String csrfCookie = null;
            String userId = null;
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if ("csrfToken".equals(cookie.getName())) {
                        csrfCookie = cookie.getValue();
                    }
                    if ("accessToken".equals(cookie.getName())) {
                        try {
                            userId = new JwtUtil().extractEmail(cookie.getValue());
                        } catch (Exception ignored) {}
                    }
                }
            }
            boolean valid = false;
            if (csrfCookie != null && csrfHeader != null && userId != null) {
                String expected = generateCsrfToken(userId);
                valid = csrfCookie.equals(csrfHeader) && csrfCookie.equals(expected);
            }
            if (!valid) {
                log.warn("[CSRF-Filter] 403 für {} {}: CSRF-Token ungültig oder fehlt", method, path);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"CSRF-Token ungültig oder fehlt\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/auth/login")
            || path.startsWith("/api/auth/register")
            || path.startsWith("/api/auth/refresh");
    }
}
