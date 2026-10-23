package com.example.project.DTO.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AdminUserDto {
    private String idUser;
    private String email;
    private String firstName;
    private String lastName;
    private String roleName;
    private String businessUnitId;
    private String businessUnitName;
    private String seniorityLevelName;
    private Boolean enabled;
}