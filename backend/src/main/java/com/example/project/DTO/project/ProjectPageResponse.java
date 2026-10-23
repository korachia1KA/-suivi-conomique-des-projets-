package com.example.project.DTO.project;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Data Transfer Object (DTO) for representing a paginated response of projects.
 * This class contains fields to describe the paginated list of projects and their associated metadata.
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProjectPageResponse<T> {
    private List<T> items;
    private long total;
    private int page;
    private int size;
    private BigDecimal totalRevenue;
    private BigDecimal totalCost;
    private BigDecimal marginPct;
}