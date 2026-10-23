package com.example.project.DTO.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Data Transfer Object (DTO) for representing the dashboard response.
 * This class contains various fields to provide a summary of the dashboard data,
 * including user information, global KPIs, risk assessments, and other metrics.
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DashboardResponse {

    /* User Info */
    private String userRole;
    private String businessUnitName;
    private String seniorityLevel;

    /* Global KPI */
    private Long totalProjects;
    private Long activeProjects;
    private Long scheduledProjects;
    private BigDecimal totalRevenue;
    private BigDecimal totalCost;
    private BigDecimal averageMargin;

    /* Expected vs Actual */
    private BigDecimal expectedRevenue;
    private BigDecimal expectedCost;
    private BigDecimal expectedMargin;

    /* Top Performing Entities */
    private List<TopItem> topProjects;   // max 3
    private List<TopItem> topBusinessUnits; // max 3 (admin)

    /* Risk Assessment */
    private RiskCounts projectRisk;
    private RiskCounts businessUnitRisk;

    /* Summary details for UI cards */
    private List<BusinessUnitCard> businessUnits; // max 4
    private List<UserAllocation> employees;       // max 4
    private List<ProjectCard> projects;

    /* Deadline Tracking */
    private List<DeadlineItem> upcomingDeadlines; // top 3
    private DeadlineCounts deadlineCounts;        // overdue / due7 / due30
}