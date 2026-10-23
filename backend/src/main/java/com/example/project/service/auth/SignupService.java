package com.example.project.service.auth;

import com.example.project.DTO.auth.AuthResponse;
import com.example.project.DTO.auth.SignupRequest;

/**
 * Service interface for handling user signup and account confirmation.
 * Provides methods for user registration and confirming user accounts.
 */

public interface SignupService {

    AuthResponse signup(SignupRequest request);

    String confirmUser(String token);
}