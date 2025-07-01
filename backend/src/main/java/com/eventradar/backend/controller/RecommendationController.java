package com.eventradar.backend.controller;

import com.eventradar.backend.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<List<String>> getRecommendations(Authentication authentication) {
        String email = authentication.getName();
        List<String> recommended = recommendationService.recommendForUser(email);
        return ResponseEntity.ok(recommended);
    }
}
