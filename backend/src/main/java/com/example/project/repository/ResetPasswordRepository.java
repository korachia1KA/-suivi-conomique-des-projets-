package com.example.project.repository;

import com.example.project.model.ResetPassword;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository interface for the ResetPassword entity.
 * Extends JpaRepository to provide CRUD operations and query methods
 * for interacting with the "reset_password" table in the database.
 */

public interface ResetPasswordRepository extends JpaRepository<ResetPassword, String> {

    Optional<ResetPassword> findTopByResetTokenOrderByIssuedAtDesc(String resetToken);

    Optional<ResetPassword> findTopByUser_EmailAndUsedFalseAndExpiresAtAfterOrderByIssuedAtDesc(
            String email,
            LocalDateTime now
    );

    long countByUser_EmailAndIssuedAtAfter(String email, LocalDateTime after);

    @Modifying
    @Transactional
    @Query("""
        update ResetPassword r
        set r.used = true
        where r.user.email = :email
          and r.used = false
    """)
    void invalidatePreviousOtps(@Param("email") String email);

}