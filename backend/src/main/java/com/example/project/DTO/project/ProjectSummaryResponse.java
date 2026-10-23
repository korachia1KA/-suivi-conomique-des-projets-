package com.example.project.DTO.project;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) for summarizing project details.
 * This class contains fields to represent aggregated data about projects.
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProjectSummaryResponse {
    private long count;
    private BigDecimal totalRevenue;
    private BigDecimal totalCost;
    private BigDecimal marginPct;
    private long atRiskCount; // Projects with margin < 15%
    private long borderlineCount;// Projects with 15% <= margin < 25%
}