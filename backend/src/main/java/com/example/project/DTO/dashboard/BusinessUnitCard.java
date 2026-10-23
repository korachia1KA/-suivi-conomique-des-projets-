package com.example.project.DTO.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) for representing a business unit card.
 * This class contains the necessary fields to describe a business unit's summary information.
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BusinessUnitCard {

    private String id;
    private String name;
    private long totalProjects;
    private long totalEmployees;
    private String managerName;
    private BigDecimal currentAverageMargin;
}