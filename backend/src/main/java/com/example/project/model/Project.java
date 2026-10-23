package com.example.project.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity class representing a Project.
 * This class maps to the "project" table in the database
 * and contains fields for storing details about a project.
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "project")
public class Project {
    @Id
    @Column(name = "id_project", nullable = false)
    private String idProject;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "start_date")
    private LocalDate startDate;
    /**
     * Last margin value for which a critical alert was sent.
     * Used to avoid sending duplicate alerts when margin is already below the threshold.
     */
    @Column(name = "last_alerted_margin", precision = 10, scale = 2)
    private BigDecimal lastAlertedMargin;

    /**
     * When the last critical alert was sent (null if never).
     */
    @Column(name = "last_alerted_at")
    private java.time.LocalDateTime lastAlertedAt;
    @Column(name = "planned_end_date")
    private LocalDate plannedEndDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "estimated_revenue", precision = 10, scale = 2)
    private BigDecimal estimatedRevenue;

    @Column(name = "estimated_cost", precision = 10, scale = 2)
    private BigDecimal estimatedCost;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_business_unit", referencedColumnName = "id_business_unit")
    private BusinessUnit businessUnit;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_status", referencedColumnName = "id_status")
    private Status status;
}