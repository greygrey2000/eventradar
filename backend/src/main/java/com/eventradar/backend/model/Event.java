package com.eventradar.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String externalId;
    private String source;
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    @ElementCollection
    private List<String> tags;
    private LocalDateTime dateTime;
    private String location;
    private String url;
    private String imageUrl;
    private java.sql.Timestamp importedAt;
}
