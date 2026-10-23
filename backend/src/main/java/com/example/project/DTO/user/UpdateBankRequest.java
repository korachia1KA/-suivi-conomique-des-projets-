package com.example.project.DTO.user;

import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) for updating bank details.
 * This class contains fields required to update the IBAN and its holder information.
 */

@Getter
@Setter
public class UpdateBankRequest {
    private String iban;
    private String ibanHolder;
}