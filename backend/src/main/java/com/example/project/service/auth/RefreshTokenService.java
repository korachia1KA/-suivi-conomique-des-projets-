package com.example.project.service.auth;

import com.example.project.model.RefreshToken;
import com.example.project.model.User;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Service interface for managing refresh tokens.
 * Provides methods for creating, validating, revoking, and checking the status of refresh tokens.
 */

public interface RefreshTokenService {
    RefreshToken createRefreshToken(User user, HttpServletRequest request, String jti, boolean rememberMe);

    RefreshToken validateRefreshToken(String rawToken);

    void revokeRefreshToken(String rawRefreshToken);

    boolean isSessionActive(String jti);
}