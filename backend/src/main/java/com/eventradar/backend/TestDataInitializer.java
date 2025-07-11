package com.eventradar.backend;

import com.eventradar.backend.model.User;
import com.eventradar.backend.model.UserEventInteraction;
import com.eventradar.backend.repository.UserEventInteractionRepository;
import com.eventradar.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;

@Configuration
public class TestDataInitializer {
    @Bean
    public CommandLineRunner initTestData(UserEventInteractionRepository interactionRepo, UserRepository userRepo, PasswordEncoder encoder) {
        return args -> {
            // Users
            User user1 = new User();
            user1.setName("Alice");
            user1.setEmail("alice@example.com");
            user1.setPassword(encoder.encode("alicepass"));
            user1.setLocation("Berlin");
            user1.setInterests(Arrays.asList("Tech", "Music"));

            User user2 = new User();
            user2.setName("Bob");
            user2.setEmail("bob@example.com");
            user2.setPassword(encoder.encode("bobpass"));
            user2.setLocation("Hamburg");
            user2.setInterests(Arrays.asList("Art", "Food"));

            userRepo.saveAll(List.of(user1, user2));

            // Beispiel für Interaktionen (optional)
            // UserEventInteraction i1 = new UserEventInteraction();
            // i1.setUser(user1);
            // i1.setLiked(true);
            // i1.setSaved(true);
            // i1.setIgnored(false);
            // i1.setRating(5);
            // interactionRepo.save(i1);
        };
    }
}
