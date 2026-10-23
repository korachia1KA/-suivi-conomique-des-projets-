package com.example.project.DTO.businessUnit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) for representing business unit options.
 * This class contains the necessary fields to describe a business unit's details.
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BusinessUnitOption {

    private String id;
    private String name;
    private BigDecimal currentAverageMargin;
    private BigDecimal expectedAnnualMargin;
    private Long employeeCount;
    private String managerName;
}