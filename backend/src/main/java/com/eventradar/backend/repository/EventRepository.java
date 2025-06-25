package com.eventradar.backend.repository;

import com.eventradar.backend.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByTagsContaining(String tag);
    List<Event> findByDateTimeAfter(java.time.LocalDateTime date);
    List<Event> findByTagsContainingAndDateTimeAfter(String tag, java.time.LocalDateTime date);
    List<Event> findByLocationContainingIgnoreCase(String location);
    Optional<Event> findByExternalIdAndSource(String externalId, String source);
}
