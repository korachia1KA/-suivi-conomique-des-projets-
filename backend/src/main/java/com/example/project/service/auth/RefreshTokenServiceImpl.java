package com.example.project.service.auth;

import com.example.project.config.HashUtil;
import com.example.project.exception.BusinessException;
import com.example.project.model.RefreshToken;
import com.example.project.model.User;
import com.example.project.repository.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service implementation for managing refresh tokens.
 * Provides methods for creating, validating, revoking, and checking the status of refresh tokens.
 */

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    private final HashUtil hashUtils;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository, HashUtil hashUtils) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.hashUtils = hashUtils;
    }

    @Override
    public RefreshToken createRefreshToken(User user, HttpServletRequest request, String accessTokenJti, boolean rememberMe) {

        String rawRefreshToken = UUID.randomUUID().toString();
        String hashedRefreshToken = hashUtils.hashSha512(rawRefreshToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setIdRefreshToken(UUID.randomUUID().toString());
        refreshToken.setRefreshToken(hashedRefreshToken);
        refreshToken.setJti(accessTokenJti);
        refreshToken.setUser(user);

        refreshToken.setIssuedAt(LocalDateTime.now());
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(rememberMe ? 30 : 1));
        refreshToken.setRevoked(false);
        refreshToken.setUserAgent(request.getHeader("User-Agent"));
        refreshToken.setIpAddress(request.getRemoteAddr());

        refreshTokenRepository.save(refreshToken);

        refreshToken.setRefreshToken(rawRefreshToken);
        return refreshToken;
    }

    @Override
    public RefreshToken validateRefreshToken(String rawToken) {

        String hashed = hashUtils.hashSha512(rawToken);

        RefreshToken token = refreshTokenRepository
                .findByRefreshToken(hashed)
                .orElseThrow(() -> new BusinessException("INVALID_REFRESH_TOKEN"));

        if (token.getRevoked() || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("REFRESH_TOKEN_EXPIRED_OR_REVOKED");
        }

        return token;
    }

    public void revokeRefreshToken(String rawRefreshToken) {
        String hashed = hashUtils.hashSha512(rawRefreshToken);
        refreshTokenRepository.findByRefreshToken(hashed)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    @Override
    public boolean isSessionActive(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        return refreshTokenRepository.existsByJtiAndRevokedFalseAndExpiresAtAfter(jti, LocalDateTime.now());
    }

}