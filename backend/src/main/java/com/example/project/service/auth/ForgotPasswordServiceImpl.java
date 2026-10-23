package com.example.project.service.auth;

import com.example.project.config.HashUtil;
import com.example.project.exception.BusinessException;
import com.example.project.model.ResetPassword;
import com.example.project.model.User;
import com.example.project.repository.ResetPasswordRepository;
import com.example.project.repository.UserRepository;
import com.example.project.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service implementation for handling forgot password functionality.
 * Provides methods to send OTPs and verify them to issue reset tokens.
 */

@RequiredArgsConstructor
@Service
public class ForgotPasswordServiceImpl implements ForgotPasswordService {

    private final ResetPasswordRepository resetPasswordRepository;
    private final UserRepository userRepository;
    private final HashUtil hashUtil;
    private final EmailService emailService;

    private static final int OTP_TTL_MIN = 10;
    private static final int RESET_TOKEN_TTL_MIN = 15;
    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_OTP_REQUESTS = 3;
    private static final int OTP_REQUEST_WINDOW_MIN = 15;

    private final SecureRandom rnd = new SecureRandom();

    @Override
    public void sendOtp(String email) {

        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new BusinessException("USER_NOT_FOUND");
        }

        String userName =
                user.getFirstName() != null ? user.getFirstName() : "User";

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart =
                now.minusMinutes(OTP_REQUEST_WINDOW_MIN);

        long requests =
                resetPasswordRepository
                        .countByUser_EmailAndIssuedAtAfter(email, windowStart);

        if (requests >= MAX_OTP_REQUESTS) {
            throw new BusinessException("OTP_TOO_MANY_REQUESTS");
        }

        resetPasswordRepository.invalidatePreviousOtps(email);

        String otp = String.format("%06d", rnd.nextInt(1_000_000));
        String otpHash = hashUtil.hashSha512(otp);

        ResetPassword entity = new ResetPassword();
        entity.setIdPasswordReset(UUID.randomUUID().toString());
        entity.setUser(user);
        entity.setOtp(otpHash);
        entity.setIssuedAt(now);
        entity.setExpiresAt(now.plusMinutes(OTP_TTL_MIN));
        entity.setAttempts(0);
        entity.setUsed(false);
        entity.setResetToken(null);
        entity.setResetTokenExpiresAt(null);

        resetPasswordRepository.save(entity);

        emailService.sendForgotPasswordEmail(email, userName, otp);
    }

    @Override
    public String verifyOtpAndIssueResetToken(String email, String otp) {

        LocalDateTime now = LocalDateTime.now();

        ResetPassword pr =
                resetPasswordRepository
                        .findTopByUser_EmailAndUsedFalseAndExpiresAtAfterOrderByIssuedAtDesc(email, now)
                        .orElseThrow(() ->
                                new BusinessException("OTP_INVALID_OR_EXPIRED"));

        if (pr.getAttempts() >= MAX_ATTEMPTS) {
            throw new BusinessException("OTP_TOO_MANY_ATTEMPTS");
        }

        String otpHash = hashUtil.hashSha512(otp);

        if (!otpHash.equals(pr.getOtp())) {
            pr.setAttempts(pr.getAttempts() + 1);
            resetPasswordRepository.save(pr);
            throw new BusinessException("OTP_INVALID");
        }

        String resetToken =
                UUID.randomUUID().toString().replace("-", "");

        pr.setResetToken(hashUtil.hashSha512(resetToken));
        pr.setResetTokenExpiresAt(
                now.plusMinutes(RESET_TOKEN_TTL_MIN));

        pr.setUsed(true);
        resetPasswordRepository.save(pr);

        return resetToken;
    }
}