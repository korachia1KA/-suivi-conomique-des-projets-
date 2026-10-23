package com.example.project.service.auth;

import com.example.project.config.HashUtil;
import com.example.project.exception.BusinessException;
import com.example.project.model.ResetPassword;
import com.example.project.model.User;
import com.example.project.repository.ResetPasswordRepository;
import com.example.project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service implementation for resetting user passwords.
 * Provides functionality to reset a user's password using a reset token and new password details.
 */

@RequiredArgsConstructor
@Service
public class ResetPasswordServiceImpl implements ResetPasswordService {

    private final ResetPasswordRepository resetPasswordRepository;
    private final UserRepository userRepository;
    private final HashUtil hashUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void resetPasswordWithToken(String resetToken,
                                       String newPassword,
                                       String confirmNewPassword) {

        if (!newPassword.equals(confirmNewPassword)) {
            throw new BusinessException("PASSWORD_MISMATCH");
        }

        String tokenHash = hashUtil.hashSha512(resetToken);

        ResetPassword pr =
                resetPasswordRepository
                        .findTopByResetTokenOrderByIssuedAtDesc(tokenHash)
                        .orElseThrow(() ->
                                new BusinessException("RESET_TOKEN_INVALID"));

        if (pr.getResetTokenExpiresAt() == null ||
                pr.getResetTokenExpiresAt()
                        .isBefore(LocalDateTime.now())) {

            throw new BusinessException("RESET_TOKEN_EXPIRED");
        }

        User user = pr.getUser();

        user.setPassword(
                passwordEncoder.encode(newPassword)
        );

        userRepository.save(user);

        pr.setResetToken(null);
        pr.setResetTokenExpiresAt(null);

        resetPasswordRepository.save(pr);
    }
}