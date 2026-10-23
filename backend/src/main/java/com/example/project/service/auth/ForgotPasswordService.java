package com.example.project.service.auth;

/**
 * Interface for ForgotPasswordService.
 * Provides methods for handling forgot password functionality,
 * including sending OTPs and verifying them to issue reset tokens.
 */

public interface ForgotPasswordService {
    void sendOtp(String email);
    String verifyOtpAndIssueResetToken(String email, String otp);
}