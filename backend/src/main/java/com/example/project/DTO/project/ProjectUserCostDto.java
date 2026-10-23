package com.example.project.DTO.project;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A user assigned to a project with their cost on this project.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProjectUserCostDto {
    private String idUser;
    private String fullName;
    private String role;
    private String seniorityLevel;
    private BigDecimal hourlyCost;
    private BigDecimal hoursWorked;    // null if no monthly record yet
}