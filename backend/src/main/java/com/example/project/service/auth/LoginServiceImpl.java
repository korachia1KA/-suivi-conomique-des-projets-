package com.example.project.service.auth;

import com.example.project.DTO.auth.AuthResponse;
import com.example.project.DTO.auth.LoginRequest;
import com.example.project.config.JwtTokenUtil;
import com.example.project.exception.BusinessException;
import com.example.project.model.RefreshToken;
import com.example.project.model.User;
import com.example.project.repository.UserRepository;
import com.example.project.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;


/**
 * Service implementation for handling user login functionality.
 * Provides methods to authenticate users and generate access and refresh tokens.
 */

@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenUtil jwtTokenUtil;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        // 1. Controlla se l'utente esiste (per distinguere EMAIL_NOT_FOUND)
        User user = userRepository.findByEmail(request.getEmail());
        if (user == null) {
            throw new BusinessException("BAD_CREDENTIALS"); // Non riveliamo se l'email non esiste
        }

        // 2. Tenta l'autenticazione
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (org.springframework.security.authentication.DisabledException e) {
            throw new BusinessException("USER_DISABLED"); // Account non confermato o disabilitato
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            throw new BusinessException("BAD_CREDENTIALS"); // Password errata
        } catch (org.springframework.security.core.AuthenticationException e) {
            throw new BusinessException("AUTHENTICATION_FAILED"); // Altri errori di sicurezza
        }

        // 3. Se l'autenticazione passa, procedi con i token
        try {
            String commonJti = UUID.randomUUID().toString();

            // Creazione Refresh Token
            RefreshToken rt = refreshTokenService.createRefreshToken(
                    user,
                    httpRequest,
                    commonJti,
                    Boolean.TRUE.equals(request.getRememberMe())
            );

            // Generazione Access Token
            UserDetails userDetails = userService.loadUserByUsername(user.getEmail());
            String accessToken = jwtTokenUtil.generateToken(userDetails, commonJti);

            return new AuthResponse(
                    accessToken,
                    rt.getRefreshToken(),
                    "Bearer",
                    user.getIdUser(),
                    user.getEmail(),
                    List.of(user.getRole().getName())
            );
        } catch (Exception e) {
            // Qui gestiamo errori tecnici (DB pieno, JWT fallito, etc.)
            throw new RuntimeException("TOKEN_GENERATION_FAILED", e);
        }
    }
}
