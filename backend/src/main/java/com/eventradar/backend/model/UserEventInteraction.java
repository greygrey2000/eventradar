package com.eventradar.backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class UserEventInteraction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User user;

    @ManyToOne(optional = false)
    private Event event;

    private Boolean liked;
    private Boolean saved;
    private Boolean ignored;
    private Integer rating;
}
