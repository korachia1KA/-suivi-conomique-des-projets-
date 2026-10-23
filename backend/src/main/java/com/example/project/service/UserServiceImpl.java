package com.example.project.service;

import com.example.project.DTO.user.*;
import com.example.project.exception.BusinessException;
import com.example.project.model.BusinessUnit;
import com.example.project.model.User;
import com.example.project.repository.BusinessUnitRepository;
import com.example.project.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BusinessUnitRepository businessUnitRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(PasswordEncoder passwordEncoder,
                           UserRepository userRepository,
                           BusinessUnitRepository businessUnitRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.businessUnitRepository = businessUnitRepository;
    }

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email);

        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        String roleName = (user.getRole() != null)
                ? "ROLE_" + user.getRole().getName()
                : "ROLE_USER";

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(roleName)
                .disabled(!Boolean.TRUE.equals(user.getEnabled()))
                .build();
    }

    @Override
    @Transactional
    public void setEnabled(String idUser, boolean enabled) {
        userRepository.updateEnabledStatus(idUser, enabled);
    }

    @Override
    @Transactional
    public UserProfile getMyProfile(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new BusinessException("USER_NOT_FOUND");
        }
        return new UserProfile(
                user.getEmail(),
                user.getRole() != null ? user.getRole().getName() : null,
                user.getBusinessUnit() != null ? user.getBusinessUnit().getIdBusinessUnit() : null,
                user.getFirstName(),
                user.getLastName(),
                user.getFiscalCode(),
                user.getIdCardNumber(),
                user.getBirthDay(),
                user.getBirthPlace(),
                user.getAddress(),
                user.getCity(),
                user.getPhoneNumber(),
                user.getIban(),
                user.getIbanHolder()
        );
    }

    private User getUserOrThrow(String email) {
        User u = userRepository.findByEmail(email);
        if (u == null) throw new BusinessException("USER_NOT_FOUND");
        return u;
    }

    @Override
    @Transactional
    public void updatePersonal(String email, UpdatePersonalRequest dto) {
        User u = getUserOrThrow(email);
        if (dto.getFirstName() != null) u.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) u.setLastName(dto.getLastName());
        if (dto.getFiscalCode() != null) u.setFiscalCode(dto.getFiscalCode());
        if (dto.getIdCardNumber() != null) u.setIdCardNumber(dto.getIdCardNumber());
        if (dto.getBirthPlace() != null) u.setBirthPlace(dto.getBirthPlace());
        if (dto.getBirthDay() != null) u.setBirthDay(dto.getBirthDay());
        userRepository.save(u);
    }

    @Override
    @Transactional
    public void updateResidence(String email, UpdateResidenceRequest dto) {
        User u = getUserOrThrow(email);
        if (dto.getAddress() != null) u.setAddress(dto.getAddress());
        if (dto.getCity() != null) u.setCity(dto.getCity());
        userRepository.save(u);
    }

    @Override
    @Transactional
    public void updateContacts(String email, UpdateContactsRequest dto) {
        User u = getUserOrThrow(email);
        if (dto.getPhoneNumber() != null) u.setPhoneNumber(dto.getPhoneNumber());
        userRepository.save(u);
    }

    @Override
    @Transactional
    public void updateBank(String email, UpdateBankRequest dto) {
        User u = getUserOrThrow(email);
        if (dto.getIban() != null) u.setIban(dto.getIban());
        if (dto.getIbanHolder() != null) u.setIbanHolder(dto.getIbanHolder());
        userRepository.save(u);
    }

    @Override
    @Transactional
    public void changePassword(String email, UpdatePasswordRequest request) {
        if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank())
            throw new BusinessException("PASSWORD_CURRENT_REQUIRED");
        if (request.getNewPassword() == null || request.getNewPassword().isBlank())
            throw new BusinessException("PASSWORD_NEW_REQUIRED");
        if (request.getConfirmNewPassword() == null || request.getConfirmNewPassword().isBlank())
            throw new BusinessException("PASSWORD_CONFIRM_REQUIRED");
        if (!request.getNewPassword().equals(request.getConfirmNewPassword()))
            throw new BusinessException("PASSWORD_MISMATCH");

        User user = userRepository.findByEmail(email);
        if (user == null) throw new BusinessException("USER_NOT_FOUND");

        boolean matches = passwordEncoder.matches(request.getCurrentPassword(), user.getPassword());
        if (!matches) throw new BusinessException("PASSWORD_CURRENT_INVALID");
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword()))
            throw new BusinessException("PASSWORD_SAME_AS_OLD");

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public List<EmployeeListItem> listForRole(String q, String buId, String role) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User me = userRepository.findByEmail(email);
        if (me == null) throw new BusinessException("USER_NOT_FOUND");

        String myRole = (me.getRole() != null) ? me.getRole().getName() : "";
        boolean isAdmin   = "ADMIN".equalsIgnoreCase(myRole) || "ROLE_ADMIN".equalsIgnoreCase(myRole);
        boolean isManager = "MANAGER".equalsIgnoreCase(myRole) || "ROLE_MANAGER".equalsIgnoreCase(myRole);

        if (!isAdmin && !isManager) throw new BusinessException("ROLE_NOT_ALLOWED");

        String scopedBuId = buId;
        if (isManager) {
            scopedBuId = (me.getBusinessUnit() != null)
                    ? me.getBusinessUnit().getIdBusinessUnit()
                    : null;
            if (scopedBuId == null) return List.of();
        }

        return userRepository.searchEmployees(
                        blankToNull(q), blankToNull(scopedBuId), blankToNull(role))
                .stream()
                .map(u -> new EmployeeListItem(
                        u.getIdUser(),
                        u.getFirstName(),
                        u.getLastName(),
                        u.getEmail(),
                        u.getRole() != null ? u.getRole().getName() : null,
                        u.getSeniorityLevel() != null ? u.getSeniorityLevel().getName() : null,
                        u.getBusinessUnit() != null ? u.getBusinessUnit().getIdBusinessUnit() : null,
                        u.getBusinessUnit() != null ? u.getBusinessUnit().getName() : null
                ))
                .toList();
    }

    // ---------- Admin user management ----------

    @Override
    @Transactional
    public List<AdminUserDto> listAllForAdmin() {
        User me = getCurrentAuthenticatedUser();
        String role = me.getRole() != null ? me.getRole().getName() : "";
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role);
        if (!isAdmin) throw new BusinessException("ROLE_NOT_ALLOWED");

        return userRepository.findAllOrdered().stream()
                .map(u -> new AdminUserDto(
                        u.getIdUser(),
                        u.getEmail(),
                        u.getFirstName(),
                        u.getLastName(),
                        u.getRole() != null ? u.getRole().getName() : null,
                        u.getBusinessUnit() != null ? u.getBusinessUnit().getIdBusinessUnit() : null,
                        u.getBusinessUnit() != null ? u.getBusinessUnit().getName() : null,
                        u.getSeniorityLevel() != null ? u.getSeniorityLevel().getName() : null,
                        u.getEnabled()
                ))
                .toList();
    }

    @Override
    @Transactional
    public void assignBusinessUnit(String idUser, String businessUnitId) {
        User me = getCurrentAuthenticatedUser();
        String role = me.getRole() != null ? me.getRole().getName() : "";
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role);
        if (!isAdmin) throw new BusinessException("ROLE_NOT_ALLOWED");

        User target = userRepository.findById(idUser)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND"));

        // Prevent admin from removing its own BU (there's none anyway) or reassigning itself
        if (target.getIdUser().equals(me.getIdUser())) {
            throw new BusinessException("CANNOT_MODIFY_SELF");
        }

        if (businessUnitId == null || businessUnitId.isBlank()) {
            target.setBusinessUnit(null);
        } else {
            BusinessUnit bu = businessUnitRepository.findById(businessUnitId)
                    .orElseThrow(() -> new BusinessException("BUSINESS_UNIT_NOT_FOUND"));
            target.setBusinessUnit(bu);
        }
        userRepository.save(target);
    }

    // ---------- helpers ----------

    private User getCurrentAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User me = userRepository.findByEmail(email);
        if (me == null) throw new BusinessException("USER_NOT_FOUND");
        return me;
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}