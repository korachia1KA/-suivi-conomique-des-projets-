package com.example.project.repository;

import com.example.project.model.BusinessUnit;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for the BusinessUnit entity.
 * Extends JpaRepository to provide CRUD operations and query methods
 * for interacting with the "business_unit" table in the database.
 */

public interface BusinessUnitRepository extends JpaRepository<BusinessUnit, String> { }
