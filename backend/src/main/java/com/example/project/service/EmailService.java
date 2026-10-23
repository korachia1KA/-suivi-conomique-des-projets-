package com.example.project.service;

/**
 * Service interface for managing email notifications.
 * Provides methods for sending various types of emails, such as signup confirmation and password recovery.
 */

public interface EmailService {
    void sendSignupEmail(String to,
                    String firstName,
                    String confirmLink);

    void sendForgotPasswordEmail(String email, String firstName, String otp);
}