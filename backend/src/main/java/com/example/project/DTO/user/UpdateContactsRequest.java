package com.example.project.DTO.user;

import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) for updating contact details.
 * This class contains fields required to update the user's phone number.
 */

@Getter @Setter
public class UpdateContactsRequest {
    private String phoneNumber;
}