package com.example.project.DTO.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) for handling forgot password requests.
 * This class contains the necessary fields for initiating a password reset process.
 */

@Getter
@Setter
public class ForgotPasswordRequest {
    @NotBlank
    @Email
    private String email;
    private String lang;
}