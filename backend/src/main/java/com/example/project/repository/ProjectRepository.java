package com.example.project.repository;

import com.example.project.model.Project;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for the Project entity.
 * Extends JpaRepository to provide CRUD operations and query methods
 * for interacting with the "project" table in the database.
 */

public interface ProjectRepository extends JpaRepository<Project, String> {

    List<Project> findAll();

    @Query("""
        select p
        from Project p
        where
          (:q is null or :q = '' or lower(p.name) like lower(concat('%', :q, '%')))
          and (:buId is null or :buId = '' or p.businessUnit.idBusinessUnit = :buId)
          and (:status is null or :status = '' or upper(p.status.name) = upper(:status))
          and (:startFrom is null or p.startDate >= :startFrom)
          and (:startTo is null or p.startDate <= :startTo)
    """)
    List<Project> search(
            @Param("q") String q,
            @Param("buId") String buId,
            @Param("status") String status,
            @Param("startFrom") LocalDate startFrom,
            @Param("startTo") LocalDate startTo,
            Sort sort
    );


    List<Project> findByBusinessUnit_IdBusinessUnit(String buId);

}