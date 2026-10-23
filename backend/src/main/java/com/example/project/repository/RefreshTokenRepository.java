package com.example.project.repository;

import com.example.project.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository interface for the RefreshToken entity.
 * Extends JpaRepository to provide CRUD operations and query methods
 * for interacting with the "refresh_token" table in the database.
 */

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByRefreshToken(String token);

    boolean existsByJtiAndRevokedFalseAndExpiresAtAfter(String jti, LocalDateTime now);
}