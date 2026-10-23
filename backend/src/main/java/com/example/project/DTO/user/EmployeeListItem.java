package com.example.project.DTO.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO returned by GET /user/list for the Employees page.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeListItem {

    private String idUser;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private String seniorityLevel;
    private String businessUnitId;
    private String businessUnit;
}