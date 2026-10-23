package com.example.project.DTO.project;

import lombok.*;

import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) for assigning a user to a project.
 * This class contains fields that specify the user and their associated hourly cost.
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProjectUserAssignmentRequest {
    private String userId;
    private BigDecimal hourlyCost;
}