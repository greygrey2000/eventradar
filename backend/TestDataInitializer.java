package de.neuefische.eventapp;

import de.neuefische.eventapp.event.EventRepository;
import de.neuefische.eventapp.interaction.UserEventInteractionRepository;
import de.neuefische.eventapp.user.User;
import de.neuefische.eventapp.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class TestDataInitializer {

    public CommandLineRunner initTestData(EventRepository eventRepo, UserEventInteractionRepository interactionRepo, UserRepository userRepo, PasswordEncoder encoder) {
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

            // Events und Interaktionen werden nicht mehr lokal gespeichert
        };
    }
}