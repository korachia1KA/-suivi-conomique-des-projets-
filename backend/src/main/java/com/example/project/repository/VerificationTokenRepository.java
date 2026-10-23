package com.example.project.repository;

import com.example.project.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Repository interface for the VerificationToken entity.
 * Extends JpaRepository to provide CRUD operations and query methods
 * for interacting with the "verification_token" table in the database.
 */

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, String> {

    Optional<VerificationToken> findByVerificationToken(String token);

}