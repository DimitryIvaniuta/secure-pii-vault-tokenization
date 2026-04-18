package com.github.dimitryivaniuta.gateway.piivault.api.model;

import com.github.dimitryivaniuta.gateway.piivault.domain.PiiClassification;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request to create a new encrypted PII record and obtain an opaque token.
 *
 * @param subjectRef business subject reference
 * @param classification data classification
 * @param pii sensitive payload
 */
public record CreateVaultTokenRequest(
        @NotBlank String subjectRef,
        @NotNull PiiClassification classification,
        @Valid @NotNull PiiPayload pii
) {
}
