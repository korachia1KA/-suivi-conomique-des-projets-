package com.example.project.DTO.project;

import lombok.*;
import java.time.LocalDate;

/**
 * Data Transfer Object (DTO) for adding a new project.
 * This class contains fields required to create a new project request.
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectCreatedEvent {
    private String projectId;
    private String name;
    private String businessUnitId;
    private LocalDate startDate;
    private LocalDate plannedEndDate;
}