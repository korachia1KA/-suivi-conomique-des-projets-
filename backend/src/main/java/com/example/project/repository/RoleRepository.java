package com.example.project.repository;

import com.example.project.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for the Role entity.
 * Provides CRUD operations and query methods for interacting with the "role" table.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, String> {

    Optional<Role> findByName(String name);

    boolean existsByName(String name);
}