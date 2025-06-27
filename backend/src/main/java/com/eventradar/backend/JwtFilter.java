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

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
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
        if (jwt == null) {
            final String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwt = authHeader.substring(7);
            }
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
                            .password(user.getPassword())
                            .authorities(authorities)
                            .build();
                    UsernamePasswordAuthenticationToken token =
                            new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(token);
                }
            } catch (io.jsonwebtoken.ExpiredJwtException ex) {
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
                        accessCookie.setMaxAge(15 * 60);
                        accessCookie.setAttribute("SameSite", "Lax");
                        response.addCookie(accessCookie);
                        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
                        org.springframework.security.core.userdetails.UserDetails userDetails =
                            org.springframework.security.core.userdetails.User
                                .withUsername(user.getEmail())
                                .password(user.getPassword())
                                .authorities(authorities)
                                .build();
                        UsernamePasswordAuthenticationToken token =
                                new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(token);
                    }
                }
            } catch (Exception ignored) {}
        }
        filterChain.doFilter(request, response);
    }
}
