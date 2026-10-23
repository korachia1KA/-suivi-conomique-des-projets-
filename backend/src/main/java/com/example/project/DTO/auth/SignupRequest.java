package com.example.project.DTO.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) for handling signup requests.
 * This class contains the necessary fields for user registration.
 */

@Getter
@Setter
@RequiredArgsConstructor
public class SignupRequest {

    @NotBlank(message = "error.email.required")
    @Email(message = "error.email.invalid")
    private String email;

    @NotBlank(message = "error.password.required")
    @Size(min = 8, message = "error.password.length")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$",
            message = "error.password.complex"
    )
    private String password;

    private String lang;
}