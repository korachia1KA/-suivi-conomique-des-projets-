package com.example.project.service;

import com.example.project.DTO.project.*;
import com.example.project.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ProjectService {

    ProjectPageResponse<ProjectTable> pageProjectsForRole(
            String q, String buId, String status,
            LocalDate startFrom, LocalDate startTo,
            Integer marginMin, Integer marginMax,
            String sortField, String sortDir,
            int page, int size
    );

    ProjectSummaryResponse summaryForRole(
            String q, String buId, String status,
            LocalDate startFrom, LocalDate startTo,
            Integer marginMin, Integer marginMax
    );

    BigDecimal calculateAverageMargin(BigDecimal totalRevenue, BigDecimal totalCost);

    void addProject(AddProjectRequest request);

    void assignUserToProjectInternal(Project project, ProjectUserAssignmentRequest a, String role);

    List<ProjectUser> getAssignableUsers();

    void checkProjectMarginAndAlert(String idProject);

    ProjectDetailDto getProjectDetail(String idProject);

    List<TimesheetEntryDto> listHours(String idProject);

    TimesheetEntryDto saveHours(String idProject, CreateHoursRequest req);

    /** Delete a timesheet entry (single user + single month). */
    void deleteHours(String idProject, String idUser, LocalDate month);
}