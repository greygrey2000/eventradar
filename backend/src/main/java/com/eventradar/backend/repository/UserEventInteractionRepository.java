package com.eventradar.backend.repository;

import com.eventradar.backend.model.UserEventInteraction;
import com.eventradar.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserEventInteractionRepository extends JpaRepository<UserEventInteraction, Long> {
    List<UserEventInteraction> findByUser(User user);
    Optional<UserEventInteraction> findByUserAndTicketmasterEventId(User user, String ticketmasterEventId);
    List<UserEventInteraction> findAllByTicketmasterEventId(String ticketmasterEventId);
    long countByTicketmasterEventIdAndLikedTrue(String ticketmasterEventId);
}
