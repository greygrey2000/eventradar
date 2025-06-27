package com.example.demo.controller;

import com.example.demo.dto.UserProfileDTO;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(user -> {
                    // Erstelle ein DTO ohne Passwort
                    return ResponseEntity.ok(new UserProfileDTO(user.getId(), user.getName(), user.getEmail(), user.getLocation(), user.getInterests()));
                })
                .orElseGet(() -> ResponseEntity.status(404).body("User not found"));
    }
}