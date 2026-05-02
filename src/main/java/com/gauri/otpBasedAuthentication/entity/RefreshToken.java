package com.gauri.otpBasedAuthentication.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Data
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "is_revoked", nullable = false)
    private boolean isRevoked;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;


    @ManyToOne(optional = false)
    @JoinColumn(name = "session_id",  nullable = false)
    private Session session;
}
