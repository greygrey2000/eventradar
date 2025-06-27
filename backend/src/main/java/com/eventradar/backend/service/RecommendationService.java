package com.eventradar.backend.service;

import com.eventradar.backend.model.Event;
import com.eventradar.backend.model.User;
import com.eventradar.backend.model.UserEventInteraction;
import com.eventradar.backend.repository.EventRepository;
import com.eventradar.backend.repository.UserEventInteractionRepository;
import com.eventradar.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserEventInteractionRepository interactionRepository;

    public List<Event> recommendForUser(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();

        // Hol dir Interaktionen
        List<UserEventInteraction> interactions = interactionRepository.findByUser(user);

        // Extrahiere Tags aus gelikten Events
        Set<String> likedTags = interactions.stream()
                .filter(UserEventInteraction::getLiked)
                .flatMap(i -> i.getEvent().getTags().stream())
                .collect(Collectors.toSet());

        // Finde passende Events
        List<Event> matches = new ArrayList<>();
        for (String tag : likedTags) {
            matches.addAll(eventRepository.findByTagsContaining(tag));
        }

        return matches.stream().distinct().limit(10).toList();
    }
}
