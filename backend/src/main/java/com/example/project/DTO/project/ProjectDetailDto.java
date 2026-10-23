package com.example.project.DTO.project;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Full detail of a single project — used by GET /projects/{id}.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProjectDetailDto {

    private String idProject;
    private String name;
    private String statusName;
    private String businessUnitId;
    private String businessUnitName;

    private LocalDate startDate;
    private LocalDate plannedEndDate;
    private LocalDate endDate;

    private BigDecimal estimatedRevenue;
    private BigDecimal estimatedCost;

    // Live figures (revenue unchanged, cost = labor + extras)
    private BigDecimal currentRevenue;
    private BigDecimal currentCost;
    private BigDecimal currentMargin;      // percentage, null if revenue == 0

    // True when currentMargin < 15
    private boolean critical;

    private List<ExtraCostDto> extraCosts;
    private List<ProjectUserCostDto> users;
}