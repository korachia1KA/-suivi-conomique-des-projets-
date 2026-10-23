package com.example.project.repository;

import com.example.project.model.UserProjectMonthly;
import com.example.project.model.UserProjectMonthlyId;
import com.example.project.repository.projections.ProjectLaborCostAgg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface UserProjectMonthlyRepository
        extends JpaRepository<UserProjectMonthly, UserProjectMonthlyId> {

    // ---------- aggregations (existing) ----------

    @Query("""
        select upm.id.idProject as idProject,
               coalesce(sum(upm.hoursWorked * upm.hourlyCost), 0) as laborCost
        from UserProjectMonthly upm
        group by upm.id.idProject
    """)
    List<ProjectLaborCostAgg> aggregateLaborCostByProjectAll();

    @Query("""
        select upm.id.idProject as idProject,
               coalesce(sum(upm.hoursWorked * upm.hourlyCost), 0) as laborCost
        from UserProjectMonthly upm
        where upm.id.date <= :asOf
        group by upm.id.idProject
    """)
    List<ProjectLaborCostAgg> aggregateLaborCostByProjectAsOf(@Param("asOf") LocalDate asOf);

    default List<ProjectLaborCostAgg> aggregateLaborCostByProject(LocalDate asOf) {
        return (asOf == null)
                ? aggregateLaborCostByProjectAll()
                : aggregateLaborCostByProjectAsOf(asOf);
    }

    // ---------- new: per-project / per-user reads ----------

    @Query("""
        select upm
        from UserProjectMonthly upm
        where upm.id.idProject = :idProject
        order by upm.id.date desc, upm.id.idUser asc
    """)
    List<UserProjectMonthly> findByProject(@Param("idProject") String idProject);

    @Query("""
        select upm
        from UserProjectMonthly upm
        where upm.id.idProject = :idProject
          and upm.id.idUser = :idUser
        order by upm.id.date desc
    """)
    List<UserProjectMonthly> findByProjectAndUser(@Param("idProject") String idProject,
                                                  @Param("idUser") String idUser);

    @Query("""
        select upm
        from UserProjectMonthly upm
        where upm.id.idUser = :idUser
        order by upm.id.date desc
    """)
    List<UserProjectMonthly> findByUser(@Param("idUser") String idUser);

    @Query("""
        select upm
        from UserProjectMonthly upm
        where upm.id.idProject = :idProject
          and upm.id.idUser = :idUser
          and upm.id.date = :date
    """)
    UserProjectMonthly findOne(@Param("idProject") String idProject,
                               @Param("idUser") String idUser,
                               @Param("date") LocalDate date);
}