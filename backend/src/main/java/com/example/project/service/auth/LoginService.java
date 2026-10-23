package com.example.project.service.auth;

import com.example.project.DTO.auth.AuthResponse;
import com.example.project.DTO.auth.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Interface for LoginService.
 * Provides a method for handling user login functionality.
 */

public interface LoginService {
    AuthResponse login(LoginRequest request, HttpServletRequest httpServletRequest);
}
