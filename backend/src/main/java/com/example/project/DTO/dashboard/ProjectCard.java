package com.example.project.DTO.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object (DTO) for representing a project card.
 * This class contains fields to describe the summary details of a project.
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProjectCard {
    private String id;
    private String name;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal marginPct;
    private String businessUnit;
}