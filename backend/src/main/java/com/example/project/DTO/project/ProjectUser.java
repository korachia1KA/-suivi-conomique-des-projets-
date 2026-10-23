package com.example.project.DTO.project;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) for representing a user associated with a project.
 * This class contains fields that provide details about the user and their association with a business unit.
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProjectUser {
    private String userId;
    private String name;
    private String businessUnitId;
}