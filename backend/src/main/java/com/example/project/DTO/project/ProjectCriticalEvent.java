package com.example.project.DTO.project;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Broadcast when a project's margin crosses below the critical threshold.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectCriticalEvent {
    private String projectId;
    private String name;
    private String businessUnitId;
    private BigDecimal marginPct;
    private String severity;   // always "CRITICAL" for now, but future-proof
}