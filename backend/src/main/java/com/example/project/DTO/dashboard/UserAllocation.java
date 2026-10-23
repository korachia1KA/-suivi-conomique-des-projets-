package com.example.project.DTO.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) for representing user allocation details.
 * This class contains fields to describe the allocation of a user within a business context.
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserAllocation {
    private String name;
    private String email;
    private String role;
    private String seniorityLevel;
    private String businessUnit;
}