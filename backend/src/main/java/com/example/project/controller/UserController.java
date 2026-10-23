package com.example.project.controller;

import com.example.project.DTO.user.*;
import com.example.project.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserProfile> getProfile(Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(userService.getMyProfile(authentication.getName()));
    }

    @PutMapping("/profile/personal")
    public ResponseEntity<Void> updatePersonal(@Valid @RequestBody UpdatePersonalRequest dto, Authentication auth) {
        userService.updatePersonal(auth.getName(), dto);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/profile/residence")
    public ResponseEntity<Void> updateResidence(@Valid @RequestBody UpdateResidenceRequest dto, Authentication auth) {
        userService.updateResidence(auth.getName(), dto);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/profile/contacts")
    public ResponseEntity<Void> updateContacts(@Valid @RequestBody UpdateContactsRequest dto, Authentication auth) {
        userService.updateContacts(auth.getName(), dto);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/profile/bank")
    public ResponseEntity<Void> updateBank(@Valid @RequestBody UpdateBankRequest dto, Authentication auth) {
        userService.updateBank(auth.getName(), dto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/profile/change-password")
    public ResponseEntity<Void> changePassword(Authentication authentication, @RequestBody UpdatePasswordRequest request) {
        userService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<EmployeeListItem>> listEmployees(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String buId,
            @RequestParam(required = false) String role
    ) {
        return ResponseEntity.ok(userService.listForRole(q, buId, role));
    }

    // ---------- Admin user management ----------

    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminUserDto>> listAllForAdmin() {
        return ResponseEntity.ok(userService.listAllForAdmin());
    }

    @PutMapping("/{idUser}/business-unit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> assignBusinessUnit(
            @PathVariable String idUser,
            @RequestBody AssignBusinessUnitRequest body
    ) {
        userService.assignBusinessUnit(idUser, body.getBusinessUnitId());
        return ResponseEntity.noContent().build();
    }
}