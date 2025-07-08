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
import com.eventradar.backend.security.AuthRateLimiter;
import io.github.bucket4j.Bucket;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

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
    @Autowired
    private AuthRateLimiter authRateLimiter;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        Bucket bucket = authRateLimiter.resolveBucket(ip, "register");
        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(429).body("Zu viele Registrierungsversuche. Bitte warte einen Moment.");
        }
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
    public ResponseEntity<?> login(@RequestBody AuthRequest request, HttpServletResponse response, HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        Bucket bucket = authRateLimiter.resolveBucket(ip, "login");
        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(429).body("Zu viele Login-Versuche. Bitte warte einen Moment.");
        }
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        User user = userRepo.findByEmail(request.getEmail()).orElseThrow();
        log.info("User {} logged in", request.getEmail());
        String accessToken = jwtUtil.generateToken(request.getEmail());
        String refreshToken = refreshTokenService.createRefreshToken(user);

        // CSRF-Token generieren (z. B. UUID, in Produktion kryptographisch sicher!)
        String csrfToken = java.util.UUID.randomUUID().toString();
        Cookie csrfCookie = new Cookie("csrfToken", csrfToken);
        csrfCookie.setPath("/");
        csrfCookie.setHttpOnly(false); // Muss im Frontend lesbar sein
        csrfCookie.setSecure(true); // Nur über HTTPS
        csrfCookie.setMaxAge(15 * 60); // 15 Minuten, synchron zu AccessToken
        csrfCookie.setAttribute("SameSite", "Strict");
        response.addCookie(csrfCookie);

        // accessToken und refreshToken als HttpOnly-Cookies setzen (optional)
        Cookie accessCookie = new Cookie("accessToken", accessToken);
        accessCookie.setPath("/");
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(true);
        accessCookie.setMaxAge(15 * 60); // 15 Minuten, synchron zu CSRF
        accessCookie.setAttribute("SameSite", "Lax");
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setPath("/");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setMaxAge(7 * 24 * 60 * 60);
        refreshCookie.setAttribute("SameSite", "Lax");
        response.addCookie(refreshCookie);

        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken));
    }

    private boolean isValidCsrf(HttpServletRequest request) {
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
        return csrfCookie != null && csrfHeader != null && csrfCookie.equals(csrfHeader);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        String ip = request.getRemoteAddr();
        Bucket bucket = authRateLimiter.resolveBucket(ip, "refresh");
        if (!bucket.tryConsume(1)) {
            response.setContentType("application/json");
            try {
                response.getWriter().write("{\"error\": \"Zu viele Token-Refresh-Versuche. Bitte warte einen Moment.\"}");
            } catch (Exception ignored) {}
            return ResponseEntity.status(429).body("Zu viele Token-Refresh-Versuche. Bitte warte einen Moment.");
        }
        if (!isValidCsrf(request)) {
            response.setContentType("application/json");
            try {
                response.getWriter().write("{\"error\": \"CSRF-Token ungültig oder fehlt\"}");
            } catch (Exception ignored) {}
            return ResponseEntity.status(403).body("CSRF-Token ungültig oder fehlt");
        }
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
                    // Refresh Token Rotation: alten Token löschen, neuen generieren
                    refreshTokenService.deleteByUser(token.getUser());
                    String newRefreshToken = refreshTokenService.createRefreshToken(token.getUser());
                    String newAccessToken = jwtUtil.generateToken(token.getUser().getEmail());
                    // Setze neuen Access-Token als Cookie
                    Cookie accessCookie = new Cookie("accessToken", newAccessToken);
                    accessCookie.setHttpOnly(true);
                    accessCookie.setSecure(true);
                    accessCookie.setPath("/");
                    accessCookie.setMaxAge(15 * 60);
                    accessCookie.setAttribute("SameSite", "Lax");
                    response.addCookie(accessCookie);
                    // Setze neuen Refresh-Token als Cookie
                    Cookie refreshCookie = new Cookie("refreshToken", newRefreshToken);
                    refreshCookie.setHttpOnly(true);
                    refreshCookie.setSecure(true);
                    refreshCookie.setPath("/");
                    refreshCookie.setMaxAge(7 * 24 * 60 * 60);
                    refreshCookie.setAttribute("SameSite", "Lax");
                    response.addCookie(refreshCookie);
                    return ResponseEntity.ok(new AuthResponse(newAccessToken, newRefreshToken));
                })
                .orElseGet(() -> ResponseEntity.status(401).body((AuthResponse) null));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (!isValidCsrf(request)) {
            response.setContentType("application/json");
            try {
                response.getWriter().write("{\"error\": \"CSRF-Token ungültig oder fehlt\"}");
            } catch (Exception ignored) {}
            return ResponseEntity.status(403).body("CSRF-Token ungültig oder fehlt");
        }

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
        refreshCookie.setAttribute("SameSite", "Lax");
        response.addCookie(refreshCookie);

        // Optional: RefreshToken aus DB löschen
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            userRepo.findByEmail(email).ifPresent(refreshTokenService::deleteByUser);
        }
        return ResponseEntity.ok("Logged out");
    }
}
