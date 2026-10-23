package com.example.project.repository;

import com.example.project.model.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for the User entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    User save(User u);

    long count();

    Boolean existsByEmail(String email);

    User findByEmail(String email);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.enabled = :status WHERE u.idUser = :id")
    void updateEnabledStatus(@Param("id") String id, @Param("status") boolean status);

    @Query("SELECT u FROM User u WHERE u.businessUnit.idBusinessUnit = :buId")
    List<User> findByBusinessUnitId(String buId);

    @Query("SELECT COUNT(u) FROM User u WHERE u.businessUnit.idBusinessUnit = :buId")
    long countByBusinessUnitId(@Param("buId") String buId);

    List<User> findTop4ByEmailNotOrderByLastNameAscFirstNameAsc(String email);

    @Query("""
        SELECT u
        FROM User u
        WHERE u.businessUnit.idBusinessUnit = :buId
          AND u.email <> :email
        ORDER BY u.lastName ASC, u.firstName ASC
    """)
    List<User> findTop4ByBusinessUnitIdAndEmailNotOrderByLastNameAscFirstNameAsc(
            @Param("buId") String buId,
            @Param("email") String email
    );
@Query("""
    SELECT u FROM User u
    ORDER BY u.lastName ASC, u.firstName ASC
""")
List<User> findAllOrdered();
    /**
     * Search users for the "Employees" page.
     * Filters are optional (null / empty means "no filter").
     * Scope is enforced at the service layer (manager → own BU).
     */
    @Query("""
        SELECT u
        FROM User u
        WHERE (:q IS NULL OR :q = ''
               OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(u.lastName)  LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(u.email)     LIKE LOWER(CONCAT('%', :q, '%')))
          AND (:buId IS NULL OR :buId = '' OR u.businessUnit.idBusinessUnit = :buId)
          AND (:role IS NULL OR :role = '' OR UPPER(u.role.name) = UPPER(:role))
        ORDER BY u.lastName ASC, u.firstName ASC
    """)
    List<User> searchEmployees(
            @Param("q") String q,
            @Param("buId") String buId,
            @Param("role") String role
    );
} 