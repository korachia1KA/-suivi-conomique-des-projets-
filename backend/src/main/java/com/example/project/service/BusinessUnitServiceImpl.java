package com.example.project.service;

import com.example.project.DTO.businessUnit.BusinessUnitOption;
import com.example.project.model.BusinessUnit;
import com.example.project.model.ExtraCost;
import com.example.project.model.Project;
import com.example.project.repository.*;
import com.example.project.repository.projections.ProjectLaborCostAgg;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service implementation for managing business units.
 * Provides functionality to retrieve business unit options and calculate related metrics.
 */

@AllArgsConstructor
@Service
public class BusinessUnitServiceImpl implements BusinessUnitService {

    private final BusinessUnitRepository businessUnitRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final UserProjectMonthlyRepository userProjectMonthlyRepository;
    private final ExtraCostRepository extraCostRepository;

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private boolean isActiveOn(ExtraCost e, LocalDate day) {
        if (e == null) return false;
        LocalDate start = e.getStartDate();
        LocalDate end = e.getEndDate();
        if (start != null && start.isAfter(day)) return false;
        return end == null || !end.isBefore(day);
    }

    private BigDecimal marginPct(BigDecimal rev, BigDecimal cost) {
        if (rev == null || rev.compareTo(BigDecimal.ZERO) <= 0) return null;
        BigDecimal c = (cost == null) ? BigDecimal.ZERO : cost;
        return rev.subtract(c)
                .divide(rev, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public List<BusinessUnitOption> listOptions() {
        LocalDate today = LocalDate.now();

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
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                e -> nvl(e.getCost()),
                                BigDecimal::add
                        )
                ));

        List<Project> allProjects = projectRepository.findAll();

        Map<String, BigDecimal> marginByProjectId = allProjects.stream()
                .collect(Collectors.toMap(
                        Project::getIdProject,
                        p -> {
                            BigDecimal currentRevenue = p.getEstimatedRevenue();
                            BigDecimal labor = laborCostByProject.getOrDefault(p.getIdProject(), BigDecimal.ZERO);
                            BigDecimal extra = extraCostByProject.getOrDefault(p.getIdProject(), BigDecimal.ZERO);
                            BigDecimal currentCost = labor.add(extra);
                            return marginPct(currentRevenue, currentCost);
                        }
                ));

        return businessUnitRepository.findAll().stream()
                .sorted(Comparator.comparing(BusinessUnit::getName, String.CASE_INSENSITIVE_ORDER))
                .map(bu -> {
                    String buId = bu.getIdBusinessUnit();

                    Long employeeCount = userRepository.countByBusinessUnitId(buId);

                    String managerName = userRepository.findByBusinessUnitId(buId).stream()
                            .filter(u -> u.getRole() != null && u.getRole().getName() != null)
                            .filter(u -> "MANAGER".equalsIgnoreCase(u.getRole().getName()))
                            .map(u -> ((u.getFirstName() != null ? u.getFirstName() : "") +
                                    (u.getLastName() != null ? " " + u.getLastName() : "")).trim())
                            .filter(s -> !s.isBlank())
                            .findFirst()
                            .orElse(null);

                    List<BigDecimal> margins = allProjects.stream()
                            .filter(p -> p.getBusinessUnit() != null && buId.equals(p.getBusinessUnit().getIdBusinessUnit()))
                            .map(p -> marginByProjectId.get(p.getIdProject()))
                            .filter(Objects::nonNull)
                            .toList();

                    BigDecimal currentAvgMargin = null;
                    if (!margins.isEmpty()) {
                        BigDecimal sum = margins.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                        currentAvgMargin = sum.divide(BigDecimal.valueOf(margins.size()), 2, RoundingMode.HALF_UP);
                    }

                    BusinessUnitOption opt = new BusinessUnitOption();
                    opt.setId(buId);
                    opt.setName(bu.getName());

                    opt.setExpectedAnnualMargin(bu.getExpectedAnnualMargin());

                    opt.setEmployeeCount(employeeCount);
                    opt.setManagerName(managerName);
                    opt.setCurrentAverageMargin(currentAvgMargin);

                    return opt;
                })
                .toList();
    }
}