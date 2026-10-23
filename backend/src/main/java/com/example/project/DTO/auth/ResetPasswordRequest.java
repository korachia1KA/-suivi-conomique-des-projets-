package com.example.project.DTO.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) for handling reset password requests.
 * This class contains the necessary fields for resetting a user's password.
 */

@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank
    private String resetToken;

    @NotBlank
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$",
            message = "error.password.complex"
    )
    private String newPassword;

    @NotBlank
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$",
            message = "error.password.complex"
    )
    private String confirmNewPassword;
}