package com.example.project.repository;

import com.example.project.model.SeniorityLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for the SeniorityLevel entity.
 * Provides CRUD operations and lookup by name.
 */
@Repository
public interface SeniorityLevelRepository extends JpaRepository<SeniorityLevel, String> {

    Optional<SeniorityLevel> findByName(String name);

    boolean existsByName(String name);
}