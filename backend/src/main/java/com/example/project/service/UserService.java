package com.example.project.service;

import com.example.project.DTO.user.*;
import com.example.project.model.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

public interface UserService extends UserDetailsService {

    UserDetails loadUserByUsername(String username);

    User saveUser(User user);
    boolean existsByEmail(String email);

    void setEnabled(String idUser, boolean enabled);

    UserProfile getMyProfile(String email);

    void updatePersonal(String email, UpdatePersonalRequest dto);
    void updateResidence(String email, UpdateResidenceRequest dto);
    void updateContacts(String email, UpdateContactsRequest dto);
    void updateBank(String email, UpdateBankRequest dto);

    void changePassword(String email, UpdatePasswordRequest request);

    /** Employees listing for the "Employees" page. Scope enforced by role. */
    List<EmployeeListItem> listForRole(String q, String buId, String role);

    // ---------- Admin user management ----------

    /** List all users (ADMIN only). */
    List<AdminUserDto> listAllForAdmin();

    /** Assign or unassign a business unit to a user (ADMIN only). */
    void assignBusinessUnit(String idUser, String businessUnitId);
}