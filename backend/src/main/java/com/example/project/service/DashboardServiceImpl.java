package com.example.project.service;

import com.example.project.DTO.dashboard.*;
import com.example.project.DTO.dashboard.UserAllocation;
import com.example.project.exception.BusinessException;
import com.example.project.model.BusinessUnit;
import com.example.project.model.ExtraCost;
import com.example.project.model.Project;
import com.example.project.model.User;
import com.example.project.repository.ExtraCostRepository;
import com.example.project.repository.ProjectRepository;
import com.example.project.repository.UserProjectMonthlyRepository;
import com.example.project.repository.UserRepository;
import com.example.project.repository.projections.ProjectLaborCostAgg;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service implementation for managing dashboard data.
 * Provides methods to retrieve and calculate data for the dashboard.
 */

@AllArgsConstructor
@Service
public class DashboardServiceImpl implements DashboardService {

    private final ProjectService projectService;

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    private final ExtraCostRepository extraCostRepository;
    private final UserProjectMonthlyRepository userProjectMonthlyRepository;

    private static final BigDecimal OK_THR = new BigDecimal("25");
    private static final BigDecimal WARN_THR = new BigDecimal("15");

    @Override
    public DashboardResponse getDashboardData() {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);
        if (user == null) throw new BusinessException("USER_NOT_FOUND");

        String roleName = (user.getRole() != null) ? user.getRole().getName() : null;
        boolean isAdmin = "ADMIN".equalsIgnoreCase(roleName);

        String seniorityName = (user.getSeniorityLevel() != null) ? user.getSeniorityLevel().getName() : null;

        BusinessUnit bu = user.getBusinessUnit();
        if (!isAdmin && bu == null) throw new BusinessException("BUSINESS_UNIT_NOT_FOUND");

        String buId = (bu != null) ? bu.getIdBusinessUnit() : null;
        String buName = !isAdmin ? bu.getName() : null;

        List<Project> scopedProjects = isAdmin
                ? projectRepository.findAll()
                : projectRepository.findByBusinessUnit_IdBusinessUnit(buId);
        List<Project> activeScopedProjects = scopedProjects.stream()
                .filter(this::isActiveProject)
                .toList();

        LocalDate today = LocalDate.now();

        List<Project> deadlineProjects = scopedProjects.stream()
                .filter(p -> p.getPlannedEndDate() != null)
                .filter(p -> p.getStatus() == null || p.getStatus().getName() == null
                        || !"COMPLETED".equalsIgnoreCase(p.getStatus().getName()))
                .toList();

        List<DeadlineItem> upcomingDeadlines = deadlineProjects.stream()
                .sorted(Comparator.comparing(Project::getPlannedEndDate))
                .limit(5)
                .map(p -> {
                    long days = ChronoUnit.DAYS.between(today, p.getPlannedEndDate());
                    String statusName = (p.getStatus() != null ? p.getStatus().getName() : null);
                    return new DeadlineItem(p.getIdProject(), p.getName(), p.getPlannedEndDate(), days, statusName);
                })
                .toList();

        long overdue = deadlineProjects.stream()
                .filter(p -> ChronoUnit.DAYS.between(today, p.getPlannedEndDate()) < 0)
                .count();

        long due7 = deadlineProjects.stream()
                .map(p -> ChronoUnit.DAYS.between(today, p.getPlannedEndDate()))
                .filter(d -> d >= 0 && d <= 7)
                .count();

        long due30 = deadlineProjects.stream()
                .map(p -> ChronoUnit.DAYS.between(today, p.getPlannedEndDate()))
                .filter(d -> d >= 0 && d <= 30)
                .count();

        DeadlineCounts deadlineCounts = new DeadlineCounts(overdue, due7, due30);

        Map<String, BigDecimal> laborCostByProject = userProjectMonthlyRepository
                .aggregateLaborCostByProject(today)
                .stream()
                .collect(Collectors.toMap(
                        ProjectLaborCostAgg::getIdProject,
                        a -> nvl(a.getLaborCost())
                ));

        Map<String, BigDecimal> extraCostByProject = extraCostRepository.findAll().stream()
                .filter(e -> isActiveOn(e, today))
                .collect(Collectors.groupingBy(
                        ExtraCost::getIdProject,
                        Collectors.reducing(BigDecimal.ZERO, e -> nvl(e.getCost()), BigDecimal::add)
                ));

        BigDecimal totalRevenue = scopedProjects.stream()
                .map(Project::getEstimatedRevenue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCost = scopedProjects.stream()
                .map(p -> {
                    BigDecimal labor = laborCostByProject.getOrDefault(p.getIdProject(), BigDecimal.ZERO);
                    BigDecimal extra = extraCostByProject.getOrDefault(p.getIdProject(), BigDecimal.ZERO);
                    return labor.add(extra);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgMargin = projectService.calculateAverageMargin(totalRevenue, totalCost);

        long totalProjects = scopedProjects.size();

        long activeProjects = activeScopedProjects.size();

        long scheduledProjects = scopedProjects.stream()
                .map(this::statusNameOf)
                .filter(Objects::nonNull)
                .filter(s ->
                        s.equalsIgnoreCase("PLANNING") ||
                                s.equalsIgnoreCase("PLANNED")  ||
                                s.equalsIgnoreCase("SCHEDULED")
                )
                .count();

        BigDecimal expectedRevenue = totalRevenue.multiply(new BigDecimal("1.08"));
        BigDecimal expectedCost = totalCost.multiply(new BigDecimal("0.92"));
        BigDecimal expectedMargin = new BigDecimal("25");

        List<TopItem> topProjects = activeScopedProjects.stream()
                .map(p -> new AbstractMap.SimpleEntry<>(p, calcProjectMarginPct(p, laborCostByProject, extraCostByProject)))
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(3)
                .map(e -> new TopItem(
                        e.getKey().getIdProject(),
                        e.getKey().getName(),
                        scale2(e.getValue())
                ))
                .toList();

        RiskCounts projectRisk = countRisk(scopedProjects.stream()
                .map(p -> calcProjectMarginPct(p, laborCostByProject, extraCostByProject))
                .toList());

        List<User> usersForCard = isAdmin
                ? userRepository.findTop4ByEmailNotOrderByLastNameAscFirstNameAsc(email)
                : userRepository.findTop4ByBusinessUnitIdAndEmailNotOrderByLastNameAscFirstNameAsc(buId, email);

        List<UserAllocation> employees = usersForCard.stream()
                .map(u -> new UserAllocation(
                        (safe(u.getFirstName()) + " " + safe(u.getLastName())).trim(),
                        u.getEmail() != null ? u.getEmail() : null,
                        u.getRole() != null ? u.getRole().getName() : null,
                        u.getSeniorityLevel() != null ? u.getSeniorityLevel().getName() : null,
                        u.getBusinessUnit() != null ? u.getBusinessUnit().getName() : null
                ))
                .toList();

        List<TopItem> topBusinessUnits = null;
        RiskCounts businessUnitRisk = null;
        List<BusinessUnitCard> businessUnits = null;

        if (isAdmin) {
            Map<String, List<Project>> projectsByBu = projectRepository.findAll().stream()
                    .filter(p -> p.getBusinessUnit() != null)
                    .collect(Collectors.groupingBy(p -> p.getBusinessUnit().getIdBusinessUnit()));
            Map<String, List<Project>> activeProjectsByBu = projectRepository.findAll().stream()
                    .filter(p -> p.getBusinessUnit() != null)
                    .filter(this::isActiveProject)
                    .collect(Collectors.groupingBy(p -> p.getBusinessUnit().getIdBusinessUnit()));

            Map<String, BigDecimal> buAvgMargin = new HashMap<>();
            for (Map.Entry<String, List<Project>> entry : projectsByBu.entrySet()) {
                List<BigDecimal> margins = entry.getValue().stream()
                        .map(p -> calcProjectMarginPct(p, laborCostByProject, extraCostByProject))
                        .toList();
                buAvgMargin.put(entry.getKey(), average(margins));
            }
            Map<String, BigDecimal> activeBuAvgMargin = new HashMap<>();
            for (Map.Entry<String, List<Project>> entry : activeProjectsByBu.entrySet()) {
                List<BigDecimal> margins = entry.getValue().stream()
                        .map(p -> calcProjectMarginPct(p, laborCostByProject, extraCostByProject))
                        .toList();
                activeBuAvgMargin.put(entry.getKey(), average(margins));
            }

            topBusinessUnits = activeProjectsByBu.entrySet().stream()
                    .map(e -> {
                        String id = e.getKey();
                        String name = e.getValue().get(0).getBusinessUnit().getName();
                        return new TopItem(id, name, scale2(activeBuAvgMargin.getOrDefault(id, BigDecimal.ZERO)));
                    })
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .limit(3)
                    .toList();

            businessUnitRisk = countRisk(buAvgMargin.values().stream().toList());

            businessUnits = projectsByBu.entrySet().stream()
                    .map(e -> {
                        String id = e.getKey();
                        String name = e.getValue().get(0).getBusinessUnit().getName();

                        long projCount = e.getValue().size();
                        long empCount = userRepository.countByBusinessUnitId(id);

                        String managerName = userRepository.findByBusinessUnitId(id).stream()
                                .filter(u -> u.getRole() != null && u.getRole().getName() != null)
                                .filter(u -> "MANAGER".equalsIgnoreCase(u.getRole().getName()))
                                .map(u -> ((safe(u.getFirstName()) + " " + safe(u.getLastName())).trim()))
                                .filter(s -> !s.isBlank())
                                .findFirst()
                                .orElse(null);

                        return new BusinessUnitCard(
                                id,
                                name,
                                projCount,
                                empCount,
                                managerName,
                                scale2(buAvgMargin.getOrDefault(id, BigDecimal.ZERO))
                        );
                    })
                    .sorted(Comparator.comparing(BusinessUnitCard::getName, String.CASE_INSENSITIVE_ORDER))
                    .limit(4)
                    .toList();
        }

        List<ProjectCard> projectsCard;

        if (!isAdmin) {
            projectsCard = scopedProjects.stream()
                    .filter(p -> {
                        String status = statusNameOf(p);
                        return status != null && (
                                            status.equalsIgnoreCase("IN_PROGRESS") ||
                                            status.equalsIgnoreCase("PLANNING")
                        );
                    })
                    .sorted(Comparator.comparing((Project p) -> {
                                        LocalDate d = p.getPlannedEndDate();
                                        if (d == null) d = p.getEndDate();
                                        return d != null ? d : LocalDate.of(2999, 12, 31);
                                    })
                                    .thenComparing(Project::getName, String.CASE_INSENSITIVE_ORDER)
                    )
                    .limit(4)
                    .map(p -> new ProjectCard(
                            p.getIdProject(),
                            p.getName(),
                            p.getStatus() != null ? p.getStatus().getName() : null,
                            p.getStartDate(),
                            (p.getPlannedEndDate() != null ? p.getPlannedEndDate() : p.getEndDate()),
                            scale2(calcProjectMarginPct(p, laborCostByProject, extraCostByProject)),
                            p.getBusinessUnit() != null ? p.getBusinessUnit().getName() : null
                    ))
                    .toList();
        } else {
            projectsCard = scopedProjects.stream()
                    .sorted(Comparator.comparing(Project::getName, String.CASE_INSENSITIVE_ORDER))
                    .limit(4)
                    .map(p -> new ProjectCard(
                            p.getIdProject(),
                            p.getName(),
                            p.getStatus() != null ? p.getStatus().getName() : null,
                            p.getStartDate(),
                            (p.getPlannedEndDate() != null ? p.getPlannedEndDate() : p.getEndDate()),
                            scale2(calcProjectMarginPct(p, laborCostByProject, extraCostByProject)),
                            p.getBusinessUnit() != null ? p.getBusinessUnit().getName() : null
                    ))
                    .toList();
        }

        DashboardResponse dto = new DashboardResponse();
        dto.setUserRole(roleName);
        dto.setBusinessUnitName(buName);
        dto.setSeniorityLevel(seniorityName);

        dto.setTotalProjects(totalProjects);
        dto.setActiveProjects(activeProjects);
        dto.setScheduledProjects(scheduledProjects);
        dto.setTotalRevenue(scale2(totalRevenue));
        dto.setTotalCost(scale2(totalCost));
        dto.setAverageMargin(scale2(avgMargin));

        dto.setExpectedRevenue(scale2(expectedRevenue));
        dto.setExpectedCost(scale2(expectedCost));
        dto.setExpectedMargin(scale2(expectedMargin));

        dto.setTopProjects(topProjects);
        dto.setTopBusinessUnits(topBusinessUnits);

        dto.setProjectRisk(projectRisk);
        dto.setBusinessUnitRisk(businessUnitRisk);

        dto.setBusinessUnits(businessUnits);
        dto.setEmployees(employees);

        dto.setUpcomingDeadlines(upcomingDeadlines);
        dto.setDeadlineCounts(deadlineCounts);

        dto.setProjects(projectsCard);

        return dto;
    }

    // ===== HELPERS =====

    private String statusNameOf(Project p) {
        if (p == null) return null;
        if (p.getStatus() == null) return null;
        return p.getStatus().getName();
    }

    private boolean isActiveProject(Project p) {
        String status = statusNameOf(p);
        return status != null && (
                status.equalsIgnoreCase("IN_PROGRESS") ||
                status.equalsIgnoreCase("ACTIVE")
        );
    }

    private BigDecimal calcProjectMarginPct(Project p,
                                            Map<String, BigDecimal> laborCostByProject,
                                            Map<String, BigDecimal> extraCostByProject) {

        BigDecimal revenue = nvl(p.getEstimatedRevenue());

        BigDecimal labor = laborCostByProject.getOrDefault(p.getIdProject(), BigDecimal.ZERO);
        BigDecimal extra = extraCostByProject.getOrDefault(p.getIdProject(), BigDecimal.ZERO);
        BigDecimal cost = labor.add(extra);

        if (revenue.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;

        return revenue.subtract(cost)
                .divide(revenue, 6, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private RiskCounts countRisk(List<BigDecimal> margins) {
        long ok = 0, warn = 0, critical = 0;

        for (BigDecimal m : margins) {
            if (m == null) continue;
            if (m.compareTo(OK_THR) >= 0) ok++;
            else if (m.compareTo(WARN_THR) >= 0) warn++;
            else critical++;
        }
        return new RiskCounts(ok, warn, critical);
    }

    private BigDecimal average(List<BigDecimal> values) {
        List<BigDecimal> v = values.stream().filter(Objects::nonNull).toList();
        if (v.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = v.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(new BigDecimal(v.size()), 6, RoundingMode.HALF_UP);
    }

    private String safe(String s) { return s == null ? "" : s; }

    private BigDecimal nvl(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private BigDecimal scale2(BigDecimal v) {
        return v == null ? null : v.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isActiveOn(ExtraCost e, LocalDate day) {
        if (e == null) return false;
        LocalDate start = e.getStartDate();
        LocalDate end = e.getEndDate();
        if (start != null && start.isAfter(day)) return false;
        return end == null || !end.isBefore(day);
    }
}