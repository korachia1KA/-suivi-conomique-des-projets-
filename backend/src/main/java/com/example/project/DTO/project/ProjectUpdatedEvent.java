package com.example.project.DTO.project;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Broadcast when a project's data changes (hours added/removed, extra-cost added/removed).
 * Unlike PROJECT_CRITICAL, this is sent for ANY change, not only critical ones.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectUpdatedEvent {
    private String projectId;
    private String name;
    private String businessUnitId;
    private BigDecimal currentMargin;   // may be null
    private String changeKind;          // HOURS_ADDED, HOURS_REMOVED, EXTRA_COST_ADDED, ...
}