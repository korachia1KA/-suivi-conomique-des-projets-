package com.example.project.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity class representing a Verification Token.
 * This class maps to the "verification_token" table in the database and
 * contains fields for storing details about verification tokens.
 */

@RequiredArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "verification_token")
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_verification_token", nullable = false, unique = true)
    private String idVerificationToken;

    @Column(name="verification_token", nullable = false, unique = true)
    private String verificationToken;

    @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "id_user")
    private User user;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}