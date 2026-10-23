package com.example.project.DTO.project;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TimesheetEntryDto {
    private String idUser;
    private String fullName;
    private String idProject;
    private String projectName;
    private LocalDate date;        // first day of the month
    private BigDecimal hoursWorked;
    private BigDecimal hourlyCost;
    private BigDecimal cost;       // hoursWorked * hourlyCost
}