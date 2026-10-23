package com.example.project.controller;

import com.example.project.DTO.businessUnit.BusinessUnitOption;
import com.example.project.service.BusinessUnitService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for managing Business Unit operations.
 * Provides an endpoint to retrieve a list of available business unit options.
 */

@AllArgsConstructor
@RestController
public class BusinessUnitController {

    private final BusinessUnitService businessUnitService;

    @GetMapping("/business-units")
    public List<BusinessUnitOption> listBusinessUnits() {
        return businessUnitService.listOptions();
    }
}