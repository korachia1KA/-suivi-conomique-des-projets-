package com.example.project.DTO.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) for verifying OTP (One-Time Password) requests.
 * This class contains the necessary fields for OTP verification.
 */

@Getter
@Setter
public class VerifyOtpRequest {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String otp;
}