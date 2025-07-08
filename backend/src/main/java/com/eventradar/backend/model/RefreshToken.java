package com.eventradar.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Data
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User user;

    // Enthält jetzt nur noch den Hash des Tokens
    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private Instant expiryDate;
}
