package com.example.project.service;

import com.example.project.DTO.dashboard.DashboardResponse;

/**
 * Service interface for managing dashboard data.
 * Provides a method to retrieve data for the dashboard.
 */

public interface DashboardService {
    DashboardResponse getDashboardData();
}