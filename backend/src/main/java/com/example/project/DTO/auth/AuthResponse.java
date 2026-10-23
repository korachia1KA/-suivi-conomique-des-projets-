package com.example.project.DTO.auth;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Data Transfer Object (DTO) representing the authentication response.
 * This class contains information about the authentication tokens, user details, and roles.
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private String accessToken;
    private String refreshToken;
    private String type = "Bearer";
    private String idUser;
    private String email;
    private List<String> roles;
}