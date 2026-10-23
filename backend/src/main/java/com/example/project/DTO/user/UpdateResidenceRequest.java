package com.example.project.DTO.user;

import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) for updating residence details.
 * This class contains fields required to update the user's address and city of residence.
 */

@Getter @Setter
public class UpdateResidenceRequest {
    private String address;
    private String city;
}