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

/**
 * Entity class representing a Business Unit.
 * This class maps to the "business_unit" table in the database and
 * contains fields for storing business unit details
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "business_unit")
public class BusinessUnit {

    @Id
    @Column(name = "id_business_unit")
    private String idBusinessUnit;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "annual_budget", precision = 10, scale = 2)
    private BigDecimal annualBudget;

    @Column(name = "expected_annual_margin", precision = 5, scale = 2)
    private BigDecimal expectedAnnualMargin;

}