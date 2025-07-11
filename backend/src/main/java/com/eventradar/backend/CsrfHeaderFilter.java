package com.eventradar.backend;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class CsrfHeaderFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(CsrfHeaderFilter.class);

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String method = request.getMethod();
        String path = request.getServletPath();
        log.debug("[CSRF-Filter] {} {} aufgerufen", method, path);
        // Nur für unsafe Methoden prüfen
        if (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT") || method.equalsIgnoreCase("DELETE")) {
            String csrfHeader = request.getHeader("X-CSRF-Token");
            String csrfCookie = null;
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if ("csrfToken".equals(cookie.getName())) {
                        csrfCookie = cookie.getValue();
                        break;
                    }
                }
            }
            boolean valid = csrfCookie != null && csrfHeader != null &&
                    MessageDigest.isEqual(csrfCookie.getBytes(StandardCharsets.UTF_8),
                                          csrfHeader.getBytes(StandardCharsets.UTF_8));
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
