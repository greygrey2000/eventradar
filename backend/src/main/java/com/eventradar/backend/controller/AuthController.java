package com.eventradar.backend.controller;

import com.eventradar.backend.JwtUtil;
import com.eventradar.backend.dto.AuthRequest;
import com.eventradar.backend.dto.RegisterRequest;
import com.eventradar.backend.dto.AuthResponse;
import com.eventradar.backend.model.User;
import com.eventradar.backend.repository.UserRepository;
import com.eventradar.backend.service.RefreshTokenService;
import com.eventradar.backend.model.RefreshToken;
import com.eventradar.backend.dto.RefreshRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepo;
    @Autowired
    private PasswordEncoder encoder;
    @Autowired
    private AuthenticationManager authManager;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private RefreshTokenService refreshTokenService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepo.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Email already in use");
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(encoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setLocation(request.getLocation());
        user.setInterests(request.getInterests());
        userRepo.save(user);
        return ResponseEntity.ok("User registered");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request, HttpServletResponse response) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        User user = userRepo.findByEmail(request.getEmail()).orElseThrow();
        String accessToken = jwtUtil.generateToken(request.getEmail());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        // Setze Access-Token als HttpOnly-Cookie
        Cookie accessCookie = new Cookie("accessToken", accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(true); // nur über HTTPS
        accessCookie.setPath("/");
        accessCookie.setMaxAge(15 * 60); // 15 Minuten
        accessCookie.setAttribute("SameSite", "Lax");
        response.addCookie(accessCookie);

        // Setze Refresh-Token als HttpOnly-Cookie
        Cookie refreshCookie = new Cookie("refreshToken", refreshToken.getToken());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7 Tage
        refreshCookie.setAttribute("SameSite", "Strict");
        response.addCookie(refreshCookie);

        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken.getToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        // Lies Refresh-Token aus Cookie
        String refreshTokenValue = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshTokenValue = cookie.getValue();
                }
            }
        }
        if (refreshTokenValue == null) {
            return ResponseEntity.status(401).body((AuthResponse) null);
        }
        return refreshTokenService.findByToken(refreshTokenValue)
                .filter(token -> !refreshTokenService.isExpired(token))
                .map(token -> {
                    String newAccessToken = jwtUtil.generateToken(token.getUser().getEmail());
                    // Setze neuen Access-Token als Cookie
                    Cookie accessCookie = new Cookie("accessToken", newAccessToken);
                    accessCookie.setHttpOnly(true);
                    accessCookie.setSecure(true);
                    accessCookie.setPath("/");
                    accessCookie.setMaxAge(15 * 60);
                    accessCookie.setAttribute("SameSite", "Lax");
                    response.addCookie(accessCookie);
                    return ResponseEntity.ok(new AuthResponse(newAccessToken, token.getToken()));
                })
                .orElseGet(() -> ResponseEntity.status(401).body((AuthResponse) null));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        // Cookies löschen
        Cookie accessCookie = new Cookie("accessToken", null);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);
        accessCookie.setAttribute("SameSite", "Lax");
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie("refreshToken", null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);
        refreshCookie.setAttribute("SameSite", "Strict");
        response.addCookie(refreshCookie);

        // Optional: RefreshToken aus DB löschen
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            userRepo.findByEmail(email).ifPresent(refreshTokenService::deleteByUser);
        }
        return ResponseEntity.ok("Logged out");
    }
}
