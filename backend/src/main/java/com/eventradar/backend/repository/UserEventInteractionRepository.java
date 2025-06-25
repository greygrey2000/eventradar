package com.eventradar.backend.repository;

import com.eventradar.backend.model.UserEventInteraction;
import com.eventradar.backend.model.User;
import com.eventradar.backend.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserEventInteractionRepository extends JpaRepository<UserEventInteraction, Long> {
    List<UserEventInteraction> findByUser(User user);
    Optional<UserEventInteraction> findByUserAndEvent(User user, Event event);
    List<UserEventInteraction> findAllByEventId(Long eventId);
    long countByEventIdAndLikedTrue(Long eventId);
}
