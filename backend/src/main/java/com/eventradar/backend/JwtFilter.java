package com.eventradar.backend;

import com.eventradar.backend.model.User;
import com.eventradar.backend.model.RefreshToken;
import com.eventradar.backend.repository.UserRepository;
import com.eventradar.backend.service.RefreshTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String method = request.getMethod();
        String path = request.getServletPath();
        log.debug("[JWT-Filter] {} {} aufgerufen", method, path);
        String jwt = null;
        // 1. Versuche JWT aus Cookie zu lesen
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    jwt = cookie.getValue();
                }
            }
        }
        // 2. Fallback: JWT aus Authorization-Header
        final String authHeader = request.getHeader("Authorization");
        if (jwt == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            // Blockiere explizit API-Calls mit Authorization-Header, wenn es kein Non-Browser-Client ist
            if (!isNonBrowserClient(request)) {
                log.warn("[JWT-Filter] 403 für {} {}: Authorization-Header nicht erlaubt", method, path);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Authorization-Header nicht erlaubt. Bitte nutze Cookies.\"}");
                return;
            }
            jwt = authHeader.substring(7);
        }
        if (jwt != null) {
            try {
                String email = jwtUtil.extractEmail(jwt);
                Optional<User> userOpt = userRepository.findByEmail(email);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
                    org.springframework.security.core.userdetails.UserDetails userDetails =
                        org.springframework.security.core.userdetails.User
                            .withUsername(user.getEmail())
                            .password("") // Avoid storing bcrypt hash in SecurityContext
                            .authorities(authorities)
                            .build();
                    UsernamePasswordAuthenticationToken token =
                            new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(token);
                }
            } catch (io.jsonwebtoken.ExpiredJwtException ex) {
                String email = ex.getClaims().getSubject();
                log.warn("Expired JWT detected for {}", email);
                // Access-Token abgelaufen, versuche Refresh
                String refreshTokenValue = null;
                if (request.getCookies() != null) {
                    for (Cookie cookie : request.getCookies()) {
                        if ("refreshToken".equals(cookie.getName())) {
                            refreshTokenValue = cookie.getValue();
                        }
                    }
                }
                if (refreshTokenValue != null) {
                    Optional<RefreshToken> refreshTokenOpt = refreshTokenService.findByToken(refreshTokenValue);
                    if (refreshTokenOpt.isPresent() && !refreshTokenService.isExpired(refreshTokenOpt.get())) {
                        User user = refreshTokenOpt.get().getUser();
                        String newAccessToken = jwtUtil.generateToken(user.getEmail());
                        // Setze neuen Access-Token als Cookie
                        Cookie accessCookie = new Cookie("accessToken", newAccessToken);
                        accessCookie.setHttpOnly(true);
                        accessCookie.setSecure(true);
                        accessCookie.setPath("/");
                        accessCookie.setMaxAge(15 * 60); // 15 Minuten, synchron zu Login/Refresh
                        accessCookie.setAttribute("SameSite", "Lax");
                        response.addCookie(accessCookie);
                        // Setze neuen CSRF-Token als Cookie (wie beim Login/Refresh)
                        String csrfToken = java.util.UUID.randomUUID().toString();
                        Cookie csrfCookie = new Cookie("csrfToken", csrfToken);
                        csrfCookie.setPath("/");
                        csrfCookie.setHttpOnly(false);
                        csrfCookie.setSecure(true);
                        csrfCookie.setMaxAge(15 * 60);
                        csrfCookie.setAttribute("SameSite", "Strict");
                        response.addCookie(csrfCookie);
                        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
                        org.springframework.security.core.userdetails.UserDetails userDetails =
                            org.springframework.security.core.userdetails.User
                                .withUsername(user.getEmail())
                                .password("") // Avoid storing bcrypt hash in SecurityContext
                                .authorities(authorities)
                                .build();
                        UsernamePasswordAuthenticationToken token =
                                new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(token);
                    }
                }
            } catch (Exception e) {
                log.warn("[JWT-Filter] Exception für {} {}: {}", method, path, e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Erkenne Non-Browser-Clients (z.B. Postman, curl) anhand des User-Agent-Headers.
     */
    private boolean isNonBrowserClient(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        if (ua == null) return false;
        String uaLower = ua.toLowerCase();
        return uaLower.contains("postman") || uaLower.contains("curl") || uaLower.contains("httpie") || uaLower.contains("insomnia");
    }
}
