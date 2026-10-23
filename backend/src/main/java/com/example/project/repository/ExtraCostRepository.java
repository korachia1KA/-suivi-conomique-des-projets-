package com.example.project.repository;

import com.example.project.model.ExtraCost;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for the ExtraCost entity.
 * Extends JpaRepository to provide CRUD operations and query methods
 * for interacting with the "extra_cost" table in the database.
 */

public interface ExtraCostRepository extends JpaRepository<ExtraCost, String> { }