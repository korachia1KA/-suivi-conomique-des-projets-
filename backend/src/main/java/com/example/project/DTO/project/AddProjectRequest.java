package com.example.project.DTO.project;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Data Transfer Object (DTO) for adding a new project.
 * This class contains fields required to create a new project request.
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AddProjectRequest {
    private String name;
    private String businessUnitId;
    private LocalDate startDate;
    private LocalDate plannedEndDate;
    private BigDecimal estimatedRevenue;
    private BigDecimal estimatedCost;
    private List<ProjectUserAssignmentRequest> users;
}