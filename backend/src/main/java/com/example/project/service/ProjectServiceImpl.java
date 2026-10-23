package com.example.project.service;

import com.example.project.DTO.project.*;
import com.example.project.exception.BusinessException;
import com.example.project.model.*;
import com.example.project.repository.*;
import com.example.project.repository.projections.ProjectLaborCostAgg;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProjectServiceImpl implements ProjectService {

    private static final BigDecimal CRITICAL_THRESHOLD = new BigDecimal("15");

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final UserProjectMonthlyRepository userProjectMonthlyRepository;
    private final ExtraCostRepository extraCostRepository;
    private final BusinessUnitRepository businessUnitRepository;
    private final StatusRepository statusRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    public ProjectServiceImpl(ProjectRepository projectRepository,
                              UserRepository userRepository,
                              UserProjectMonthlyRepository userProjectMonthlyRepository,
                              ExtraCostRepository extraCostRepository,
                              BusinessUnitRepository businessUnitRepository,
                              StatusRepository statusRepository,
                              SimpMessagingTemplate messagingTemplate,
                              NotificationService notificationService) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.userProjectMonthlyRepository = userProjectMonthlyRepository;
        this.extraCostRepository = extraCostRepository;
        this.businessUnitRepository = businessUnitRepository;
        this.statusRepository = statusRepository;
        this.messagingTemplate = messagingTemplate;
        this.notificationService = notificationService;
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);
        if (user == null) throw new BusinessException("USER_NOT_FOUND");
        return user;
    }

    private String getCurrentUserRole() {
        User user = getCurrentUser();
        return user.getRole() != null ? user.getRole().getName() : "";
    }

    private String getCurrentUserBusinessUnitId() {
        User user = getCurrentUser();
        if (user.getBusinessUnit() == null) throw new BusinessException("BUSINESS_UNIT_NOT_FOUND");
        return user.getBusinessUnit().getIdBusinessUnit();
    }

    private BigDecimal nvl(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private BigDecimal marginPct(BigDecimal rev, BigDecimal cost) {
        if (rev == null || rev.compareTo(BigDecimal.ZERO) == 0) return null;
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

    private String safe(String s) { return s == null ? "" : s; }

    // ------------------------------------------------------------------
    // sorting
    // ------------------------------------------------------------------

    private Sort buildDbSort(String sortField, boolean desc) {
        String prop = switch (sortField) {
            case "name" -> "name";
            case "bu" -> "businessUnit.name";
            case "status" -> "status.name";
            case "startDate" -> "startDate";
            case "endDate" -> "endDate";
            case "currentRevenue" -> "estimatedRevenue";
            default -> null;
        };
        if (prop == null) return Sort.unsorted();
        return Sort.by(desc ? Sort.Direction.DESC : Sort.Direction.ASC, prop)
                .and(Sort.by(Sort.Direction.ASC, "name"));
    }

    private boolean isComputedSort(String sortField) {
        return "currentCost".equals(sortField) || "currentMargin".equals(sortField);
    }

    private Comparator<ProjectTable> buildComputedComparator(String sortField) {
        return switch (sortField) {
            case "currentCost" ->
                    Comparator.comparing(ProjectTable::getCurrentCost, Comparator.nullsLast(BigDecimal::compareTo));
            case "currentMargin" ->
                    Comparator.comparing(ProjectTable::getCurrentMargin, Comparator.nullsLast(BigDecimal::compareTo));
            default -> null;
        };
    }

    // ------------------------------------------------------------------
    // listing / summary
    // ------------------------------------------------------------------

    private List<ProjectTable> buildRowsForRole(
            String q, String buId, String status,
            LocalDate startFrom, LocalDate startTo,
            Integer marginMin, Integer marginMax,
            String sortField, String sortDir
    ) {
        String role = getCurrentUserRole();
        if ("MANAGER".equalsIgnoreCase(role)) {
            buId = getCurrentUserBusinessUnitId();
        }

        boolean desc = "desc".equalsIgnoreCase(sortDir);
        Sort dbSort = isComputedSort(sortField) ? Sort.unsorted() : buildDbSort(sortField, desc);

        List<Project> projects = projectRepository.search(q, buId, status, startFrom, startTo, dbSort);

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
                        Collectors.reducing(BigDecimal.ZERO, e -> nvl(e.getCost()), BigDecimal::add)
                ));

        List<ProjectTable> rows = projects.stream()
                .map(p -> {
                    BigDecimal currentRevenue = p.getEstimatedRevenue();
                    BigDecimal labor = laborCostByProject.getOrDefault(p.getIdProject(), BigDecimal.ZERO);
                    BigDecimal extra = extraCostByProject.getOrDefault(p.getIdProject(), BigDecimal.ZERO);
                    BigDecimal currentCost = labor.add(extra);
                    BigDecimal currentMargin = marginPct(currentRevenue, currentCost);

                    return new ProjectTable(
                            p.getIdProject(),
                            p.getName(),
                            p.getStatus() != null ? p.getStatus().getName() : null,
                            "MANAGER".equalsIgnoreCase(role) || p.getBusinessUnit() == null
                                    ? null
                                    : p.getBusinessUnit().getName(),
                            p.getStartDate(),
                            p.getEndDate(),
                            currentRevenue,
                            currentCost,
                            currentMargin
                    );
                })
                .toList();

        if (marginMin != null || marginMax != null) {
            BigDecimal min = (marginMin == null) ? null : BigDecimal.valueOf(marginMin);
            BigDecimal max = (marginMax == null) ? null : BigDecimal.valueOf(marginMax);

            rows = rows.stream()
                    .filter(r -> {
                        BigDecimal m = r.getCurrentMargin();
                        if (m == null) return false;
                        if (min != null && m.compareTo(min) < 0) return false;
                        return max == null || m.compareTo(max) <= 0;
                    })
                    .toList();
        }

        if (isComputedSort(sortField)) {
            Comparator<ProjectTable> cmp = buildComputedComparator(sortField);
            if (cmp != null) {
                rows = rows.stream().sorted(desc ? cmp.reversed() : cmp).toList();
            }
        }

        return rows;
    }

    @Override
    public ProjectPageResponse<ProjectTable> pageProjectsForRole(
            String q, String buId, String status,
            LocalDate startFrom, LocalDate startTo,
            Integer marginMin, Integer marginMax,
            String sortField, String sortDir,
            int page, int size
    ) {
        List<ProjectTable> all = buildRowsForRole(
                q, buId, status, startFrom, startTo, marginMin, marginMax, sortField, sortDir
        );
        ProjectSummaryResponse summary = summaryForRole(q, buId, status, startFrom, startTo, marginMin, marginMax);

        long total = all.size();
        int from = Math.max(0, page * size);
        int to = Math.min(all.size(), from + size);
        List<ProjectTable> items = (from >= to) ? List.of() : all.subList(from, to);

        return new ProjectPageResponse<>(
                items, total, page, size,
                summary.getTotalRevenue(),
                summary.getTotalCost(),
                summary.getMarginPct()
        );
    }

    @Override
    public ProjectSummaryResponse summaryForRole(
            String q, String buId, String status,
            LocalDate startFrom, LocalDate startTo,
            Integer marginMin, Integer marginMax
    ) {
        List<ProjectTable> all = buildRowsForRole(
                q, buId, status, startFrom, startTo, marginMin, marginMax, "name", "asc"
        );

        BigDecimal totalRevenue = all.stream()
                .map(ProjectTable::getCurrentRevenue).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCost = all.stream()
                .map(ProjectTable::getCurrentCost).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal marginPct = marginPct(totalRevenue, totalCost);

        long atRiskCount = all.stream()
                .filter(p -> p.getCurrentMargin() != null && p.getCurrentMargin().compareTo(CRITICAL_THRESHOLD) < 0)
                .count();

        long borderlineCount = all.stream()
                .filter(p -> p.getCurrentMargin() != null &&
                        p.getCurrentMargin().compareTo(CRITICAL_THRESHOLD) >= 0 &&
                        p.getCurrentMargin().compareTo(new BigDecimal("25")) < 0)
                .count();

        return new ProjectSummaryResponse(all.size(), totalRevenue, totalCost, marginPct, atRiskCount, borderlineCount);
    }

    @Override
    public BigDecimal calculateAverageMargin(BigDecimal totalRevenue, BigDecimal totalCost) {
        if (totalRevenue == null || totalRevenue.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return totalRevenue.subtract(totalCost)
                .divide(totalRevenue, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------
    // add project
    // ------------------------------------------------------------------

    @Transactional
    @Override
    public void addProject(AddProjectRequest request) {
        String role = getCurrentUserRole();
        if (!"ADMIN".equalsIgnoreCase(role) && !"MANAGER".equalsIgnoreCase(role)) {
            throw new BusinessException("ROLE_NOT_ALLOWED");
        }

        Project project = new Project();
        project.setIdProject(UUID.randomUUID().toString());
        project.setName(request.getName());

        if ("MANAGER".equalsIgnoreCase(role)) {
            String businessUnitId = getCurrentUserBusinessUnitId();
            project.setBusinessUnit(businessUnitRepository.findById(businessUnitId)
                    .orElseThrow(() -> new BusinessException("BUSINESS_UNIT_NOT_FOUND")));
        } else {
            String buId = request.getBusinessUnitId();
            if (buId == null || buId.isBlank()) throw new BusinessException("BUSINESS_UNIT_REQUIRED");
            project.setBusinessUnit(businessUnitRepository.findById(buId)
                    .orElseThrow(() -> new BusinessException("BUSINESS_UNIT_NOT_FOUND:" + buId)));
        }

        project.setStartDate(request.getStartDate());
        project.setPlannedEndDate(request.getPlannedEndDate());
        project.setEstimatedRevenue(request.getEstimatedRevenue());
        project.setEstimatedCost(request.getEstimatedCost());

        String status = calculateStatus(project);
        project.setStatus(statusRepository.findByName(status)
                .orElseThrow(() -> new BusinessException("STATUS_NOT_FOUND")));

        Project saved = projectRepository.save(project);

        if (request.getUsers() != null && !request.getUsers().isEmpty()) {
            for (ProjectUserAssignmentRequest a : request.getUsers()) {
                assignUserToProjectInternal(saved, a, role);
            }
        }

        final String savedId      = saved.getIdProject();
        final String savedName    = saved.getName();
        final String savedBuId    = saved.getBusinessUnit().getIdBusinessUnit();
        final LocalDate savedStart = saved.getStartDate();
        final LocalDate savedEnd   = saved.getPlannedEndDate();

        final String destination       = "/topic/bu/" + savedBuId + "/projects.created";
        final String globalDestination = "/topic/projects.created";
        final ProjectCreatedEvent createdEvent = new ProjectCreatedEvent(
                savedId, savedName, savedBuId, savedStart, savedEnd
        );

        Runnable dispatch = () -> {
            messagingTemplate.convertAndSend(destination, createdEvent);
            messagingTemplate.convertAndSend(globalDestination, createdEvent);
            notificationService.notifyProjectCreated(savedId, savedName, savedBuId);
            checkProjectMarginAndAlert(savedId);
        };

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { dispatch.run(); }
            });
        } else {
            dispatch.run();
        }
    }

    private String calculateStatus(Project project) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = project.getStartDate();
        if (startDate == null) throw new IllegalArgumentException("Start date cannot be null");
        return startDate.isAfter(today) ? "PLANNING" : "IN_PROGRESS";
    }

    // ------------------------------------------------------------------
    // user assignment
    // ------------------------------------------------------------------

    public void assignUserToProjectInternal(Project project, ProjectUserAssignmentRequest a, String role) {
        if (a.getUserId() == null || a.getUserId().isBlank()) {
            throw new BusinessException("USER_REQUIRED");
        }

        User user = userRepository.findById(a.getUserId())
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND"));

        if (user.getBusinessUnit() == null) {
            throw new BusinessException("USER_HAS_NO_BU:" + user.getIdUser());
        }
        if (project.getBusinessUnit() == null) {
            throw new BusinessException("BUSINESS_UNIT_NOT_FOUND");
        }

        String userBu = user.getBusinessUnit().getIdBusinessUnit();
        String projectBu = project.getBusinessUnit().getIdBusinessUnit();

        if ("MANAGER".equalsIgnoreCase(role) && !userBu.equals(projectBu)) {
            throw new BusinessException("USER_BUSINESS_UNIT_MISMATCH");
        }

        UserProjectMonthlyId id = new UserProjectMonthlyId();
        id.setIdProject(project.getIdProject());
        id.setIdUser(user.getIdUser());
        id.setDate(project.getStartDate());

        UserProjectMonthly upm = new UserProjectMonthly();
        upm.setId(id);
        upm.setHourlyCost(a.getHourlyCost());
        upm.setAllocationRate(null);

        userProjectMonthlyRepository.save(upm);
    }

    public List<ProjectUser> getAssignableUsers() {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUserEmail);
        if (currentUser == null) throw new BusinessException("USER_NOT_FOUND");

        String role = currentUser.getRole().getName();
        List<User> users;

        if ("ADMIN".equalsIgnoreCase(role)) {
            users = userRepository.findAll().stream()
                    .filter(user -> !user.getEmail().equals(currentUserEmail))
                    .filter(user -> user.getBusinessUnit() != null)
                    .toList();
        } else if ("MANAGER".equalsIgnoreCase(role)) {
            if (currentUser.getBusinessUnit() == null) throw new BusinessException("BUSINESS_UNIT_NOT_FOUND");
            String currentUserBuId = currentUser.getBusinessUnit().getIdBusinessUnit();
            users = userRepository.findByBusinessUnitId(currentUserBuId).stream()
                    .filter(user -> !user.getEmail().equals(currentUserEmail))
                    .filter(user -> user.getBusinessUnit() != null)
                    .toList();
        } else {
            throw new BusinessException("ROLE_NOT_ALLOWED");
        }

        return users.stream()
                .map(user -> {
                    ProjectUser projectUser = new ProjectUser();
                    projectUser.setUserId(user.getIdUser());
                    projectUser.setName(
                            (user.getFirstName() != null ? user.getFirstName() : "")
                            + (user.getLastName() != null ? " " + user.getLastName() : "")
                    );
                    projectUser.setBusinessUnitId(user.getBusinessUnit().getIdBusinessUnit());
                    return projectUser;
                })
                .toList();
    }

    // ------------------------------------------------------------------
    // critical margin detection — WITH DEDUPLICATION
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public void checkProjectMarginAndAlert(String idProject) {
        Project project = projectRepository.findById(idProject)
                .orElseThrow(() -> new BusinessException("PROJECT_NOT_FOUND"));

        BigDecimal margin = computeProjectMargin(project);

        // above threshold → reset the dedup marker if needed
        if (margin == null || margin.compareTo(CRITICAL_THRESHOLD) >= 0) {
            if (project.getLastAlertedMargin() != null) {
                project.setLastAlertedMargin(null);
                project.setLastAlertedAt(null);
                projectRepository.save(project);
            }
            return;
        }

        // below threshold — dedup rule:
        //    send only if never alerted OR margin got WORSE than the last alert (lower)
        BigDecimal last = project.getLastAlertedMargin();
        boolean shouldAlert = (last == null) || (margin.compareTo(last) < 0);

        if (!shouldAlert) return;

        project.setLastAlertedMargin(margin);
        project.setLastAlertedAt(LocalDateTime.now());
        projectRepository.save(project);

        String buId = project.getBusinessUnit() != null
                ? project.getBusinessUnit().getIdBusinessUnit()
                : null;

        ProjectCriticalEvent event = new ProjectCriticalEvent(
                project.getIdProject(),
                project.getName(),
                buId,
                margin,
                "CRITICAL"
        );

        if (buId != null) {
            messagingTemplate.convertAndSend("/topic/bu/" + buId + "/projects.critical", event);
        }
        messagingTemplate.convertAndSend("/topic/projects.critical", event);

        notificationService.notifyProjectCritical(
                project.getIdProject(),
                project.getName(),
                buId,
                margin
        );
    }

    private BigDecimal computeProjectMargin(Project project) {
        BigDecimal revenue = project.getEstimatedRevenue();
        if (revenue == null || revenue.compareTo(BigDecimal.ZERO) <= 0) return null;

        LocalDate today = LocalDate.now();

        BigDecimal labor = userProjectMonthlyRepository
                .aggregateLaborCostByProject(today)
                .stream()
                .filter(a -> project.getIdProject().equals(a.getIdProject()))
                .map(a -> nvl(a.getLaborCost()))
                .findFirst()
                .orElse(BigDecimal.ZERO);

        BigDecimal extra = extraCostRepository.findAll().stream()
                .filter(e -> project.getIdProject().equals(e.getIdProject()))
                .filter(e -> isActiveOn(e, today))
                .map(e -> nvl(e.getCost()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return marginPct(revenue, labor.add(extra));
    }

    // ------------------------------------------------------------------
    // project detail
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public ProjectDetailDto getProjectDetail(String idProject) {
        if (idProject == null || idProject.isBlank() || "create".equalsIgnoreCase(idProject)) {
            throw new BusinessException("PROJECT_NOT_FOUND");
        }

        Project project = projectRepository.findById(idProject)
                .orElseThrow(() -> new BusinessException("PROJECT_NOT_FOUND"));

        User me = getCurrentUser();
        String role = me.getRole() != null ? me.getRole().getName() : "";
        boolean isAdmin   = "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role);
        boolean isManager = "MANAGER".equalsIgnoreCase(role) || "ROLE_MANAGER".equalsIgnoreCase(role);

        if (!isAdmin && !isManager) throw new BusinessException("ROLE_NOT_ALLOWED");

        if (isManager) {
            if (me.getBusinessUnit() == null || project.getBusinessUnit() == null
                    || !me.getBusinessUnit().getIdBusinessUnit()
                            .equals(project.getBusinessUnit().getIdBusinessUnit())) {
                throw new BusinessException("BU_NOT_ALLOWED");
            }
        }

        List<ExtraCost> extraCosts = extraCostRepository.findAll().stream()
                .filter(e -> project.getIdProject().equals(e.getIdProject()))
                .sorted(Comparator.comparing(ExtraCost::getStartDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<ExtraCostDto> extraCostDtos = extraCosts.stream()
                .map(e -> new ExtraCostDto(
                        e.getIdExtraCost(), e.getDescription(), e.getCost(),
                        e.getStartDate(), e.getEndDate(), e.getIdProject()))
                .toList();

        List<UserProjectMonthly> upmList = userProjectMonthlyRepository.findAll().stream()
                .filter(upm -> project.getIdProject().equals(upm.getId().getIdProject()))
                .toList();

        Map<String, UserProjectMonthly> byUser = new LinkedHashMap<>();
        for (UserProjectMonthly upm : upmList) {
            String uid = upm.getId().getIdUser();
            UserProjectMonthly existing = byUser.get(uid);
            if (existing == null
                    || (upm.getId().getDate() != null
                        && (existing.getId().getDate() == null
                            || upm.getId().getDate().isAfter(existing.getId().getDate())))) {
                byUser.put(uid, upm);
            }
        }

        List<ProjectUserCostDto> userDtos = new ArrayList<>();
        for (UserProjectMonthly upm : byUser.values()) {
            User u = userRepository.findById(upm.getId().getIdUser()).orElse(null);
            if (u == null) continue;
            userDtos.add(new ProjectUserCostDto(
                    u.getIdUser(),
                    (safe(u.getFirstName()) + " " + safe(u.getLastName())).trim(),
                    u.getRole() != null ? u.getRole().getName() : null,
                    u.getSeniorityLevel() != null ? u.getSeniorityLevel().getName() : null,
                    upm.getHourlyCost(),
                    upm.getHoursWorked()
            ));
        }

        LocalDate today = LocalDate.now();
        BigDecimal labor = userProjectMonthlyRepository
                .aggregateLaborCostByProject(today)
                .stream()
                .filter(a -> project.getIdProject().equals(a.getIdProject()))
                .map(a -> nvl(a.getLaborCost()))
                .findFirst()
                .orElse(BigDecimal.ZERO);

        BigDecimal extraActive = extraCosts.stream()
                .filter(e -> isActiveOn(e, today))
                .map(e -> nvl(e.getCost()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal currentRevenue = project.getEstimatedRevenue();
        BigDecimal currentCost = labor.add(extraActive);
        BigDecimal currentMargin = marginPct(currentRevenue, currentCost);
        boolean critical = currentMargin != null && currentMargin.compareTo(CRITICAL_THRESHOLD) < 0;

        return new ProjectDetailDto(
                project.getIdProject(),
                project.getName(),
                project.getStatus() != null ? project.getStatus().getName() : null,
                project.getBusinessUnit() != null ? project.getBusinessUnit().getIdBusinessUnit() : null,
                project.getBusinessUnit() != null ? project.getBusinessUnit().getName() : null,
                project.getStartDate(),
                project.getPlannedEndDate(),
                project.getEndDate(),
                project.getEstimatedRevenue(),
                project.getEstimatedCost(),
                currentRevenue,
                currentCost,
                currentMargin,
                critical,
                extraCostDtos,
                userDtos
        );
    }

    // ------------------------------------------------------------------
    // timesheet — hours
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public List<TimesheetEntryDto> listHours(String idProject) {
        Project project = projectRepository.findById(idProject)
                .orElseThrow(() -> new BusinessException("PROJECT_NOT_FOUND"));

        User me = getCurrentUser();
        String role = me.getRole() != null ? me.getRole().getName() : "";
        boolean isAdmin   = "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role);
        boolean isManager = "MANAGER".equalsIgnoreCase(role) || "ROLE_MANAGER".equalsIgnoreCase(role);

        List<UserProjectMonthly> entries;
        if (isAdmin) {
            entries = userProjectMonthlyRepository.findByProject(idProject);
        } else if (isManager) {
            if (me.getBusinessUnit() == null || project.getBusinessUnit() == null
                    || !me.getBusinessUnit().getIdBusinessUnit()
                            .equals(project.getBusinessUnit().getIdBusinessUnit())) {
                throw new BusinessException("BU_NOT_ALLOWED");
            }
            entries = userProjectMonthlyRepository.findByProject(idProject);
        } else {
            entries = userProjectMonthlyRepository.findByProjectAndUser(idProject, me.getIdUser());
        }

        return entries.stream().map(upm -> {
            User u = userRepository.findById(upm.getId().getIdUser()).orElse(null);
            String fullName = u == null ? null : (safe(u.getFirstName()) + " " + safe(u.getLastName())).trim();

            BigDecimal cost = upm.getHoursWorked() == null || upm.getHourlyCost() == null
                    ? BigDecimal.ZERO
                    : upm.getHoursWorked().multiply(upm.getHourlyCost());

            return new TimesheetEntryDto(
                    upm.getId().getIdUser(), fullName, idProject, project.getName(),
                    upm.getId().getDate(), upm.getHoursWorked(), upm.getHourlyCost(), cost);
        }).toList();
    }

    @Override
    @Transactional
    public TimesheetEntryDto saveHours(String idProject, CreateHoursRequest req) {
        if (req == null || req.getDate() == null || req.getHoursWorked() == null) {
            throw new BusinessException("INVALID_REQUEST");
        }

        Project project = projectRepository.findById(idProject)
                .orElseThrow(() -> new BusinessException("PROJECT_NOT_FOUND"));

        User me = getCurrentUser();
        String role = me.getRole() != null ? me.getRole().getName() : "";
        boolean isAdmin   = "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role);
        boolean isManager = "MANAGER".equalsIgnoreCase(role) || "ROLE_MANAGER".equalsIgnoreCase(role);

        String targetUserId;
        if (isAdmin || isManager) {
            targetUserId = (req.getIdUser() != null && !req.getIdUser().isBlank())
                    ? req.getIdUser() : me.getIdUser();
        } else {
            targetUserId = me.getIdUser();
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND"));

        if (isManager) {
            if (me.getBusinessUnit() == null
                    || target.getBusinessUnit() == null
                    || !me.getBusinessUnit().getIdBusinessUnit()
                            .equals(target.getBusinessUnit().getIdBusinessUnit())
                    || project.getBusinessUnit() == null
                    || !me.getBusinessUnit().getIdBusinessUnit()
                            .equals(project.getBusinessUnit().getIdBusinessUnit())) {
                throw new BusinessException("BU_NOT_ALLOWED");
            }
        }
        if (!isAdmin && !isManager) {
            boolean assigned = userProjectMonthlyRepository
                    .findByProjectAndUser(idProject, me.getIdUser()).stream().findAny().isPresent();
            if (!assigned) throw new BusinessException("NOT_ASSIGNED");
        }

        LocalDate month = req.getDate().withDayOfMonth(1);

        UserProjectMonthly upm = userProjectMonthlyRepository.findOne(idProject, targetUserId, month);

        if (upm == null) {
            UserProjectMonthlyId id = new UserProjectMonthlyId();
            id.setIdProject(idProject);
            id.setIdUser(targetUserId);
            id.setDate(month);
            upm = new UserProjectMonthly();
            upm.setId(id);
            upm.setHourlyCost(req.getHourlyCost() != null ? req.getHourlyCost() : target.getHourlyCost());
            upm.setAllocationRate(null);
        } else if (req.getHourlyCost() != null) {
            upm.setHourlyCost(req.getHourlyCost());
        }

        upm.setHoursWorked(req.getHoursWorked());
        userProjectMonthlyRepository.save(upm);

        // Broadcast: hours changed
        broadcastProjectUpdated(project, "HOURS_ADDED");

        // Margin recompute + dedup critical alert
        checkProjectMarginAndAlert(idProject);

        BigDecimal cost = upm.getHoursWorked().multiply(
                upm.getHourlyCost() != null ? upm.getHourlyCost() : BigDecimal.ZERO);

        return new TimesheetEntryDto(
                target.getIdUser(),
                (safe(target.getFirstName()) + " " + safe(target.getLastName())).trim(),
                idProject, project.getName(), month,
                upm.getHoursWorked(), upm.getHourlyCost(), cost
        );
    }

    @Override
    @Transactional
    public void deleteHours(String idProject, String idUser, LocalDate month) {
        Project project = projectRepository.findById(idProject)
                .orElseThrow(() -> new BusinessException("PROJECT_NOT_FOUND"));

        User me = getCurrentUser();
        String role = me.getRole() != null ? me.getRole().getName() : "";
        boolean isAdmin   = "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role);
        boolean isManager = "MANAGER".equalsIgnoreCase(role) || "ROLE_MANAGER".equalsIgnoreCase(role);

        if (!isAdmin && !isManager) throw new BusinessException("ROLE_NOT_ALLOWED");

        if (isManager) {
            if (me.getBusinessUnit() == null || project.getBusinessUnit() == null
                    || !me.getBusinessUnit().getIdBusinessUnit()
                            .equals(project.getBusinessUnit().getIdBusinessUnit())) {
                throw new BusinessException("BU_NOT_ALLOWED");
            }
        }

        LocalDate normalized = month.withDayOfMonth(1);
        UserProjectMonthly upm = userProjectMonthlyRepository.findOne(idProject, idUser, normalized);
        if (upm == null) throw new BusinessException("HOURS_NOT_FOUND");

        userProjectMonthlyRepository.delete(upm);

        broadcastProjectUpdated(project, "HOURS_REMOVED");
        checkProjectMarginAndAlert(idProject);
    }

    private void broadcastProjectUpdated(Project project, String changeKind) {
        String buId = project.getBusinessUnit() != null
                ? project.getBusinessUnit().getIdBusinessUnit() : null;

        BigDecimal margin = computeProjectMargin(project);

        ProjectUpdatedEvent event = new ProjectUpdatedEvent(
                project.getIdProject(), project.getName(), buId, margin, changeKind
        );

        if (buId != null) {
            messagingTemplate.convertAndSend("/topic/bu/" + buId + "/projects.updated", event);
        }
        messagingTemplate.convertAndSend("/topic/projects.updated", event);
    }
}