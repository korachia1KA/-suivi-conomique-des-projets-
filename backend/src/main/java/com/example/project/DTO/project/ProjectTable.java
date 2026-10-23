package com.example.project.DTO.project;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object (DTO) for representing project details in a tabular format.
 * This class contains fields that provide an overview of a project's key attributes.
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProjectTable {
    private String idProject;
    private String name;
    private String statusName;
    private String businessUnitName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal currentRevenue;
    private BigDecimal currentCost;
    private BigDecimal currentMargin;
}