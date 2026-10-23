package com.example.project.DTO.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AssignBusinessUnitRequest {
    /** Can be null or empty to unassign the BU. */
    private String businessUnitId;
}