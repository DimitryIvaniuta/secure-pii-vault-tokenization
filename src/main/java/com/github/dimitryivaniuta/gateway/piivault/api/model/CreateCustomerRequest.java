package com.github.dimitryivaniuta.gateway.piivault.api.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to create a token-only downstream customer record.
 *
 * @param externalRef caller-managed reference
 * @param piiToken token obtained from the vault
 * @param customerStatus business status
 */
public record CreateCustomerRequest(
        @NotBlank String externalRef,
        @NotBlank String piiToken,
        @NotBlank String customerStatus
) {
}
