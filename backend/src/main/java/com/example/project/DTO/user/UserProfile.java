package com.example.project.DTO.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.sql.Date;
import java.time.LocalDate;

/**
 * Data Transfer Object (DTO) representing a user's profile.
 * This class contains fields that store various details about the user.
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserProfile {

    private String email;
    private String roleName;
    private String businessUnitId;
    private String firstName;
    private String lastName;
    private String fiscalCode;
    private String idCardNumber;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate birthDay;
    private String birthPlace;
    private String address;
    private String city;
    private String phoneNumber;
    private String iban;
    private String ibanHolder;
}
