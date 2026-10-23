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
public class ExtraCostDto {
    private String idExtraCost;
    private String description;
    private BigDecimal cost;
    private LocalDate startDate;
    private LocalDate endDate;
    private String idProject;
}