package com.example.project.DTO.project;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Body for POST /projects/{id}/hours.
 * `date` is normalized server-side to the first day of the month.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateHoursRequest {

    /** Optional for ADMIN / MANAGER (defaults to caller). Required for EMPLOYEE. */
    private String idUser;

    @NotNull
    private LocalDate date;    // any day of the month → normalized to day 1

    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal hoursWorked;

    /** Optional override; if null, uses the user's current hourlyCost on this project. */
    private BigDecimal hourlyCost;
}