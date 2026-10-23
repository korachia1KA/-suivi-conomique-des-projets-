package com.example.project.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity class representing an Extra Cost.
 * This class maps to the "extra_cost" table in the database and contains
 * fields for storing details about additional costs associated with a project.
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "extra_cost")
public class ExtraCost {

    @Id
    @Column(name = "id_extra_cost")
    private String idExtraCost;

    @Column(name = "description")
    private String description;

    @Column(name = "cost", precision = 10, scale = 2, nullable = false)
    private BigDecimal cost;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "id_frequency")
    private String idFrequency;

    @Column(name = "id_project", nullable = false)
    private String idProject;
}