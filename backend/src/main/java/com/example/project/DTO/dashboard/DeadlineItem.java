package com.example.project.DTO.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Data Transfer Object (DTO) for representing a deadline item.
 * This class contains fields to describe the details of a specific deadline.
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeadlineItem {
    private String id;
    private String label;
    private LocalDate date;
    private long days;
    private String status;
}