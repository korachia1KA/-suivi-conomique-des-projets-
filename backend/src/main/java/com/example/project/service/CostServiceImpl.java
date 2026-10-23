package com.example.project.service;

import com.example.project.DTO.costs.CostLineDto;
import com.example.project.exception.BusinessException;
import com.example.project.model.ExtraCost;
import com.example.project.model.Project;
import com.example.project.model.User;
import com.example.project.repository.ExtraCostRepository;
import com.example.project.repository.ProjectRepository;
import com.example.project.repository.UserProjectMonthlyRepository;
import com.example.project.repository.UserRepository;
import com.example.project.repository.projections.ProjectLaborCostAgg;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CostServiceImpl implements CostService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final UserProjectMonthlyRepository userProjectMonthlyRepository;
    private final ExtraCostRepository extraCostRepository;

    private BigDecimal nvl(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private BigDecimal marginPct(BigDecimal rev, BigDecimal cost) {
        if (rev == null || rev.compareTo(BigDecimal.ZERO) <= 0) return null;
        BigDecimal c = (cost == null) ? BigDecimal.ZERO : cost;
        return rev.subtract(c)
                .divide(rev, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isActiveOn(ExtraCost e, LocalDate day) {
        if (e == null) return false;
        LocalDate start = e.getStartDate();
        LocalDate end = e.getEndDate();
        if (start != null && start.isAfter(day)) return false;
        return end == null || !end.isBefore(day);
    }

    @Override
    @Transactional
    public List<CostLineDto> listCosts(String q, String buId, String status) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User me = userRepository.findByEmail(email);
        if (me == null) throw new BusinessException("USER_NOT_FOUND");

        String role = me.getRole() != null ? me.getRole().getName() : "";
        boolean isAdmin   = "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role);
        boolean isManager = "MANAGER".equalsIgnoreCase(role) || "ROLE_MANAGER".equalsIgnoreCase(role);

        // ---- Scope by role ----
        List<Project> projects;
        if (isAdmin) {
            projects = projectRepository.search(q, buId, status, null, null,
                    org.springframework.data.domain.Sort.by("name"));
        } else if (isManager) {
            if (me.getBusinessUnit() == null) throw new BusinessException("BUSINESS_UNIT_NOT_FOUND");
            String myBu = me.getBusinessUnit().getIdBusinessUnit();
            projects = projectRepository.search(q, myBu, status, null, null,
                    org.springframework.data.domain.Sort.by("name"));
        } else {
            // EMPLOYEE: only projects they are assigned to
            List<String> myProjectIds = userProjectMonthlyRepository
                    .findByUser(me.getIdUser())
                    .stream()
                    .map(upm -> upm.getId().getIdProject())
                    .distinct()
                    .toList();

            projects = projectRepository.findAll().stream()
                    .filter(p -> myProjectIds.contains(p.getIdProject()))
                    .filter(p -> q == null || q.isBlank()
                            || p.getName().toLowerCase().contains(q.toLowerCase()))
                    .filter(p -> status == null || status.isBlank()
                            || (p.getStatus() != null && p.getStatus().getName().equalsIgnoreCase(status)))
                    .sorted(Comparator.comparing(Project::getName, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }

        // ---- Aggregates ----
        LocalDate today = LocalDate.now();

        Map<String, BigDecimal> laborByProject = userProjectMonthlyRepository
                .aggregateLaborCostByProject(today)
                .stream()
                .collect(Collectors.toMap(
                        ProjectLaborCostAgg::getIdProject,
                        a -> nvl(a.getLaborCost())
                ));

        Map<String, BigDecimal> extraByProject = extraCostRepository.findAll().stream()
                .filter(e -> isActiveOn(e, today))
                .collect(Collectors.groupingBy(
                        ExtraCost::getIdProject,
                        Collectors.reducing(BigDecimal.ZERO, e -> nvl(e.getCost()), BigDecimal::add)
                ));

        // ---- Map to DTO ----
        return projects.stream()
                .map(p -> {
                    BigDecimal labor = laborByProject.getOrDefault(p.getIdProject(), BigDecimal.ZERO);
                    BigDecimal extra = extraByProject.getOrDefault(p.getIdProject(), BigDecimal.ZERO);
                    BigDecimal totalCost = labor.add(extra);
                    BigDecimal margin = marginPct(p.getEstimatedRevenue(), totalCost);

                    return new CostLineDto(
                            p.getIdProject(),
                            p.getName(),
                            p.getBusinessUnit() != null ? p.getBusinessUnit().getIdBusinessUnit() : null,
                            p.getBusinessUnit() != null ? p.getBusinessUnit().getName() : null,
                            p.getStatus() != null ? p.getStatus().getName() : null,
                            p.getStartDate(),
                            p.getPlannedEndDate(),
                            p.getEstimatedRevenue(),
                            labor,
                            extra,
                            totalCost,
                            margin
                    );
                })
                .toList();
    }
}