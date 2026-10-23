package com.example.project.controller;

import com.example.project.DTO.dashboard.DashboardResponse;
import com.example.project.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for handling dashboard-related operations.
 * Provides an endpoint to retrieve dashboard data.
 */

@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public DashboardResponse getDashboardData() {
        return dashboardService.getDashboardData();
    }
}