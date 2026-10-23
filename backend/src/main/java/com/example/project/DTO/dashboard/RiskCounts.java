package com.example.project.DTO.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) for representing risk counts.
 * This class contains fields to track the number of items in different risk categories.
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RiskCounts {
    private long ok;
    private long warning;
    private long critical;
}