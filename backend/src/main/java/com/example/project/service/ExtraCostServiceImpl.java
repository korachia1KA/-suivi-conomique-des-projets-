package com.example.project.service;

import com.example.project.DTO.project.CreateExtraCostRequest;
import com.example.project.DTO.project.ExtraCostDto;
import com.example.project.exception.BusinessException;
import com.example.project.model.ExtraCost;
import com.example.project.model.Project;
import com.example.project.model.User;
import com.example.project.repository.ExtraCostRepository;
import com.example.project.repository.ProjectRepository;
import com.example.project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExtraCostServiceImpl implements ExtraCostService {

    private final ExtraCostRepository extraCostRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ExtraCostDto create(String idProject, CreateExtraCostRequest req) {

        Project project = projectRepository.findById(idProject)
                .orElseThrow(() -> new BusinessException("PROJECT_NOT_FOUND"));

        enforceScope(project);

        ExtraCost e = new ExtraCost();
        e.setIdExtraCost(UUID.randomUUID().toString());
        e.setDescription(req.getDescription());
        e.setCost(req.getCost());
        e.setStartDate(req.getStartDate());
        e.setEndDate(req.getEndDate());
        e.setIdProject(idProject);

        ExtraCost saved = extraCostRepository.save(e);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExtraCostDto> listByProject(String idProject) {

        Project project = projectRepository.findById(idProject)
                .orElseThrow(() -> new BusinessException("PROJECT_NOT_FOUND"));

        enforceScope(project);

        return extraCostRepository.findAll().stream()
                .filter(e -> idProject.equals(e.getIdProject()))
                .map(this::toDto)
                .toList();
    }

    /**
     * ADMIN: any project.
     * MANAGER: only projects in their BU.
     * Others: forbidden.
     */
    private void enforceScope(Project project) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User me = userRepository.findByEmail(email);
        if (me == null) throw new BusinessException("USER_NOT_FOUND");

        String role = me.getRole() != null ? me.getRole().getName() : "";

        boolean isAdmin = "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role);
        if (isAdmin) return;

        boolean isManager = "MANAGER".equalsIgnoreCase(role) || "ROLE_MANAGER".equalsIgnoreCase(role);
        if (!isManager) throw new BusinessException("ROLE_NOT_ALLOWED");

        if (me.getBusinessUnit() == null || project.getBusinessUnit() == null) {
            throw new BusinessException("BUSINESS_UNIT_NOT_FOUND");
        }
        String myBu = me.getBusinessUnit().getIdBusinessUnit();
        String projectBu = project.getBusinessUnit().getIdBusinessUnit();
        if (!myBu.equals(projectBu)) {
            throw new BusinessException("BU_NOT_ALLOWED");
        }
    }

    private ExtraCostDto toDto(ExtraCost e) {
        return new ExtraCostDto(
                e.getIdExtraCost(),
                e.getDescription(),
                e.getCost(),
                e.getStartDate(),
                e.getEndDate(),
                e.getIdProject()
        );
    }
}