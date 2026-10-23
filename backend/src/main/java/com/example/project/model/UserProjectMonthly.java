package com.example.project.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Entity class representing a User Project Monthly record.
 * This class maps to the "user_project_monthly" table in the database and contains
 * fields for storing details about a user's monthly allocation and costs for a specific project.
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_project_monthly")
public class UserProjectMonthly {

    @EmbeddedId
    private UserProjectMonthlyId id;

    @Column(name = "allocation_rate")
    private String allocationRate;

    @Column(name = "hourly_cost", precision = 10, scale = 2)
    private BigDecimal hourlyCost;

    @Column(name = "hours_worked", precision = 10, scale = 2)
    private BigDecimal hoursWorked;

}