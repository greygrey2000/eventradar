package com.eventradar.backend.service;

import com.eventradar.backend.model.User;
import com.eventradar.backend.model.UserEventInteraction;
import com.eventradar.backend.repository.UserEventInteractionRepository;
import com.eventradar.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecommendationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserEventInteractionRepository interactionRepository;

    // Neu: Empfiehlt Ticketmaster-Event-IDs basierend auf User-Interaktionen
    public List<String> recommendForUser(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        List<UserEventInteraction> interactions = interactionRepository.findByUser(user);
        // Empfiehlt gelikte/saved Ticketmaster-Event-IDs
        return interactions.stream()
                .filter(i -> Boolean.TRUE.equals(i.getLiked()) || Boolean.TRUE.equals(i.getSaved()))
                .map(UserEventInteraction::getTicketmasterEventId)
                .distinct()
                .limit(10)
                .toList();
    }
}

// Diese Klasse ist obsolet, da Empfehlungen nicht mehr aus lokalen Events generiert werden.
// Lösche oder deaktiviere sie.
