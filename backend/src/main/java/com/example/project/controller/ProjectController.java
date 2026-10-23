package com.example.project.controller;

import com.example.project.DTO.project.*;
import com.example.project.service.ExtraCostService;
import com.example.project.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService service;
    private final ExtraCostService extraCostService;

    public ProjectController(ProjectService service, ExtraCostService extraCostService) {
        this.service = service;
        this.extraCostService = extraCostService;
    }

    @GetMapping
    public ProjectPageResponse<ProjectTable> listProjects(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String buId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startTo,
            @RequestParam(required = false) Integer marginMin,
            @RequestParam(required = false) Integer marginMax,
            @RequestParam(defaultValue = "name") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.pageProjectsForRole(q, buId, status, startFrom, startTo,
                marginMin, marginMax, sortField, sortDir, page, size);
    }

    @GetMapping("/summary")
    public ProjectSummaryResponse summary(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String buId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startTo,
            @RequestParam(required = false) Integer marginMin,
            @RequestParam(required = false) Integer marginMax
    ) {
        return service.summaryForRole(q, buId, status, startFrom, startTo, marginMin, marginMax);
    }

    @GetMapping("/totals")
    public ProjectSummaryResponse getProjectTotals(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String buId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startTo,
            @RequestParam(required = false) Integer marginMin,
            @RequestParam(required = false) Integer marginMax
    ) {
        return service.summaryForRole(q, buId, status, startFrom, startTo, marginMin, marginMax);
    }

    @GetMapping("/assignable-users")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public List<ProjectUser> getAssignableUsers() {
        return service.getAssignableUsers();
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> addProject(@RequestBody AddProjectRequest request) {
        service.addProject(request);
        return ResponseEntity.status(201).build();
    }

    @GetMapping("/{idProject}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ProjectDetailDto getProjectDetail(@PathVariable String idProject) {
        return service.getProjectDetail(idProject);
    }

    @PostMapping("/{idProject}/extra-costs")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ExtraCostDto> createExtraCost(
            @PathVariable String idProject,
            @Valid @RequestBody CreateExtraCostRequest req
    ) {
        ExtraCostDto created = extraCostService.create(idProject, req);
        service.checkProjectMarginAndAlert(idProject);
        return ResponseEntity.status(201).body(created);
    }

    @GetMapping("/{idProject}/extra-costs")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public List<ExtraCostDto> listExtraCosts(@PathVariable String idProject) {
        return extraCostService.listByProject(idProject);
    }

    @GetMapping("/{idProject}/hours")
    public List<TimesheetEntryDto> listHours(@PathVariable String idProject) {
        return service.listHours(idProject);
    }

    @PostMapping("/{idProject}/hours")
    public ResponseEntity<TimesheetEntryDto> saveHours(
            @PathVariable String idProject,
            @Valid @RequestBody CreateHoursRequest req
    ) {
        return ResponseEntity.ok(service.saveHours(idProject, req));
    }

    @DeleteMapping("/{idProject}/hours/{idUser}/{date}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteHours(
            @PathVariable String idProject,
            @PathVariable String idUser,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        service.deleteHours(idProject, idUser, date);
        return ResponseEntity.noContent().build();
    }
}