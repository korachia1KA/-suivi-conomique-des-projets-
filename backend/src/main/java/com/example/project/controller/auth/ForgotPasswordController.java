package com.example.project.controller.auth;

import com.example.project.DTO.auth.ForgotPasswordRequest;
import com.example.project.DTO.auth.VerifyOtpRequest;
import com.example.project.config.Translator;
import com.example.project.service.auth.ForgotPasswordService;
import com.example.project.service.auth.ResetPasswordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;

/**
 * Controller for handling password recovery through OTP (One-Time Password).
 * Provides endpoints for generating and verifying OTPs.
 */

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class ForgotPasswordController {

    private final ForgotPasswordService forgotPasswordService;

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        String lang = request.getLang() != null ? request.getLang() : "it";
        Translator.setCurrentLocale(new Locale(lang));

        forgotPasswordService.sendOtp(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "OTP_SENT"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody @Valid VerifyOtpRequest req) {

        String resetToken = forgotPasswordService.verifyOtpAndIssueResetToken(
                req.getEmail(),
                req.getOtp()
        );

        return ResponseEntity.ok(Map.of("resetToken", resetToken));
    }
}