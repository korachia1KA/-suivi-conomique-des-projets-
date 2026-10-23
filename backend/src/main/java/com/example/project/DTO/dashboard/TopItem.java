package com.example.project.DTO.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) for representing a top-performing item.
 * This class contains fields to describe the details of an item.
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TopItem {
    private String id;
    private String label;
    private BigDecimal value;
}