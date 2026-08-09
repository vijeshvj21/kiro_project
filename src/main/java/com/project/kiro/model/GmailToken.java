package com.project.kiro.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "gmail_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GmailToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "access_token", nullable = false, length = 1024)
    private String accessToken;

    @Column(name = "refresh_token", nullable = false, length = 1024)
    private String refreshToken;

    @Column(nullable = false)
    private boolean connected;

    @Column(name = "token_expires_at", nullable = false)
    private Instant tokenExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
