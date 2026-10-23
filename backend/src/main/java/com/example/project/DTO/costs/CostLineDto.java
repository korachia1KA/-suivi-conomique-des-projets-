package com.example.project.DTO.costs;

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
public class CostLineDto {
    private String idProject;
    private String projectName;
    private String businessUnitId;
    private String businessUnitName;
    private String statusName;

    private LocalDate startDate;
    private LocalDate plannedEndDate;

    private BigDecimal estimatedRevenue;
    private BigDecimal laborCost;
    private BigDecimal extraCost;
    private BigDecimal totalCost;
    private BigDecimal marginPct;
}