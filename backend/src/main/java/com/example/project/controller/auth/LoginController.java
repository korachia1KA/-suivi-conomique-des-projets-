package com.example.project.controller.auth;

import com.example.project.DTO.auth.AuthResponse;
import com.example.project.DTO.auth.LoginRequest;
import com.example.project.service.auth.RefreshTokenService;
import com.example.project.service.auth.LoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for handling user signup and email confirmation.
 * Provides endpoints for user registration and account confirmation.
 */

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class LoginController {

    private LoginService loginService;

    private RefreshTokenService refreshTokenService;

    @Autowired
    public LoginController(LoginService loginService, RefreshTokenService refreshTokenService) {
        this.loginService = loginService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {

        AuthResponse authResponse = loginService.login(request, httpRequest);

        String refreshTokenValue = authResponse.getRefreshToken() != null ? authResponse.getRefreshToken() : "";

        boolean remember = Boolean.TRUE.equals(request.getRememberMe());

        int maxAge = remember
                ? 60 * 60 * 24 * 30   // 30 days
                : 60 * 60 * 24;        // 1 day

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshTokenValue)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(maxAge)
                .sameSite("Lax")
                .build();

        authResponse.setRefreshToken(null);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(authResponse);

    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(name = "refreshToken", required = false) String refreshToken) {

        if (refreshToken != null) {
            refreshTokenService.revokeRefreshToken(refreshToken);
        }

        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(Map.of("message", "LOGOUT_SUCCESS"));
    }
}