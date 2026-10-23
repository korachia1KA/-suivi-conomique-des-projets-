package com.example.project.controller.auth;

import com.example.project.config.JwtTokenUtil;
import com.example.project.model.RefreshToken;
import com.example.project.service.UserService;
import com.example.project.service.auth.RefreshTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller for handling authentication-related endpoints.
 * Provides functionality for refreshing JWT access tokens using refresh tokens.
 */

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final RefreshTokenService refreshTokenService;

    private final JwtTokenUtil jwtTokenUtil;

    private final UserService userService;

    public AuthController(RefreshTokenService refreshTokenService, JwtTokenUtil jwtTokenUtil, UserService userService) {
        this.refreshTokenService = refreshTokenService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userService = userService;
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(name = "refreshToken", required = false) String refreshToken) {

        if (refreshToken == null) {
            return ResponseEntity.status(401).body(Map.of("message", "NO_REFRESH_TOKEN"));
        }

        RefreshToken tokenEntity = refreshTokenService.validateRefreshToken(refreshToken);

        String email = tokenEntity.getUser().getEmail();
        String jti = tokenEntity.getJti();

        UserDetails userDetails = userService.loadUserByUsername(email);

        String newAccessToken = jwtTokenUtil.generateToken(userDetails, jti);

        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }

}
