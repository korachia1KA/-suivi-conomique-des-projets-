package com.example.project.DTO.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) for representing deadline counts.
 * This class contains fields to track the number of deadlines in different categories.
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeadlineCounts {
    private long overdue;   // expired (plannedEndDate < today)
    private long next7;     // within 7 days
    private long next30;    // within 30 days
}