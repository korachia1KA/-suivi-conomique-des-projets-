package com.example.project.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * Entity class representing a Refresh Token.
 * This class maps to the "refresh_token" table in the database and contains
 * fields for storing details about refresh tokens used for authentication.
 */

@Getter
@Setter
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @Column(name = "id_refresh_token")
    private String idRefreshToken;

    @Column(name = "refresh_token", nullable = false, unique = true)
    private String refreshToken;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked")
    private Boolean revoked;

    @Column(name = "user_agent", nullable = false)
    private String userAgent;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "jti", nullable = false)
    private String jti;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user", referencedColumnName = "id_user")
    private User user;
}