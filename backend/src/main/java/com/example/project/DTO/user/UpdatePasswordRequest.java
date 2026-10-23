package com.example.project.DTO.user;

import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) for updating a user's password.
 * This class contains fields required to update the current password to a new one.
 */

@Getter
@Setter
public class UpdatePasswordRequest {
    private String currentPassword;
    private String newPassword;
    private String confirmNewPassword;
}