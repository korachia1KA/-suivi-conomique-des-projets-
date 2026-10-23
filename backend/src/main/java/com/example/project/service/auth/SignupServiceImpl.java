package com.example.project.service.auth;

import com.example.project.DTO.auth.AuthResponse;
import com.example.project.DTO.auth.SignupRequest;
import com.example.project.config.HashUtil;
import com.example.project.exception.BusinessException;
import com.example.project.model.Role;
import com.example.project.model.User;
import com.example.project.model.VerificationToken;
import com.example.project.repository.RoleRepository;
import com.example.project.repository.VerificationTokenRepository;
import com.example.project.service.EmailService;
import com.example.project.service.UserService;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Service implementation for handling user signup and account confirmation.
 * Provides methods for registering new users and confirming their accounts.
 */

@Service
public class SignupServiceImpl implements SignupService {

    private final UserService userService;

    private final PasswordEncoder passwordEncoder;

    private final RoleRepository roleRepository;

    private final EmailService emailService;

    private final VerificationTokenRepository verificationTokenRepository;

    private final HashUtil hashUtils;

    public SignupServiceImpl(UserService userService, PasswordEncoder passwordEncoder, RoleRepository roleRepository, EmailService emailService, VerificationTokenRepository verificationTokenRepository, HashUtil hashUtils) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.emailService = emailService;
        this.verificationTokenRepository = verificationTokenRepository;
        this.hashUtils = hashUtils;
    }

    @Override
    @Transactional
    public AuthResponse signup(SignupRequest request) {

        if (userService.existsByEmail(request.getEmail())) {
            throw new BusinessException("EMAIL_DUPLICATE");
        }

        User user = new User();
        user.setIdUser(UUID.randomUUID().toString());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Role defaultRole = roleRepository.findByName("EMPLOYEE")
                .orElseThrow(() -> new BusinessException("ROLE_NOT_FOUND"));
        user.setRole(defaultRole);

        user.setEnabled(false);
        user = userService.saveUser(user);

        String rawToken = UUID.randomUUID().toString();
        String hashedToken = hashUtils.hashSha512(rawToken);
        VerificationToken vToken = new VerificationToken();
        vToken.setVerificationToken(hashedToken);
        vToken.setUser(user);
        vToken.setIssuedAt(LocalDateTime.now());
        vToken.setExpiresAt(LocalDateTime.now().plusHours(24));  // Token expires in 24 hours
        verificationTokenRepository.save(vToken);

        String confirmLink = "http://localhost:4200/confirm?token=" + rawToken;

        emailService.sendSignupEmail(user.getEmail(), user.getFirstName(), confirmLink);

        return new AuthResponse(null, null, "Bearer", user.getIdUser(), user.getEmail(), new ArrayList<>());
    }

    @Transactional
    public String confirmUser(String rawToken) {

        String hashedTokenFromLink = hashUtils.hashSha512(rawToken);

        VerificationToken vToken = verificationTokenRepository.findByVerificationToken(hashedTokenFromLink)
                .orElseThrow(() -> new BusinessException("TOKEN_INVALID"));

        if (vToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            verificationTokenRepository.delete(vToken);
            throw new BusinessException("TOKEN_EXPIRED");
        }

        User user = vToken.getUser();
        userService.setEnabled(user.getIdUser(), true);

        verificationTokenRepository.findById(vToken.getIdVerificationToken()).ifPresent(verificationTokenRepository::delete);

        return user.getEmail();
    }
}