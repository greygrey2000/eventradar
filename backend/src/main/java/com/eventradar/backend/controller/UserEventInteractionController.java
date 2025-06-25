package com.eventradar.backend.controller;

import com.eventradar.backend.model.UserEventInteraction;
import com.eventradar.backend.repository.UserEventInteractionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interactions")
public class UserEventInteractionController {
    @Autowired
    private UserEventInteractionRepository interactionRepository;

    @GetMapping
    public List<UserEventInteraction> getAllInteractions() {
        return interactionRepository.findAll();
    }

    @PostMapping
    public UserEventInteraction createInteraction(@RequestBody UserEventInteraction interaction) {
        return interactionRepository.save(interaction);
    }
}
