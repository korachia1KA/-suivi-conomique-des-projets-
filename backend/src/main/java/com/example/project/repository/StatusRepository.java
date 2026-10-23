package com.example.project.repository;

import com.example.project.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for the Status entity.
 * Extends JpaRepository to provide CRUD operations and query methods
 * for interacting with the "status" table in the database.
 */

@Repository
public interface StatusRepository extends JpaRepository<Status, String> {
    Optional<Status> findByName(String status);
}