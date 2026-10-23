package com.example.project.DTO.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Data Transfer Object (DTO) for updating personal details.
 * This class contains fields required to update a user's personal information such as name, identification, and birth details.
 */

@Getter @Setter
public class UpdatePersonalRequest {
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    private String fiscalCode;
    private String idCardNumber;
    private LocalDate birthDay;
    private String birthPlace;
}