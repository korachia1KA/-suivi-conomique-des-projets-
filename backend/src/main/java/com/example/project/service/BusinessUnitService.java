package com.example.project.service;

import com.example.project.DTO.businessUnit.BusinessUnitOption;

import java.util.List;

/**
 * Service interface for managing business units.
 * Provides methods for retrieving business unit options.
 */

public interface BusinessUnitService {
    List<BusinessUnitOption> listOptions();
}