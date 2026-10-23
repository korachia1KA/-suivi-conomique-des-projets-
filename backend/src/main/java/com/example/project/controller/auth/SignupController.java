package com.example.project.controller.auth;

import com.example.project.DTO.auth.AuthResponse;
import com.example.project.DTO.auth.SignupRequest;
import com.example.project.config.Translator;
import com.example.project.exception.BusinessException;
import com.example.project.service.auth.SignupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
public class SignupController {

    private final SignupService signupService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {

        String lang = request.getLang() != null ? request.getLang() : "it";
        Translator.setCurrentLocale(new java.util.Locale(lang));

        AuthResponse authResponse = signupService.signup(request);

        return ResponseEntity.ok().body(authResponse);
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmEmail(
            @RequestParam(value = "token", required = false) String queryToken,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String bodyToken = body != null ? body.get("token") : null;
        String token = (queryToken != null && !queryToken.isBlank()) ? queryToken : bodyToken;
        if (token == null || token.isBlank()) {
            throw new BusinessException("TOKEN_REQUIRED");
        }

        String email = signupService.confirmUser(token);
        return ResponseEntity.ok(Map.of("email", email, "message", "ACCOUNT_CONFIRMED"));
    }
}
