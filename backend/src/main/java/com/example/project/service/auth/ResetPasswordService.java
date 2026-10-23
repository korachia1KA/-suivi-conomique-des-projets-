package com.example.project.service.auth;

/**
 * Service interface for resetting user passwords.
 * Provides a method to reset a password using a reset token and new password details.
 */

public interface ResetPasswordService {
    void resetPasswordWithToken(String resetToken, String newPassword, String confirmNewPassword);
}