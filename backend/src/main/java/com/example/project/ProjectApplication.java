package com.example.project;

import com.example.project.model.BusinessUnit;
import com.example.project.model.Role;
import com.example.project.model.SeniorityLevel;
import com.example.project.model.Status;
import com.example.project.model.User;
import com.example.project.repository.BusinessUnitRepository;
import com.example.project.repository.RoleRepository;
import com.example.project.repository.SeniorityLevelRepository;
import com.example.project.repository.StatusRepository;
import com.example.project.repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@SpringBootApplication
public class ProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjectApplication.class, args);
    }

    @Bean
    CommandLineRunner initData(
            RoleRepository roleRepository,
            UserRepository userRepository,
            StatusRepository statusRepository,
            SeniorityLevelRepository seniorityLevelRepository,
            BusinessUnitRepository businessUnitRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            // 1. Roles
            createRoleIfNotExists(roleRepository, "1", "MANAGER");
            createRoleIfNotExists(roleRepository, "2", "EMPLOYEE");
            createRoleIfNotExists(roleRepository, "3", "ADMIN");

            // 2. Project statuses  (required by ProjectServiceImpl.addProject)
            createStatusIfNotExists(statusRepository, "1", "PLANNING");
            createStatusIfNotExists(statusRepository, "2", "IN_PROGRESS");
            createStatusIfNotExists(statusRepository, "3", "COMPLETED");

            // 3. Seniority levels  (used by the dashboard)
            createSeniorityIfNotExists(seniorityLevelRepository, "1", "JUNIOR");
            createSeniorityIfNotExists(seniorityLevelRepository, "2", "MID");
            createSeniorityIfNotExists(seniorityLevelRepository, "3", "SENIOR");

            // 4. Default Business Unit  (so a MANAGER can create a project on a fresh DB)
            createBusinessUnitIfNotExists(
                    businessUnitRepository,
                    "bu-1",
                    "Business Unit 1",
                    new BigDecimal("500000.00"),
                    new BigDecimal("25.00")
            );

            // 5. Test accounts
            createTestUser(
                    userRepository,
                    roleRepository,
                    businessUnitRepository,
                    seniorityLevelRepository,
                    passwordEncoder,
                    "admin-teszt",
                    "admin@tzest.com",
                    "Admizn123!",
                    "Admzin",
                    "Test",
                    "ADMIN",
                    null,
                    "SENIOR"
            );

            createTestUser(
                    userRepository,
                    roleRepository,
                    businessUnitRepository,
                    seniorityLevelRepository,
                    passwordEncoder,
                    "managerz-test",
                    "managerz@test.com",
                    "Managzer123!",
                    "Managzer",
                    "Test",
                    "MANAGER",
                    "bu-1",
                    "SENIOR"
            );
        };
    }

    private void createRoleIfNotExists(RoleRepository roleRepository, String id, String name) {
        if (!roleRepository.existsByName(name)) {
            Role role = new Role();
            role.setIdRole(id);
            role.setName(name);
            roleRepository.save(role);
            System.out.println("Rôle créé : " + name);
        }
    }

    private void createStatusIfNotExists(StatusRepository statusRepository, String id, String name) {
        if (statusRepository.findByName(name).isEmpty()) {
            Status s = new Status();
            s.setIdStatus(id);
            s.setName(name);
            statusRepository.save(s);
            System.out.println("Statut créé : " + name);
        }
    }

    private void createSeniorityIfNotExists(SeniorityLevelRepository repo, String id, String name) {
        if (!repo.existsByName(name)) {
            SeniorityLevel s = new SeniorityLevel();
            s.setIdSeniorityLevel(id);
            s.setName(name);
            repo.save(s);
            System.out.println("Seniority créée : " + name);
        }
    }

    private void createBusinessUnitIfNotExists(
            BusinessUnitRepository repo,
            String id,
            String name,
            BigDecimal annualBudget,
            BigDecimal expectedMargin) {

        if (repo.findById(id).isEmpty()) {
            BusinessUnit bu = new BusinessUnit();
            bu.setIdBusinessUnit(id);
            bu.setName(name);
            bu.setAnnualBudget(annualBudget);
            bu.setExpectedAnnualMargin(expectedMargin);
            repo.save(bu);
            System.out.println("Business Unit créée : " + name);
        }
    }

    private void createTestUser(
            UserRepository userRepository,
            RoleRepository roleRepository,
            BusinessUnitRepository businessUnitRepository,
            SeniorityLevelRepository seniorityLevelRepository,
            PasswordEncoder passwordEncoder,
            String id,
            String email,
            String rawPassword,
            String firstName,
            String lastName,
            String roleName,
            String businessUnitId,
            String seniorityName) {

        if (userRepository.existsByEmail(email)) {
            System.out.println("Utilisateur déjà existant : " + email);
            return;
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Rôle introuvable : " + roleName));

        User user = new User();
        user.setIdUser(id);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        user.setRole(role);

        if (businessUnitId != null) {
            user.setBusinessUnit(businessUnitRepository.findById(businessUnitId)
                    .orElseThrow(() -> new IllegalStateException("BU introuvable : " + businessUnitId)));
        }

        if (seniorityName != null) {
            user.setSeniorityLevel(seniorityLevelRepository.findByName(seniorityName)
                    .orElseThrow(() -> new IllegalStateException("Seniority introuvable : " + seniorityName)));
        }

        userRepository.save(user);

        System.out.println("Utilisateur créé : " + email
                + " | Rôle : " + roleName
                + (businessUnitId != null ? " | BU : " + businessUnitId : ""));
    }
}