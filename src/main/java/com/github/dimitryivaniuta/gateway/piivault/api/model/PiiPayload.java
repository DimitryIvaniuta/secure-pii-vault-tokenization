package com.github.dimitryivaniuta.gateway.piivault.api.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

/**
 * Sensitive customer payload stored only inside the encrypted vault.
 *
 * @param fullName legal full name
 * @param email primary email
 * @param phoneNumber phone in E.164-like format
 * @param nationalId government or internal id
 * @param addressLine1 primary address line
 * @param dateOfBirth date of birth
 */
public record PiiPayload(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "^[+0-9 ()-]{7,32}$") String phoneNumber,
        @NotBlank String nationalId,
        @NotBlank String addressLine1,
        LocalDate dateOfBirth
) {
}
