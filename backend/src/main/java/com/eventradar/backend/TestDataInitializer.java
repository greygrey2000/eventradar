package com.eventradar.backend;

import com.eventradar.backend.model.Event;
import com.eventradar.backend.model.User;
import com.eventradar.backend.model.UserEventInteraction;
import com.eventradar.backend.repository.EventRepository;
import com.eventradar.backend.repository.UserEventInteractionRepository;
import com.eventradar.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Configuration
public class TestDataInitializer {
    @Bean
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

            // Events
            Event event1 = new Event();
            event1.setExternalId("evt-001");
            event1.setSource("eventbrite");
            event1.setName("Tech Conference 2025");
            event1.setDescription("Große Tech-Konferenz in Berlin");
            event1.setTags(Arrays.asList("Tech", "Conference"));
            event1.setDateTime(LocalDateTime.now().plusDays(10));
            event1.setLocation("Berlin");
            event1.setUrl("https://eventbrite.com/evt-001");
            event1.setImageUrl("https://img.com/evt-001.jpg");
            event1.setImportedAt(java.sql.Timestamp.valueOf(LocalDateTime.now()));

            Event event2 = new Event();
            event2.setExternalId("evt-002");
            event2.setSource("meetup");
            event2.setName("Art & Food Festival");
            event2.setDescription("Kunst und Kulinarik in Hamburg");
            event2.setTags(Arrays.asList("Art", "Food"));
            event2.setDateTime(LocalDateTime.now().plusDays(20));
            event2.setLocation("Hamburg");
            event2.setUrl("https://meetup.com/evt-002");
            event2.setImageUrl("https://img.com/evt-002.jpg");
            event2.setImportedAt(java.sql.Timestamp.valueOf(LocalDateTime.now()));

            eventRepo.saveAll(List.of(event1, event2));

            // Interactions
            UserEventInteraction i1 = new UserEventInteraction();
            i1.setUser(user1);
            i1.setEvent(event1);
            i1.setLiked(true);
            i1.setSaved(true);
            i1.setIgnored(false);
            i1.setRating(5);

            UserEventInteraction i2 = new UserEventInteraction();
            i2.setUser(user1);
            i2.setEvent(event2);
            i2.setLiked(false);
            i2.setSaved(false);
            i2.setIgnored(true);
            i2.setRating(null);

            UserEventInteraction i3 = new UserEventInteraction();
            i3.setUser(user2);
            i3.setEvent(event1);
            i3.setLiked(true);
            i3.setSaved(false);
            i3.setIgnored(false);
            i3.setRating(4);

            UserEventInteraction i4 = new UserEventInteraction();
            i4.setUser(user2);
            i4.setEvent(event2);
            i4.setLiked(true);
            i4.setSaved(true);
            i4.setIgnored(false);
            i4.setRating(5);

            interactionRepo.saveAll(List.of(i1, i2, i3, i4));
        };
    }
}
