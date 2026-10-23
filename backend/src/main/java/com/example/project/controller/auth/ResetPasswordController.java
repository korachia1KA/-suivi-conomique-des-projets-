package com.example.project.controller.auth;

import com.example.project.DTO.auth.ResetPasswordRequest;
import com.example.project.service.auth.ResetPasswordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for handling password reset functionality.
 * This controller provides an endpoint for resetting the user's password
 * after verifying the OTP (One-Time Password).
 */

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class ResetPasswordController {

    private final ResetPasswordService resetPasswordService;

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {

        resetPasswordService.resetPasswordWithToken(
                request.getResetToken(),
                request.getNewPassword(),
                request.getConfirmNewPassword()
        );

        return ResponseEntity.ok(Map.of("message", "PASSWORD_UPDATED"));
    }
}